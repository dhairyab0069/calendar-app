//! The collection of calendars and the cross-calendar operations.

use std::collections::HashMap;

use chrono::{Duration, NaiveDate, NaiveDateTime};
use chrono_tz::Tz;

use super::calendar::Calendar;
use super::error::{CalendarError, Result};
use super::event::{Event, SeriesId, iso_local};
use super::zone;

/// All calendars, in creation order, with one of them "in use".
///
/// This is Java's `SparseHashCalendar` (the `CalendarModel` interface had one
/// implementation, so no trait is needed). Java tracked the active calendar by
/// *name* and had to patch the name on rename; here it is an index into the
/// list, which a rename cannot invalidate. Nothing ever deletes a calendar; if
/// that is added, the index must be fixed up at the same time.
///
/// Operations on the active calendar go through [`active`](Self::active) and
/// [`active_mut`](Self::active_mut) instead of mirroring every `Calendar`
/// method here. Only operations that touch *two* calendars live on this type,
/// since the borrow checker rightly refuses two `&mut` into one list.
#[derive(Debug, Default)]
pub struct Calendars {
    calendars: Vec<Calendar>,
    active: Option<usize>,
}

impl Calendars {
    pub fn new() -> Self {
        Self::default()
    }

    /// Creates a calendar. The first one created becomes the active one.
    ///
    /// Names must not be blank and must be unique (case-sensitive). The name is
    /// stored as given, not trimmed, like Java.
    pub fn create_calendar(&mut self, name: &str, zone: Tz) -> Result<()> {
        validate_name(name)?;
        if self.index_of(name).is_some() {
            return Err(already_exists(name));
        }
        self.calendars.push(Calendar::new(name, zone));
        self.active.get_or_insert(self.calendars.len() - 1);
        Ok(())
    }

    /// Renames a calendar in place. Renaming to its own name is allowed.
    ///
    /// Java removed and re-inserted the entry, which moved a renamed calendar
    /// to the end of `listCalendars()`. That looks accidental, so the position
    /// is kept here (listed as an open question in PLAN.md).
    pub fn rename_calendar(&mut self, current: &str, new_name: &str) -> Result<()> {
        validate_name(new_name)?;
        let index = self.require_index(current)?;
        if self.index_of(new_name).is_some_and(|other| other != index) {
            return Err(already_exists(new_name));
        }
        self.calendars[index].set_name(new_name);
        Ok(())
    }

    /// Changes a calendar's timezone, keeping every event at the same instant:
    /// 10:00 New York becomes 15:00 London. Gaps and overlaps follow
    /// [`zone::to_instant`]. All or nothing: a clash after conversion leaves
    /// the calendar unchanged.
    pub fn update_calendar_timezone(&mut self, name: &str, new_zone: Tz) -> Result<()> {
        let index = self.require_index(name)?;
        if self.calendars[index].zone() == new_zone {
            return Ok(());
        }
        self.calendars[index] = self.calendars[index].converted_to(new_zone)?;
        Ok(())
    }

    /// Makes `name` the active calendar.
    pub fn use_calendar(&mut self, name: &str) -> Result<()> {
        self.active = Some(self.require_index(name)?);
        Ok(())
    }

    pub fn has_active_calendar(&self) -> bool {
        self.active.is_some()
    }

    pub fn active_calendar_name(&self) -> Option<&str> {
        self.active.map(|i| self.calendars[i].name())
    }

    /// The active calendar's timezone, or `NoActiveCalendar`.
    pub fn active_calendar_zone(&self) -> Result<Tz> {
        self.active().map(Calendar::zone)
    }

    /// Calendar names in creation order.
    pub fn list_calendars(&self) -> Vec<&str> {
        self.calendars.iter().map(Calendar::name).collect()
    }

    /// The active calendar, or `NoActiveCalendar`.
    pub fn active(&self) -> Result<&Calendar> {
        self.active
            .map(|i| &self.calendars[i])
            .ok_or(CalendarError::NoActiveCalendar)
    }

    /// Mutable access to the active calendar, or `NoActiveCalendar`.
    pub fn active_mut(&mut self) -> Result<&mut Calendar> {
        match self.active {
            Some(i) => Ok(&mut self.calendars[i]),
            None => Err(CalendarError::NoActiveCalendar),
        }
    }

    /// Any calendar by name.
    pub fn calendar(&self, name: &str) -> Result<&Calendar> {
        Ok(&self.calendars[self.require_index(name)?])
    }

    /// Copies one event to `target` so that it starts at `target_start` (a
    /// local time in the *target's* zone). If the event belongs to a series,
    /// the whole series is copied with the same shift and gets a new series id.
    ///
    /// The shift is measured on the instant axis, so the copy keeps its
    /// duration even across a DST change.
    pub fn copy_event(
        &mut self,
        subject: &str,
        source_start: NaiveDateTime,
        target: &str,
        target_start: NaiveDateTime,
    ) -> Result<()> {
        let source_index = self.active_index()?;
        let target_index = self.require_index(target)?;
        let (source_zone, target_zone) = (
            self.calendars[source_index].zone(),
            self.calendars[target_index].zone(),
        );

        let source = &self.calendars[source_index];
        let pivot = source.find_event(subject, source_start).ok_or_else(|| {
            CalendarError::EventNotFound(format!(
                "Event not found: {subject} at {}",
                iso_local(source_start)
            ))
        })?;
        let mut to_copy: Vec<Event> = match pivot.series() {
            Some(id) => source.series_events(id).into_iter().cloned().collect(),
            None => vec![pivot.clone()],
        };
        to_copy.sort_by_key(Event::start);

        let shift = zone::to_instant(target_zone, target_start)
            - zone::to_instant(source_zone, source_start);
        let mut series_map = HashMap::new();
        let target_cal = &mut self.calendars[target_index];
        let mut copies = Vec::with_capacity(to_copy.len());
        for event in &to_copy {
            let series = renumber(&mut series_map, event.series());
            let start = zone::to_local(
                target_zone,
                zone::to_instant(source_zone, event.start()) + shift,
            );
            let end = zone::to_local(
                target_zone,
                zone::to_instant(source_zone, event.end()) + shift,
            );
            copies.push(event.rebuilt(start, end, series)?);
        }
        target_cal.add_all(copies)
    }

    /// Copies every event starting on `source_date` to `target`, placing them
    /// on `target_date` (a date in the target's zone).
    pub fn copy_events_on_date(
        &mut self,
        source_date: NaiveDate,
        target: &str,
        target_date: NaiveDate,
    ) -> Result<()> {
        self.copy_range(source_date, source_date, target, target_date)
    }

    /// Copies every event starting between `first` and `last` (inclusive) to
    /// `target`, keeping their spacing, with `first` landing on `target_first`.
    pub fn copy_events_between(
        &mut self,
        first: NaiveDate,
        last: NaiveDate,
        target: &str,
        target_first: NaiveDate,
    ) -> Result<()> {
        self.copy_range(first, last, target, target_first)
    }

    fn copy_range(
        &mut self,
        first: NaiveDate,
        last: NaiveDate,
        target: &str,
        target_first: NaiveDate,
    ) -> Result<()> {
        let source_index = self.active_index()?;
        let target_index = self.require_index(target)?;
        let source_zone = self.calendars[source_index].zone();
        let target_zone = self.calendars[target_index].zone();

        let mut to_copy: Vec<Event> = self.calendars[source_index]
            .events_in_range(first, last)
            .into_iter()
            .cloned()
            .collect();
        to_copy.sort_by_key(Event::start);

        let mut series_map = HashMap::new();
        let target_cal = &mut self.calendars[target_index];
        let mut copies = Vec::with_capacity(to_copy.len());
        for event in &to_copy {
            let series = renumber(&mut series_map, event.series());
            let align = |t| align_date_time(t, source_zone, target_zone, first, target_first);
            copies.push(event.rebuilt(align(event.start())?, align(event.end())?, series)?);
        }
        target_cal.add_all(copies)
    }

    fn active_index(&self) -> Result<usize> {
        self.active.ok_or(CalendarError::NoActiveCalendar)
    }

    fn index_of(&self, name: &str) -> Option<usize> {
        self.calendars.iter().position(|c| c.name() == name)
    }

    fn require_index(&self, name: &str) -> Result<usize> {
        self.index_of(name)
            .ok_or_else(|| CalendarError::CalendarNotFound(format!("Calendar not found: {name}")))
    }
}

/// One new series id per distinct source series within a single copy command,
/// so occurrences that were together stay together in the target. Events that
/// are not in a series stay that way.
fn renumber(map: &mut HashMap<SeriesId, SeriesId>, old: Option<SeriesId>) -> Option<SeriesId> {
    old.map(|id| *map.entry(id).or_insert_with(SeriesId::fresh))
}

/// Maps a source local time to the target calendar for the date-based copies.
///
/// The day offset from the source base date is preserved, then the zone
/// conversion is applied, and if that moved the time to another calendar day
/// (e.g. 22:00 Los Angeles is 01:00 the next day in New York) the target date
/// shifts by that many days too. The time of day is the converted one.
fn align_date_time(
    source: NaiveDateTime,
    source_zone: Tz,
    target_zone: Tz,
    source_base: NaiveDate,
    target_base: NaiveDate,
) -> Result<NaiveDateTime> {
    let day_offset = (source.date() - source_base).num_days();
    let converted = zone::convert_local(source, source_zone, target_zone);
    let zone_offset = (converted.date() - source.date()).num_days();
    let date = target_base
        .checked_add_signed(Duration::days(day_offset + zone_offset))
        .ok_or_else(|| CalendarError::InvalidInput("Copied date is out of range".into()))?;
    Ok(date.and_time(converted.time()))
}

fn validate_name(name: &str) -> Result<()> {
    if name.trim().is_empty() {
        return Err(CalendarError::InvalidInput(
            "Calendar name cannot be blank".into(),
        ));
    }
    Ok(())
}

fn already_exists(name: &str) -> CalendarError {
    CalendarError::DuplicateCalendar(format!("Calendar name already exists: {name}"))
}

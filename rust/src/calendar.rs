//! A single named calendar: its timezone and its events.

use std::collections::BTreeMap;

use chrono::{Datelike, Duration, NaiveDate, NaiveDateTime};
use chrono_tz::Tz;

use crate::error::{CalendarError, Result};
use crate::event::{Event, EventChanges, RecurrenceEnd, SeriesId, iso_local};
use crate::weekday::Weekday;
use crate::zone;

/// Recurrence expansion gives up after walking this many days (Java's
/// `maxIterations`). It stops `repeats M for 20000 times` from freezing the app.
const MAX_RECURRENCE_DAYS: u32 = 10_000;

/// Events of one calendar, plus the zone their local times are expressed in.
///
/// Storage is a `BTreeMap` from start date to that day's events. Java used a
/// `HashMap` for the date index *and* a second map from series id to events,
/// and every edit had to keep both in step. Here the day map is the only
/// source of truth and series membership is read off `Event::series`, so the
/// two cannot drift apart. The `BTreeMap` also gives date-ordered output
/// (Java's `getAllEvents` order was arbitrary, so no behaviour is lost).
///
/// Events are indexed by their *start* date only. A multi-day event therefore
/// shows up in `events_on_date` and `is_busy_at` for its start day alone;
/// that is how the Java model behaved and is kept on purpose.
///
/// Every mutating operation is atomic: on error the calendar is left exactly
/// as it was. (The Java series edits could stop half-way and leave some
/// occurrences changed.)
#[derive(Debug, Clone)]
pub struct Calendar {
    name: String,
    zone: Tz,
    by_date: BTreeMap<NaiveDate, Vec<Event>>,
}

impl Calendar {
    pub(crate) fn new(name: &str, zone: Tz) -> Self {
        Self {
            name: name.to_string(),
            zone,
            by_date: BTreeMap::new(),
        }
    }

    pub(crate) fn set_name(&mut self, name: &str) {
        self.name = name.to_string();
    }

    /// The calendar's unique name.
    pub fn name(&self) -> &str {
        &self.name
    }

    /// The timezone that this calendar's event times are expressed in.
    pub fn zone(&self) -> Tz {
        self.zone
    }

    /// Adds a single event, or expands a recurring template into one stored
    /// event per matching day (all sharing a new [`SeriesId`]).
    ///
    /// Fails with `DuplicateEvent` if any resulting event has the same
    /// (subject, start, end) as one already stored; nothing is added then.
    pub fn add_event(&mut self, event: Event) -> Result<()> {
        self.transact(|cal| {
            let Some(recurrence) = event.recurrence() else {
                if cal.is_duplicate(&event) {
                    return Err(CalendarError::DuplicateEvent(format!(
                        "Event already exists: {} at {}",
                        event.subject(),
                        iso_local(event.start())
                    )));
                }
                cal.insert(event);
                return Ok(());
            };

            if event.start().date() != event.end().date() {
                return Err(CalendarError::InvalidInput(
                    "Recurring event template must start and end on the same day".into(),
                ));
            }
            let series = SeriesId::fresh();
            let occurrences = occurrences(&event, recurrence.days(), recurrence.end(), series)?;
            if let Some(clash) = occurrences.iter().find(|o| cal.is_duplicate(o)) {
                return Err(CalendarError::DuplicateEvent(format!(
                    "Recurring event would create duplicate: {} at {}",
                    clash.subject(),
                    iso_local(clash.start())
                )));
            }
            for occurrence in occurrences {
                cal.insert(occurrence);
            }
            Ok(())
        })
    }

    /// Edits the one event identified by subject and exact start.
    ///
    /// An edited occurrence is *detached* from its series (the Java behaviour
    /// the original tests call "detaches from series"): later series edits no
    /// longer touch it.
    pub fn edit_event(
        &mut self,
        subject: &str,
        start: NaiveDateTime,
        changes: &EventChanges,
    ) -> Result<()> {
        self.transact(|cal| {
            let original = cal
                .take(subject, start)
                .ok_or_else(|| not_found(subject, start))?;
            let edited = changes.apply_to(&original, None)?;
            if cal.is_duplicate(&edited) {
                return Err(CalendarError::DuplicateEvent(
                    "Edit would create duplicate event".into(),
                ));
            }
            cal.insert(edited);
            Ok(())
        })
    }

    /// Edits the series occurrence at `start_from` and every later one.
    pub fn edit_series_from_date(
        &mut self,
        subject: &str,
        start_from: NaiveDateTime,
        changes: &EventChanges,
    ) -> Result<()> {
        let series = self
            .find_event(subject, start_from)
            .and_then(Event::series)
            .ok_or_else(|| CalendarError::EventNotFound("Event or series not found".into()))?;
        self.edit_series_where(series, |e| e.start() >= start_from, changes)
    }

    /// Edits every occurrence of a series.
    pub fn edit_entire_series(&mut self, series: SeriesId, changes: &EventChanges) -> Result<()> {
        self.edit_series_where(series, |_| true, changes)
    }

    /// The event with this subject starting exactly at `start`, if any.
    pub fn find_event(&self, subject: &str, start: NaiveDateTime) -> Option<&Event> {
        self.by_date
            .get(&start.date())?
            .iter()
            .find(|e| e.subject() == subject && e.start() == start)
    }

    /// Events starting on `date`, in the order they were added.
    pub fn events_on_date(&self, date: NaiveDate) -> Vec<&Event> {
        self.by_date.get(&date).into_iter().flatten().collect()
    }

    /// Events starting on any date from `first` to `last`, both inclusive.
    /// An inverted range is simply empty (it must not reach `BTreeMap::range`,
    /// which panics on one).
    pub fn events_in_range(&self, first: NaiveDate, last: NaiveDate) -> Vec<&Event> {
        if first > last {
            return Vec::new();
        }
        self.by_date
            .range(first..=last)
            .flat_map(|(_, events)| events)
            .collect()
    }

    /// Every event, ordered by start date (then insertion order within a day).
    pub fn all_events(&self) -> Vec<&Event> {
        self.by_date.values().flatten().collect()
    }

    /// Stored events of one series, in date order.
    pub fn series_events(&self, series: SeriesId) -> Vec<&Event> {
        self.all_events()
            .into_iter()
            .filter(|e| e.series() == Some(series))
            .collect()
    }

    /// True if an event that starts on `at`'s date covers `at`. The start is
    /// inclusive and the end exclusive, so back-to-back events never overlap.
    pub fn is_busy_at(&self, at: NaiveDateTime) -> bool {
        self.events_on_date(at.date())
            .iter()
            .any(|e| e.start() <= at && at < e.end())
    }

    /// Adds several events as one all-or-nothing step (used by copy).
    pub(crate) fn add_all(&mut self, events: Vec<Event>) -> Result<()> {
        self.transact(|cal| events.into_iter().try_for_each(|e| cal.add_event(e)))
    }

    /// This calendar re-expressed in `zone`: same instants, new local times.
    /// Series ids are kept.
    pub(crate) fn converted_to(&self, zone: Tz) -> Result<Self> {
        let mut converted = Self::new(&self.name, zone);
        for event in self.all_events() {
            let start = zone::convert_local(event.start(), self.zone, zone);
            let end = zone::convert_local(event.end(), self.zone, zone);
            converted.add_event(event.rebuilt(start, end, event.series())?)?;
        }
        Ok(converted)
    }

    fn edit_series_where(
        &mut self,
        series: SeriesId,
        selected: impl Fn(&Event) -> bool,
        changes: &EventChanges,
    ) -> Result<()> {
        self.transact(|cal| {
            let members = cal.series_events(series);
            if members.is_empty() {
                return Err(CalendarError::EventNotFound(format!(
                    "Series not found: {series}"
                )));
            }
            let targets: Vec<Event> = members
                .into_iter()
                .filter(|e| selected(e))
                .cloned()
                .collect();
            // One at a time, like Java: each edited event is checked against
            // the calendar as it stands, including occurrences edited just
            // before it. That is why giving every occurrence the same new
            // start fails for a series of two or more.
            for target in targets {
                cal.take(target.subject(), target.start());
                let edited = changes.apply_to(&target, Some(series))?;
                if cal.is_duplicate(&edited) {
                    return Err(CalendarError::DuplicateEvent(
                        "Edit would create duplicate in series".into(),
                    ));
                }
                cal.insert(edited);
            }
            Ok(())
        })
    }

    /// Runs `op`, restoring the previous state if it fails.
    ///
    /// Cloning the whole map is O(events) per edit. A personal calendar holds
    /// hundreds of events, so the simplicity beats an undo log; revisit only
    /// if profiling says otherwise.
    fn transact<T>(&mut self, op: impl FnOnce(&mut Self) -> Result<T>) -> Result<T> {
        let backup = self.by_date.clone();
        let result = op(self);
        if result.is_err() {
            self.by_date = backup;
        }
        result
    }

    fn is_duplicate(&self, event: &Event) -> bool {
        self.events_on_date(event.start().date())
            .iter()
            .any(|e| e.key() == event.key())
    }

    fn insert(&mut self, event: Event) {
        self.by_date
            .entry(event.start().date())
            .or_default()
            .push(event);
    }

    fn take(&mut self, subject: &str, start: NaiveDateTime) -> Option<Event> {
        let date = start.date();
        let day = self.by_date.get_mut(&date)?;
        let index = day
            .iter()
            .position(|e| e.subject() == subject && e.start() == start)?;
        let event = day.remove(index);
        if day.is_empty() {
            self.by_date.remove(&date);
        }
        Some(event)
    }
}

fn not_found(subject: &str, start: NaiveDateTime) -> CalendarError {
    CalendarError::EventNotFound(format!(
        "Event not found: {subject} at {}",
        iso_local(start)
    ))
}

/// Walks day by day from the template's date, emitting one occurrence per day
/// whose weekday is listed, until the count is reached or the date passes
/// `until`. The walk is capped at [`MAX_RECURRENCE_DAYS`] days.
fn occurrences(
    template: &Event,
    days: &[Weekday],
    end: RecurrenceEnd,
    series: SeriesId,
) -> Result<Vec<Event>> {
    let mut found = Vec::new();
    let mut date = template.start().date();
    let mut walked = 0;
    loop {
        let more = match end {
            RecurrenceEnd::Count(max) => found.len() < max as usize,
            RecurrenceEnd::Until(until) => date <= until.date(),
        };
        if !more {
            break;
        }
        walked += 1;
        if walked > MAX_RECURRENCE_DAYS {
            return Err(CalendarError::TooManyOccurrences);
        }
        if days.iter().any(|d| d.to_chrono() == date.weekday()) {
            found.push(template.rebuilt(
                date.and_time(template.start().time()),
                date.and_time(template.end().time()),
                Some(series),
            )?);
        }
        match date.checked_add_signed(Duration::days(1)) {
            Some(next) => date = next,
            None => break,
        }
    }
    Ok(found)
}

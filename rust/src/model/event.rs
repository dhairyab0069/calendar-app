//! The event value type, its recurrence template and the edit description.

use std::fmt;
use std::sync::atomic::{AtomicU64, Ordering};

use chrono::{NaiveDateTime, Timelike};

use super::error::{CalendarError, Result};
use super::status::EventStatus;
use super::weekday::Weekday;

/// Identifies one recurring series inside a single [`Calendar`](crate::Calendar).
///
/// Java stored a random UUID string on every occurrence. Here ids come from a
/// process-wide atomic counter: still unique across all calendars (so a series
/// copied into another calendar can never be confused with its original), but
/// `Copy`, hashable and with no `uuid` dependency. The numbers carry no
/// meaning and are not stable between runs; never persist them.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct SeriesId(u64);

impl SeriesId {
    /// A fresh id, different from every id issued before in this process.
    pub(crate) fn fresh() -> Self {
        static NEXT: AtomicU64 = AtomicU64::new(1);
        Self(NEXT.fetch_add(1, Ordering::Relaxed))
    }
}

impl fmt::Display for SeriesId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "series-{}", self.0)
    }
}

/// How a recurring series stops.
///
/// Java kept `Integer repeatCount` and `LocalDateTime repeatUntil` side by
/// side and made the builder clear one when the other was set, plus a
/// validation step for "neither given". An enum makes "exactly one" the only
/// representable state, so those checks (and their tests) disappear.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RecurrenceEnd {
    /// Stop after this many occurrences. Must be positive.
    Count(u32),
    /// Stop after this date. Only the date part is used, and it is inclusive.
    Until(NaiveDateTime),
}

/// The weekday pattern of a recurring event, as written by `repeats MWF ...`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Recurrence {
    days: Vec<Weekday>,
    end: RecurrenceEnd,
}

impl Recurrence {
    fn new(days: &[Weekday], end: RecurrenceEnd) -> Result<Self> {
        if days.is_empty() {
            return Err(CalendarError::InvalidInput(
                "Recurrence days cannot be null or empty".into(),
            ));
        }
        if end == RecurrenceEnd::Count(0) {
            return Err(CalendarError::InvalidInput(
                "Repeat count must be positive".into(),
            ));
        }
        Ok(Self {
            days: days.to_vec(),
            end,
        })
    }

    /// Weekdays the event repeats on, in the order given.
    pub fn days(&self) -> &[Weekday] {
        &self.days
    }

    /// When the series stops.
    pub fn end(&self) -> RecurrenceEnd {
        self.end
    }
}

/// The identity of an event: subject plus start plus end.
///
/// Java defined `CalendarEvent.equals` on this triple, ignoring description,
/// location and status, which made `equals` surprising. In Rust that rule is
/// named instead of hidden inside `PartialEq`: the calendar's "no duplicates"
/// rule compares `key()`s, and `Event` itself has no `==`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct EventKey<'a> {
    subject: &'a str,
    start: NaiveDateTime,
    end: NaiveDateTime,
}

/// An immutable calendar event.
///
/// Fields are private so that the invariants (non-blank trimmed subject, end
/// not before start, valid recurrence) hold for every `Event` that exists.
/// "Modifying" an event means building a new one, like Java's `withX` methods.
///
/// Times are zone-less (`NaiveDateTime`): the owning calendar supplies the
/// timezone, which is how the Java model worked too.
///
/// An event is *recurring* if it is a template carrying a [`Recurrence`] or an
/// occurrence stored with a [`SeriesId`]. Java used only the series id, but a
/// template had one from construction; here the calendar assigns ids, so a
/// template gets its id when it is added.
#[derive(Debug, Clone)]
pub struct Event {
    subject: String,
    start: NaiveDateTime,
    end: NaiveDateTime,
    description: String,
    location: String,
    status: EventStatus,
    series: Option<SeriesId>,
    recurrence: Option<Recurrence>,
}

impl Event {
    /// Creates a single event with empty description and location and
    /// `Public` status. The subject is trimmed. An end equal to the start is
    /// allowed (Java only rejected end *before* start).
    pub fn new(subject: &str, start: NaiveDateTime, end: NaiveDateTime) -> Result<Self> {
        let subject = subject.trim();
        if subject.is_empty() {
            return Err(CalendarError::InvalidInput(
                "Event subject cannot be null or empty".into(),
            ));
        }
        check_order(start, end)?;
        Ok(Self {
            subject: subject.to_string(),
            start,
            end,
            description: String::new(),
            location: String::new(),
            status: EventStatus::Public,
            series: None,
            recurrence: None,
        })
    }

    /// Turns the event into a recurring template. The calendar expands it into
    /// one stored occurrence per matching day when it is added.
    pub fn recurring(mut self, days: &[Weekday], end: RecurrenceEnd) -> Result<Self> {
        self.recurrence = Some(Recurrence::new(days, end)?);
        Ok(self)
    }

    /// Replaces the description.
    #[must_use]
    pub fn with_description(mut self, description: &str) -> Self {
        self.description = description.to_string();
        self
    }

    /// Replaces the location.
    #[must_use]
    pub fn with_location(mut self, location: &str) -> Self {
        self.location = location.to_string();
        self
    }

    /// Replaces the status.
    #[must_use]
    pub fn with_status(mut self, status: EventStatus) -> Self {
        self.status = status;
        self
    }

    /// Replaces start and end together. Setting them one at a time could
    /// transiently violate "end not before start", so there is no separate
    /// setter for each.
    pub fn with_times(mut self, start: NaiveDateTime, end: NaiveDateTime) -> Result<Self> {
        check_order(start, end)?;
        self.start = start;
        self.end = end;
        Ok(self)
    }

    pub fn subject(&self) -> &str {
        &self.subject
    }

    pub fn start(&self) -> NaiveDateTime {
        self.start
    }

    pub fn end(&self) -> NaiveDateTime {
        self.end
    }

    pub fn description(&self) -> &str {
        &self.description
    }

    pub fn location(&self) -> &str {
        &self.location
    }

    pub fn status(&self) -> EventStatus {
        self.status
    }

    /// The series this stored event belongs to, if any.
    pub fn series(&self) -> Option<SeriesId> {
        self.series
    }

    /// The recurrence template, present only on events not yet expanded.
    pub fn recurrence(&self) -> Option<&Recurrence> {
        self.recurrence.as_ref()
    }

    /// True for templates and for occurrences that belong to a series.
    pub fn is_recurring(&self) -> bool {
        self.series.is_some() || self.recurrence.is_some()
    }

    /// The (subject, start, end) identity used for duplicate detection.
    pub fn key(&self) -> EventKey<'_> {
        EventKey {
            subject: &self.subject,
            start: self.start,
            end: self.end,
        }
    }

    /// Copy with new times and series membership, and no recurrence template.
    ///
    /// This is the one operation behind Java's `createOccurrence`,
    /// `cloneEvent` and `convertEventToZone`: same text fields, different
    /// times and series.
    pub(crate) fn rebuilt(
        &self,
        start: NaiveDateTime,
        end: NaiveDateTime,
        series: Option<SeriesId>,
    ) -> Result<Self> {
        check_order(start, end)?;
        Ok(Self {
            start,
            end,
            series,
            recurrence: None,
            ..self.clone()
        })
    }
}

/// Mirrors the Java `CalendarEvent.toString()` shape.
impl fmt::Display for Event {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "Event{{subject='{}', start={}, end={}, recurring={}, status={}}}",
            self.subject,
            iso_local(self.start),
            iso_local(self.end),
            self.is_recurring(),
            self.status
        )
    }
}

/// Which fields an edit changes; `None` keeps the current value.
///
/// This replaces the Java signature with six nullable parameters
/// (`editEvent(subject, start, newSubject, newStart, ..., newStatus)`). Use
/// struct-update syntax: `EventChanges { subject: Some("x".into()), ..Default::default() }`.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct EventChanges {
    pub subject: Option<String>,
    pub start: Option<NaiveDateTime>,
    pub end: Option<NaiveDateTime>,
    pub description: Option<String>,
    pub location: Option<String>,
    pub status: Option<EventStatus>,
}

impl EventChanges {
    /// Builds the edited event, validating it like a freshly created one.
    /// The recurrence template is dropped; `series` is whatever membership the
    /// caller wants the edited event to have.
    pub(crate) fn apply_to(&self, original: &Event, series: Option<SeriesId>) -> Result<Event> {
        let mut edited = Event::new(
            self.subject.as_deref().unwrap_or(&original.subject),
            self.start.unwrap_or(original.start),
            self.end.unwrap_or(original.end),
        )?
        .with_description(self.description.as_deref().unwrap_or(&original.description))
        .with_location(self.location.as_deref().unwrap_or(&original.location))
        .with_status(self.status.unwrap_or(original.status));
        edited.series = series;
        Ok(edited)
    }
}

fn check_order(start: NaiveDateTime, end: NaiveDateTime) -> Result<()> {
    if end < start {
        return Err(CalendarError::InvalidInput(
            "End time must be after start time".into(),
        ));
    }
    Ok(())
}

/// Formats like Java's `LocalDateTime.toString()`: `2025-01-15T10:00`, with
/// seconds only when non-zero. Used so error messages match the Java ones.
pub(crate) fn iso_local(dt: NaiveDateTime) -> String {
    if dt.second() == 0 {
        dt.format("%Y-%m-%dT%H:%M").to_string()
    } else {
        dt.format("%Y-%m-%dT%H:%M:%S").to_string()
    }
}

#[cfg(test)]
mod tests {
    use std::collections::HashSet;

    use chrono::NaiveDate;

    use super::*;

    fn at(h: u32, m: u32) -> NaiveDateTime {
        NaiveDate::from_ymd_opt(2025, 1, 15)
            .unwrap()
            .and_hms_opt(h, m, 0)
            .unwrap()
    }

    fn start() -> NaiveDateTime {
        at(10, 0)
    }

    fn end() -> NaiveDateTime {
        at(11, 0)
    }

    fn meeting() -> Event {
        Event::new("Meeting", start(), end()).unwrap()
    }

    #[test]
    fn create_single_event() {
        let event = meeting();
        assert_eq!(event.subject(), "Meeting");
        assert_eq!(event.start(), start());
        assert_eq!(event.end(), end());
        assert!(!event.is_recurring());
        assert_eq!(event.series(), None);
    }

    #[test]
    fn create_event_defaults() {
        let event = meeting();
        assert_eq!(event.description(), "");
        assert_eq!(event.location(), "");
        assert_eq!(event.status(), EventStatus::Public);
    }

    #[test]
    fn create_event_trim_subject() {
        let event = Event::new("  Meeting  ", start(), end()).unwrap();
        assert_eq!(event.subject(), "Meeting");
    }

    #[test]
    fn create_event_empty_subject() {
        assert!(matches!(
            Event::new("", start(), end()),
            Err(CalendarError::InvalidInput(_))
        ));
    }

    #[test]
    fn create_event_whitespace_subject() {
        assert!(Event::new("   ", start(), end()).is_err());
    }

    #[test]
    fn create_event_end_before_start() {
        assert!(Event::new("Meeting", start(), at(9, 0)).is_err());
    }

    #[test]
    fn create_event_end_equal_to_start_is_allowed() {
        assert!(Event::new("Meeting", start(), start()).is_ok());
    }

    #[test]
    fn builder_start_after_end_throws() {
        assert!(meeting().with_times(at(11, 1), end()).is_err());
    }

    #[test]
    fn builder_end_before_start_throws() {
        assert!(meeting().with_times(start(), at(9, 59)).is_err());
    }

    #[test]
    fn create_recurring_event() {
        let days = [Weekday::Monday, Weekday::Wednesday, Weekday::Friday];
        let event = meeting().recurring(&days, RecurrenceEnd::Count(5)).unwrap();
        let recurrence = event.recurrence().unwrap();
        assert!(event.is_recurring());
        assert_eq!(recurrence.days(), days);
        assert_eq!(recurrence.end(), RecurrenceEnd::Count(5));
    }

    #[test]
    fn create_recurring_event_with_until() {
        let until = NaiveDate::from_ymd_opt(2025, 2, 1)
            .unwrap()
            .and_hms_opt(23, 59, 0)
            .unwrap();
        let event = meeting()
            .recurring(&[Weekday::Monday], RecurrenceEnd::Until(until))
            .unwrap();
        assert!(event.is_recurring());
        assert_eq!(
            event.recurrence().unwrap().end(),
            RecurrenceEnd::Until(until)
        );
    }

    #[test]
    fn create_recurring_event_empty_days() {
        assert!(matches!(
            meeting().recurring(&[], RecurrenceEnd::Count(1)),
            Err(CalendarError::InvalidInput(_))
        ));
    }

    #[test]
    fn recurrence_days_length_boundary() {
        let days: [Weekday; 0] = [];
        assert!(meeting().recurring(&days, RecurrenceEnd::Count(1)).is_err());
        assert!(
            meeting()
                .recurring(&[Weekday::Monday], RecurrenceEnd::Count(1))
                .is_ok()
        );
    }

    #[test]
    fn from_copies_recurring_fields_and_is_defensive() {
        // Java copied through a builder and defended the days array. Here
        // `clone` copies everything and `days()` is a shared slice that cannot
        // be mutated, so the "defensive" half is enforced by the type system.
        let recurring = meeting()
            .recurring(
                &[Weekday::Monday, Weekday::Wednesday],
                RecurrenceEnd::Count(3),
            )
            .unwrap();
        let copy = recurring.clone();
        assert!(copy.is_recurring());
        assert_eq!(copy.recurrence(), recurring.recurrence());
    }

    #[test]
    fn from_copies_recurring_event_with_repeat_until() {
        let until = at(23, 59);
        let recurring = meeting()
            .recurring(&[Weekday::Tuesday], RecurrenceEnd::Until(until))
            .unwrap();
        let copy = recurring.clone();
        assert_eq!(
            copy.recurrence().unwrap().end(),
            RecurrenceEnd::Until(until)
        );
    }

    #[test]
    fn create_recurring_event_zero_count() {
        assert!(
            meeting()
                .recurring(&[Weekday::Monday], RecurrenceEnd::Count(0))
                .is_err()
        );
    }

    #[test]
    fn repeat_count_boundary_zero() {
        assert!(Recurrence::new(&[Weekday::Monday], RecurrenceEnd::Count(0)).is_err());
        assert!(Recurrence::new(&[Weekday::Monday], RecurrenceEnd::Count(1)).is_ok());
    }

    #[test]
    fn with_description() {
        let event = meeting();
        let updated = event.clone().with_description("Important meeting");
        assert_eq!(event.description(), "");
        assert_eq!(updated.description(), "Important meeting");
    }

    #[test]
    fn with_location() {
        let event = meeting();
        let updated = event.clone().with_location("Conference Room A");
        assert_eq!(event.location(), "");
        assert_eq!(updated.location(), "Conference Room A");
    }

    #[test]
    fn with_status() {
        let event = meeting();
        let updated = event.clone().with_status(EventStatus::Private);
        assert_eq!(event.status(), EventStatus::Public);
        assert_eq!(updated.status(), EventStatus::Private);
    }

    #[test]
    fn with_times() {
        let event = meeting();
        let updated = event.clone().with_times(at(11, 0), at(12, 0)).unwrap();
        assert_eq!(event.start(), start());
        assert_eq!(event.end(), end());
        assert_eq!(updated.start(), at(11, 0));
        assert_eq!(updated.end(), at(12, 0));
    }

    #[test]
    fn event_equals_equal_objects() {
        assert_eq!(meeting().key(), meeting().key());
    }

    #[test]
    fn event_equals_ignores_description_location_and_status() {
        let other = meeting()
            .with_description("different")
            .with_location("elsewhere")
            .with_status(EventStatus::Private);
        assert_eq!(meeting().key(), other.key());
    }

    #[test]
    fn event_equals_different_subject() {
        let other = Event::new("Different", start(), end()).unwrap();
        assert_ne!(meeting().key(), other.key());
    }

    #[test]
    fn event_equals_different_start() {
        let other = Event::new("Meeting", at(11, 0), end()).unwrap();
        assert_ne!(meeting().key(), other.key());
    }

    #[test]
    fn event_hash_code() {
        let (first, second) = (meeting(), meeting());
        let mut set = HashSet::new();
        set.insert(first.key());
        assert!(set.contains(&second.key()));
    }

    #[test]
    fn event_to_string() {
        let text = meeting().to_string();
        assert!(text.contains("Meeting"));
        assert!(text.contains("recurring=false"));
        assert!(text.contains("start=2025-01-15T10:00,"));
    }

    #[test]
    fn event_to_string_recurring() {
        let event = meeting()
            .recurring(&[Weekday::Monday], RecurrenceEnd::Count(5))
            .unwrap();
        assert!(event.to_string().contains("recurring=true"));
    }

    #[test]
    fn edit_changes_keep_unset_fields() {
        let original = meeting().with_description("Original desc");
        let changes = EventChanges {
            subject: Some("Renamed".into()),
            ..Default::default()
        };
        let edited = changes.apply_to(&original, None).unwrap();
        assert_eq!(edited.subject(), "Renamed");
        assert_eq!(edited.description(), "Original desc");
        assert_eq!(edited.start(), start());
    }
}

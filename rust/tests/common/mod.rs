//! Shared helpers for the ported Java tests.

#![allow(dead_code)] // each test binary uses a different subset

use calendar::{Calendars, Event, RecurrenceEnd, Tz, Weekday};
use chrono::{NaiveDate, NaiveDateTime};

pub fn tz(name: &str) -> Tz {
    name.parse().expect("valid IANA zone")
}

pub fn date(y: i32, m: u32, d: u32) -> NaiveDate {
    NaiveDate::from_ymd_opt(y, m, d).unwrap()
}

pub fn dt(y: i32, m: u32, d: u32, h: u32, min: u32) -> NaiveDateTime {
    date(y, m, d).and_hms_opt(h, min, 0).unwrap()
}

/// Java `ModelTest.start`: 2025-01-15 10:00.
pub fn start() -> NaiveDateTime {
    dt(2025, 1, 15, 10, 0)
}

/// Java `ModelTest.end`: 2025-01-15 11:00.
pub fn end() -> NaiveDateTime {
    dt(2025, 1, 15, 11, 0)
}

/// Java `setUpCalendar`: a "Default" calendar in New York, selected.
pub fn fixture() -> Calendars {
    let mut book = Calendars::new();
    book.create_calendar("Default", tz("America/New_York"))
        .unwrap();
    book.use_calendar("Default").unwrap();
    book
}

pub fn single(subject: &str, start: NaiveDateTime, end: NaiveDateTime) -> Event {
    Event::new(subject, start, end).unwrap()
}

pub fn repeating(
    subject: &str,
    start: NaiveDateTime,
    end: NaiveDateTime,
    days: &[Weekday],
    count: u32,
) -> Event {
    single(subject, start, end)
        .recurring(days, RecurrenceEnd::Count(count))
        .unwrap()
}

/// Mondays 2025-01-06 09:00-09:30, the template most series tests use.
pub fn monday_standup(subject: &str, count: u32) -> Event {
    repeating(
        subject,
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
        &[Weekday::Monday],
        count,
    )
}

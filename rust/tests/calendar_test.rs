//! Model-level cases from the Java `CalendarTest` (the integration suite).
//! The cases that go through the command parser are deferred to phase 2.

mod common;

use calendar::Weekday;
use common::*;

#[test]
fn event_persistence() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Meeting", start(), end()))
        .unwrap();

    let cal = book.active().unwrap();
    assert_eq!(cal.events_on_date(date(2025, 1, 15)).len(), 1);
    assert_eq!(cal.events_on_date(date(2025, 1, 15)).len(), 1);
    assert_eq!(
        cal.events_on_date(date(2025, 1, 15))[0].subject(),
        "Meeting"
    );
}

#[test]
fn event_count() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Event1",
        dt(2025, 1, 15, 10, 0),
        dt(2025, 1, 15, 11, 0),
    ))
    .unwrap();
    cal.add_event(single(
        "Event2",
        dt(2025, 1, 16, 10, 0),
        dt(2025, 1, 16, 11, 0),
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 2);
}

#[test]
fn series_relationships() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Standup",
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
        &[Weekday::Monday, Weekday::Friday],
        4,
    ))
    .unwrap();

    let all = cal.all_events();
    let series = all[0].series();
    assert!(series.is_some());
    for event in all {
        assert_eq!(event.series(), series);
        assert!(event.is_recurring());
    }
}

#[test]
fn midnight_events() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Midnight Event",
        dt(2025, 1, 15, 0, 0),
        dt(2025, 1, 15, 1, 0),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(date(2025, 1, 15)).len(), 1);
}

#[test]
fn end_of_day_events() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Late Event",
        dt(2025, 1, 15, 23, 0),
        dt(2025, 1, 15, 23, 59),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(date(2025, 1, 15)).len(), 1);
}

#[test]
fn year_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "NYE Event",
        dt(2024, 12, 31, 23, 0),
        dt(2024, 12, 31, 23, 59),
    ))
    .unwrap();
    cal.add_event(single(
        "NY Event",
        dt(2025, 1, 1, 1, 0),
        dt(2025, 1, 1, 2, 0),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(date(2024, 12, 31)).len(), 1);
    assert_eq!(cal.events_on_date(date(2025, 1, 1)).len(), 1);
}

#[test]
fn leap_year_date() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Leap Day Event",
        dt(2024, 2, 29, 10, 0),
        dt(2024, 2, 29, 11, 0),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(date(2024, 2, 29)).len(), 1);
}

#[test]
fn empty_date_queries() {
    let book = fixture();
    assert!(
        book.active()
            .unwrap()
            .events_on_date(date(2025, 1, 15))
            .is_empty()
    );
}

#[test]
fn single_day_range() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert_eq!(
        cal.events_in_range(date(2025, 1, 15), date(2025, 1, 15))
            .len(),
        1
    );
}

#[test]
fn busy_day_multiple_events() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    let day = date(2025, 1, 15);
    for (subject, from, to) in [
        ("Morning Meeting", 9, 10),
        ("Lunch", 12, 13),
        ("Afternoon Meeting", 14, 15),
    ] {
        let at = |h| day.and_hms_opt(h, 0, 0).unwrap();
        cal.add_event(single(subject, at(from), at(to))).unwrap();
    }
    assert_eq!(cal.events_on_date(day).len(), 3);
}

#[test]
fn multiple_recurring_series() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Standup",
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
        &[Weekday::Monday],
        4,
    ))
    .unwrap();
    cal.add_event(repeating(
        "Review",
        dt(2025, 1, 8, 14, 0),
        dt(2025, 1, 8, 15, 0),
        &[Weekday::Wednesday],
        4,
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 8);
}

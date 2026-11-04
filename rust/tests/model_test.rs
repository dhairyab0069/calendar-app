//! Port of the model-level cases in Java `ModelTest` (calendar section).
//! Enum, event-builder and `DateTimeParser` cases live next to their code or
//! are deferred; see PLAN.md for the case-by-case table.
//!
//! Test names are the Java names in snake_case without the `test` prefix.

mod common;

use std::collections::HashSet;

use calendar::{CalendarError, Calendars, EventChanges, EventStatus, RecurrenceEnd, Weekday};
use common::*;

fn rename_to(subject: &str) -> EventChanges {
    EventChanges {
        subject: Some(subject.into()),
        ..Default::default()
    }
}

fn first_series(book: &Calendars) -> calendar::SeriesId {
    book.active().unwrap().all_events()[0].series().unwrap()
}

// ---- adding and querying -------------------------------------------------

#[test]
fn add_single_event() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Meeting", start(), end()))
        .unwrap();
    let events = book.active().unwrap().events_on_date(start().date());
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].subject(), "Meeting");
}

#[test]
fn add_duplicate_event() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    let err = cal
        .add_event(single("Meeting", start(), end()))
        .unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
    assert_eq!(
        err.to_string(),
        "Event already exists: Meeting at 2025-01-15T10:00"
    );
}

#[test]
fn add_multiple_events_same_day() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    cal.add_event(single(
        "Lunch",
        dt(2025, 1, 15, 12, 0),
        dt(2025, 1, 15, 13, 0),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(start().date()).len(), 2);
}

#[test]
fn add_recurring_event_with_count() {
    let mut book = fixture();
    let days = [Weekday::Monday, Weekday::Wednesday, Weekday::Friday];
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Standup",
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
        &days,
        4,
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 4);
}

#[test]
fn add_recurring_event_with_until() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    let event = single("Standup", dt(2025, 1, 6, 9, 0), dt(2025, 1, 6, 9, 30))
        .recurring(
            &[Weekday::Monday],
            RecurrenceEnd::Until(dt(2025, 1, 20, 23, 59)),
        )
        .unwrap();
    cal.add_event(event).unwrap();
    assert_eq!(cal.all_events().len(), 3);
}

#[test]
fn add_recurring_event_duplicate() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Standup", 2)).unwrap();
    let err = cal.add_event(monday_standup("Standup", 2)).unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
    assert_eq!(
        cal.all_events().len(),
        2,
        "nothing from the failed add is kept"
    );
}

#[test]
fn recurring_event_single_day_array() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Solo",
        dt(2025, 1, 3, 14, 0),
        dt(2025, 1, 3, 15, 0),
        &[Weekday::Friday],
        2,
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 2);
}

#[test]
fn get_events_on_date() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Meeting", start(), end()))
        .unwrap();
    let events = book.active().unwrap().events_on_date(date(2025, 1, 15));
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].subject(), "Meeting");
}

#[test]
fn get_events_on_date_empty() {
    assert!(
        fixture()
            .active()
            .unwrap()
            .events_on_date(date(2025, 1, 15))
            .is_empty()
    );
}

#[test]
fn get_events_in_range() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    for (subject, day) in [("Meeting1", 15), ("Meeting2", 17), ("Meeting3", 20)] {
        cal.add_event(single(
            subject,
            dt(2025, 1, day, 10, 0),
            dt(2025, 1, day, 11, 0),
        ))
        .unwrap();
    }
    assert_eq!(
        cal.events_in_range(date(2025, 1, 15), date(2025, 1, 18))
            .len(),
        2
    );
}

#[test]
fn get_events_in_range_single_day() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert_eq!(cal.events_in_range(start().date(), start().date()).len(), 1);
}

#[test]
fn get_events_in_range_inverted_is_empty() {
    // Not a Java case: guards the BTreeMap::range panic on first > last.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert!(
        cal.events_in_range(date(2025, 1, 20), date(2025, 1, 10))
            .is_empty()
    );
}

#[test]
fn get_all_events() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting1", start(), end())).unwrap();
    cal.add_event(single(
        "Meeting2",
        dt(2025, 1, 16, 10, 0),
        dt(2025, 1, 16, 11, 0),
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 2);
}

#[test]
fn get_all_events_empty() {
    assert!(fixture().active().unwrap().all_events().is_empty());
}

// ---- busy / available ----------------------------------------------------

#[test]
fn is_busy_at_during_event() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert!(cal.is_busy_at(dt(2025, 1, 15, 10, 30)));
}

#[test]
fn is_busy_at_before_event() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert!(!cal.is_busy_at(dt(2025, 1, 15, 9, 59)));
}

#[test]
fn is_busy_at_after_event() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    assert!(!cal.is_busy_at(dt(2025, 1, 15, 11, 30)));
}

#[test]
fn is_busy_at_empty_day() {
    assert!(!fixture().active().unwrap().is_busy_at(start()));
}

#[test]
fn is_busy_at_boundary_times() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    let second = chrono::Duration::seconds(1);
    assert!(cal.is_busy_at(start()));
    assert!(cal.is_busy_at(end() - second));
    assert!(!cal.is_busy_at(end()));
    assert!(!cal.is_busy_at(end() + second));
}

#[test]
fn multi_day_event_is_only_indexed_on_its_start_date() {
    // Not a Java case: pins the inherited behaviour described in PLAN.md.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Conference",
        dt(2025, 1, 15, 9, 0),
        dt(2025, 1, 17, 17, 0),
    ))
    .unwrap();
    assert_eq!(cal.events_on_date(date(2025, 1, 15)).len(), 1);
    assert!(cal.events_on_date(date(2025, 1, 16)).is_empty());
    assert!(!cal.is_busy_at(dt(2025, 1, 16, 12, 0)));
}

// ---- editing a single event ---------------------------------------------

#[test]
fn edit_event_subject() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    cal.edit_event("Meeting", start(), &rename_to("Updated Meeting"))
        .unwrap();
    let events = cal.events_on_date(start().date());
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].subject(), "Updated Meeting");
}

#[test]
fn edit_event_null_description_preserves_original() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end()).with_description("Original desc"))
        .unwrap();
    cal.edit_event("Meeting", start(), &EventChanges::default())
        .unwrap();
    assert_eq!(
        cal.events_on_date(start().date())[0].description(),
        "Original desc"
    );
}

#[test]
fn edit_event_null_location_preserves_original() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end()).with_location("Original location"))
        .unwrap();
    cal.edit_event("Meeting", start(), &EventChanges::default())
        .unwrap();
    assert_eq!(
        cal.events_on_date(start().date())[0].location(),
        "Original location"
    );
}

#[test]
fn edit_event_not_found() {
    let mut book = fixture();
    let err = book
        .active_mut()
        .unwrap()
        .edit_event("Nonexistent", start(), &rename_to("New"))
        .unwrap_err();
    assert!(matches!(err, CalendarError::EventNotFound(_)));
}

#[test]
fn edit_event_creates_duplicate() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting1", start(), end())).unwrap();
    cal.add_event(single(
        "Meeting2",
        dt(2025, 1, 15, 11, 0),
        dt(2025, 1, 15, 12, 0),
    ))
    .unwrap();
    let changes = EventChanges {
        subject: Some("Meeting1".into()),
        start: Some(start()),
        end: Some(end()),
        ..Default::default()
    };
    let err = cal
        .edit_event("Meeting2", dt(2025, 1, 15, 11, 0), &changes)
        .unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
    assert!(
        cal.find_event("Meeting2", dt(2025, 1, 15, 11, 0)).is_some(),
        "original restored"
    );
}

#[test]
fn edit_event_with_invalid_times_leaves_calendar_unchanged() {
    // Not a Java case: Java removed the event before validating the edit and
    // lost it when the new end was before the start.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Meeting", start(), end())).unwrap();
    let changes = EventChanges {
        end: Some(dt(2025, 1, 15, 9, 0)),
        ..Default::default()
    };
    assert!(matches!(
        cal.edit_event("Meeting", start(), &changes),
        Err(CalendarError::InvalidInput(_))
    ));
    assert!(cal.find_event("Meeting", start()).is_some());
}

#[test]
fn edit_recurring_event_detaches_from_series() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Weekly", 2)).unwrap();
    cal.edit_event("Weekly", dt(2025, 1, 6, 9, 0), &rename_to("Modified"))
        .unwrap();
    let edited = cal
        .events_on_date(date(2025, 1, 6))
        .into_iter()
        .find(|e| e.subject() == "Modified")
        .unwrap();
    assert!(!edited.is_recurring());
}

#[test]
fn edit_single_occurrence_detaches_from_series() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("SeriesB", 2)).unwrap();
    let series = cal.all_events()[0].series().unwrap();
    cal.edit_event("SeriesB", dt(2025, 1, 6, 9, 0), &rename_to("Changed"))
        .unwrap();
    assert!(
        !cal.find_event("Changed", dt(2025, 1, 6, 9, 0))
            .unwrap()
            .is_recurring()
    );
    assert_eq!(cal.series_events(series).len(), 1);
}

#[test]
fn edit_series_cleanup_when_last_event_removed() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Series", 1)).unwrap();
    let series = cal.all_events()[0].series().unwrap();
    cal.edit_event("Series", dt(2025, 1, 6, 9, 0), &rename_to("Changed"))
        .unwrap();
    let all = cal.all_events();
    assert_eq!(all.len(), 1);
    assert!(!all[0].is_recurring());
    assert!(cal.series_events(series).is_empty());
}

// ---- recurrence expansion ------------------------------------------------

#[test]
fn recurring_event_series_tracking() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Standup", 3)).unwrap();
    let all = cal.all_events();
    let series = all[0].series();
    assert!(series.is_some());
    assert!(all.iter().all(|e| e.series() == series));
}

#[test]
fn recurring_event_safety_limit() {
    let mut book = fixture();
    let err = book
        .active_mut()
        .unwrap()
        .add_event(monday_standup("Standup", 20000))
        .unwrap_err();
    assert_eq!(err, CalendarError::TooManyOccurrences);
}

#[test]
fn recurring_event_safety_limit_exact() {
    let mut book = fixture();
    let err = book
        .active_mut()
        .unwrap()
        .add_event(monday_standup("Standup", 10001))
        .unwrap_err();
    assert_eq!(err, CalendarError::TooManyOccurrences);
}

#[test]
fn recurring_event_safety_limit_counts_days_not_occurrences() {
    // Not a Java case: the cap is 10 000 days walked. Every-day for 10 000
    // times walks exactly 10 000 days and is allowed.
    let mut book = fixture();
    let all_days = Weekday::parse_str("MTWRFSU").unwrap();
    let daily = repeating(
        "Daily",
        dt(2025, 1, 6, 8, 0),
        dt(2025, 1, 6, 9, 0),
        &all_days,
        10_000,
    );
    book.active_mut().unwrap().add_event(daily).unwrap();
    assert_eq!(book.active().unwrap().all_events().len(), 10_000);
}

#[test]
fn recurring_event_count_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Single", 1)).unwrap();
    assert_eq!(cal.all_events().len(), 1);
}

#[test]
fn matches_recurrence_pattern_all_days() {
    let mut book = fixture();
    let all_days = Weekday::parse_str("MTWRFSU").unwrap();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Daily",
        dt(2025, 1, 6, 8, 0),
        dt(2025, 1, 6, 9, 0),
        &all_days,
        7,
    ))
    .unwrap();
    assert_eq!(cal.all_events().len(), 7);
}

#[test]
fn should_continue_generating_count_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Once", 1)).unwrap();
    assert_eq!(cal.all_events().len(), 1);
}

#[test]
fn should_continue_generating_until_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    let event = single("Limited", dt(2025, 1, 7, 10, 0), dt(2025, 1, 7, 11, 0))
        .recurring(
            &[Weekday::Tuesday],
            RecurrenceEnd::Until(dt(2025, 1, 14, 23, 59)),
        )
        .unwrap();
    cal.add_event(event).unwrap();
    assert_eq!(cal.all_events().len(), 2);
}

#[test]
fn recurring_template_must_fit_in_one_day() {
    // Not a Java ModelTest case; the rule is in SingleCalendarStore.
    let mut book = fixture();
    let template = single("Overnight", dt(2025, 1, 6, 22, 0), dt(2025, 1, 7, 6, 0))
        .recurring(&[Weekday::Monday], RecurrenceEnd::Count(2))
        .unwrap();
    assert!(matches!(
        book.active_mut().unwrap().add_event(template),
        Err(CalendarError::InvalidInput(_))
    ));
}

// ---- editing series ------------------------------------------------------

#[test]
fn edit_series_from_date_creates_duplicate() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "SeriesA",
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
        &[Weekday::Monday, Weekday::Wednesday],
        2,
    ))
    .unwrap();
    cal.add_event(single(
        "Conflicting",
        dt(2025, 1, 8, 9, 0),
        dt(2025, 1, 8, 9, 30),
    ))
    .unwrap();
    let err = cal
        .edit_series_from_date("SeriesA", dt(2025, 1, 8, 9, 0), &rename_to("Conflicting"))
        .unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
}

#[test]
fn edit_series_from_date_nulls_preserve_original() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(
        monday_standup("Series", 2)
            .with_description("Original desc")
            .with_location("Original loc"),
    )
    .unwrap();
    cal.edit_series_from_date("Series", dt(2025, 1, 6, 9, 0), &EventChanges::default())
        .unwrap();
    let modified = cal.events_on_date(date(2025, 1, 6))[0];
    assert_eq!(modified.description(), "Original desc");
    assert_eq!(modified.location(), "Original loc");
}

#[test]
fn edit_entire_series_nulls_preserve_original() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    let template = repeating(
        "Series",
        dt(2025, 1, 7, 10, 0),
        dt(2025, 1, 7, 11, 0),
        &[Weekday::Tuesday],
        2,
    )
    .with_description("Desc")
    .with_location("Loc");
    cal.add_event(template).unwrap();
    let series = cal.all_events()[0].series().unwrap();
    cal.edit_entire_series(series, &EventChanges::default())
        .unwrap();
    for event in cal.all_events() {
        assert_eq!(event.description(), "Desc");
        assert_eq!(event.location(), "Loc");
    }
}

#[test]
fn edit_series_from_date_index_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Series", 3)).unwrap();
    cal.edit_series_from_date("Series", dt(2025, 1, 6, 9, 0), &rename_to("Changed"))
        .unwrap();
    assert!(cal.all_events().iter().all(|e| e.subject() == "Changed"));
}

#[test]
fn edit_entire_series_index_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Series",
        dt(2025, 1, 8, 13, 0),
        dt(2025, 1, 8, 14, 0),
        &[Weekday::Wednesday],
        2,
    ))
    .unwrap();
    let series = cal.all_events()[0].series().unwrap();
    cal.edit_entire_series(series, &rename_to("AllChanged"))
        .unwrap();
    let all = cal.all_events();
    assert_eq!(all.len(), 2);
    assert!(all.iter().all(|e| e.subject() == "AllChanged"));
}

#[test]
fn edit_entire_series_creates_duplicate() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("TeamSync", 2)).unwrap();
    cal.add_event(single(
        "Existing",
        dt(2025, 1, 6, 9, 0),
        dt(2025, 1, 6, 9, 30),
    ))
    .unwrap();
    let series = cal
        .find_event("TeamSync", dt(2025, 1, 6, 9, 0))
        .unwrap()
        .series()
        .unwrap();
    let err = cal
        .edit_entire_series(series, &rename_to("Existing"))
        .unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
}

#[test]
fn edit_series_from_date_filter_boundary() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Series", 3)).unwrap();
    cal.edit_series_from_date("Series", dt(2025, 1, 13, 9, 0), &rename_to("FromSecond"))
        .unwrap();
    assert_eq!(cal.events_on_date(date(2025, 1, 6))[0].subject(), "Series");
    assert_eq!(
        cal.events_on_date(date(2025, 1, 13))[0].subject(),
        "FromSecond"
    );
    assert_eq!(
        cal.events_on_date(date(2025, 1, 20))[0].subject(),
        "FromSecond"
    );
}

#[test]
fn edit_series_preserves_series_id() {
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Series",
        dt(2025, 1, 7, 11, 0),
        dt(2025, 1, 7, 12, 0),
        &[Weekday::Tuesday],
        2,
    ))
    .unwrap();
    let original = cal.all_events()[0].series().unwrap();
    cal.edit_series_from_date("Series", dt(2025, 1, 7, 11, 0), &rename_to("Modified"))
        .unwrap();
    assert!(
        cal.all_events()
            .iter()
            .all(|e| e.series() == Some(original))
    );
}

#[test]
fn edit_series_from_date_requires_a_series_member() {
    // Not a Java ModelTest case: SingleCalendarStore.editSeriesFromDate.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Plain", start(), end())).unwrap();
    assert!(matches!(
        cal.edit_series_from_date("Plain", start(), &rename_to("X")),
        Err(CalendarError::EventNotFound(_))
    ));
}

#[test]
fn edit_series_failure_leaves_every_occurrence_unchanged() {
    // Not a Java case: giving every occurrence the same new start collides on
    // the second one. Java had already rewritten the first by then.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(monday_standup("Series", 3)).unwrap();
    let changes = EventChanges {
        start: Some(dt(2025, 1, 6, 12, 0)),
        end: Some(dt(2025, 1, 6, 12, 30)),
        ..Default::default()
    };
    assert!(matches!(
        cal.edit_series_from_date("Series", dt(2025, 1, 6, 9, 0), &changes),
        Err(CalendarError::DuplicateEvent(_))
    ));
    let starts: Vec<_> = cal.all_events().iter().map(|e| e.start()).collect();
    assert_eq!(
        starts,
        [
            dt(2025, 1, 6, 9, 0),
            dt(2025, 1, 13, 9, 0),
            dt(2025, 1, 20, 9, 0)
        ]
    );
}

// ---- copying between calendars ------------------------------------------

#[test]
fn copy_events_on_date_between_calendars() {
    let mut book = fixture();
    book.create_calendar("West", tz("America/Los_Angeles"))
        .unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("Standup", start(), end()))
        .unwrap();

    book.copy_events_on_date(start().date(), "West", start().date())
        .unwrap();

    book.use_calendar("West").unwrap();
    let west = book.active().unwrap().events_on_date(start().date());
    assert_eq!(west.len(), 1);
    assert_eq!(west[0].start(), dt(2025, 1, 15, 7, 0));
    assert_eq!(west[0].end(), dt(2025, 1, 15, 8, 0));
}

#[test]
fn copy_event_to_target_calendar() {
    let mut book = fixture();
    book.create_calendar("Work", tz("Europe/London")).unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("Design Review", start(), end()))
        .unwrap();

    let target_start = dt(2025, 1, 16, 14, 0);
    book.copy_event("Design Review", start(), "Work", target_start)
        .unwrap();

    book.use_calendar("Work").unwrap();
    let events = book.active().unwrap().events_on_date(date(2025, 1, 16));
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].start(), target_start);
    assert_eq!(events[0].end(), dt(2025, 1, 16, 15, 0));
}

#[test]
fn copy_recurring_event_copies_series() {
    let mut book = fixture();
    book.create_calendar("Target", tz("America/New_York"))
        .unwrap();
    book.active_mut()
        .unwrap()
        .add_event(repeating(
            "Recurring",
            dt(2025, 1, 9, 15, 0),
            dt(2025, 1, 9, 16, 0),
            &[Weekday::Thursday],
            2,
        ))
        .unwrap();
    let original = first_series(&book);

    book.copy_event(
        "Recurring",
        dt(2025, 1, 9, 15, 0),
        "Target",
        dt(2025, 1, 9, 15, 0),
    )
    .unwrap();

    let copied = book.calendar("Target").unwrap().all_events();
    assert_eq!(copied.len(), 2);
    let copied_series = copied[0].series().unwrap();
    assert_ne!(copied_series, original);
    assert!(copied.iter().all(|e| e.series() == Some(copied_series)));
}

#[test]
fn copy_events_align_date_time_calculation() {
    let mut book = fixture();
    book.create_calendar("Other", tz("America/Chicago"))
        .unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("Event", start(), end()))
        .unwrap();

    book.copy_events_on_date(start().date(), "Other", date(2025, 1, 20))
        .unwrap();

    let events = book
        .calendar("Other")
        .unwrap()
        .events_on_date(date(2025, 1, 20));
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].start(), dt(2025, 1, 20, 9, 0));
}

#[test]
fn copy_events_across_midnight_shifts_the_target_date() {
    // Not a Java case: 22:00 in Los Angeles is 01:00 next day in New York.
    let mut book = Calendars::new();
    book.create_calendar("West", tz("America/Los_Angeles"))
        .unwrap();
    book.create_calendar("East", tz("America/New_York"))
        .unwrap();
    book.use_calendar("West").unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single(
            "Late",
            dt(2025, 1, 15, 22, 0),
            dt(2025, 1, 15, 23, 0),
        ))
        .unwrap();

    book.copy_events_on_date(date(2025, 1, 15), "East", date(2025, 1, 15))
        .unwrap();

    let east = book.calendar("East").unwrap().all_events();
    assert_eq!(east[0].start(), dt(2025, 1, 16, 1, 0));
    assert_eq!(east[0].end(), dt(2025, 1, 16, 2, 0));
}

#[test]
fn copy_events_single_event_no_series() {
    let mut book = fixture();
    book.create_calendar("Dest", tz("UTC")).unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("Single", start(), end()))
        .unwrap();
    book.copy_events_on_date(start().date(), "Dest", start().date())
        .unwrap();
    let events = book.calendar("Dest").unwrap().all_events();
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].series(), None);
}

#[test]
fn copy_events_on_date_with_recurring_series() {
    let mut book = fixture();
    book.create_calendar("Target", tz("America/Chicago"))
        .unwrap();
    book.active_mut()
        .unwrap()
        .add_event(repeating(
            "Weekly",
            dt(2025, 1, 15, 10, 0),
            dt(2025, 1, 15, 11, 0),
            &[Weekday::Wednesday],
            2,
        ))
        .unwrap();
    let original = first_series(&book);

    book.copy_events_on_date(date(2025, 1, 15), "Target", date(2025, 1, 15))
        .unwrap();

    let events = book
        .calendar("Target")
        .unwrap()
        .events_on_date(date(2025, 1, 15));
    assert!(!events.is_empty());
    let new_series = events[0].series().unwrap();
    assert_ne!(new_series, original);
}

#[test]
fn clone_event_null_series_id() {
    let mut book = fixture();
    book.create_calendar("Clone", tz("UTC")).unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("Single", start(), end()))
        .unwrap();
    book.copy_event("Single", start(), "Clone", dt(2025, 1, 16, 10, 0))
        .unwrap();
    assert_eq!(
        book.calendar("Clone").unwrap().all_events()[0].series(),
        None
    );
}

#[test]
fn copy_events_between_date_range() {
    let mut book = fixture();
    book.create_calendar("Range", tz("UTC")).unwrap();
    let cal = book.active_mut().unwrap();
    for (subject, day) in [("Event1", 15), ("Event2", 16), ("Event3", 17)] {
        cal.add_event(single(
            subject,
            dt(2025, 1, day, 10, 0),
            dt(2025, 1, day, 11, 0),
        ))
        .unwrap();
    }

    book.copy_events_between(
        date(2025, 1, 15),
        date(2025, 1, 16),
        "Range",
        date(2025, 2, 1),
    )
    .unwrap();

    let copied = book.calendar("Range").unwrap().all_events();
    assert_eq!(copied.len(), 2);
    assert!(copied.iter().any(|e| e.start().date() == date(2025, 2, 1)));
    assert!(copied.iter().any(|e| e.start().date() == date(2025, 2, 2)));
}

#[test]
fn copy_events_between_with_recurring_series() {
    let mut book = fixture();
    book.create_calendar("Batch", tz("America/Denver")).unwrap();
    book.active_mut()
        .unwrap()
        .add_event(repeating(
            "Series",
            dt(2025, 1, 6, 9, 0),
            dt(2025, 1, 6, 9, 30),
            &[Weekday::Monday, Weekday::Wednesday],
            2,
        ))
        .unwrap();
    let original = first_series(&book);

    book.copy_events_between(
        date(2025, 1, 6),
        date(2025, 1, 8),
        "Batch",
        date(2025, 2, 10),
    )
    .unwrap();

    let copied = book.calendar("Batch").unwrap().all_events();
    assert_eq!(copied.len(), 2);
    assert_ne!(copied[0].series().unwrap(), original);
}

#[test]
fn copy_events_on_date_sorts_events() {
    let mut book = fixture();
    book.create_calendar("Sorted", tz("UTC")).unwrap();
    let cal = book.active_mut().unwrap();
    cal.add_event(single(
        "Late",
        dt(2025, 1, 15, 14, 0),
        dt(2025, 1, 15, 15, 0),
    ))
    .unwrap();
    cal.add_event(single(
        "Early",
        dt(2025, 1, 15, 9, 0),
        dt(2025, 1, 15, 10, 0),
    ))
    .unwrap();
    book.copy_events_on_date(date(2025, 1, 15), "Sorted", date(2025, 1, 15))
        .unwrap();
    assert_eq!(book.calendar("Sorted").unwrap().all_events().len(), 2);
}

#[test]
fn resolve_series_creates_unique_ids() {
    let mut book = fixture();
    book.create_calendar("Multi", tz("UTC")).unwrap();
    let cal = book.active_mut().unwrap();
    cal.add_event(repeating(
        "Series1",
        dt(2025, 1, 15, 9, 0),
        dt(2025, 1, 15, 10, 0),
        &[Weekday::Wednesday],
        1,
    ))
    .unwrap();
    cal.add_event(repeating(
        "Series2",
        dt(2025, 1, 15, 14, 0),
        dt(2025, 1, 15, 15, 0),
        &[Weekday::Wednesday],
        1,
    ))
    .unwrap();
    let originals: HashSet<_> = cal
        .all_events()
        .iter()
        .map(|e| e.series().unwrap())
        .collect();

    book.copy_events_on_date(date(2025, 1, 15), "Multi", date(2025, 1, 15))
        .unwrap();

    let copied: HashSet<_> = book
        .calendar("Multi")
        .unwrap()
        .all_events()
        .iter()
        .map(|e| e.series().unwrap())
        .collect();
    assert_eq!(copied.len(), 2);
    assert!(copied.is_disjoint(&originals));
}

#[test]
fn require_calendar_not_found() {
    let mut book = fixture();
    let err = book
        .copy_event("Event", start(), "NonExistent", start())
        .unwrap_err();
    assert!(matches!(err, CalendarError::CalendarNotFound(_)));
}

#[test]
fn copy_event_source_missing() {
    // Not a Java ModelTest case: EventNotFound path of copyEvent.
    let mut book = fixture();
    book.create_calendar("Dest", tz("UTC")).unwrap();
    let err = book
        .copy_event("Ghost", start(), "Dest", start())
        .unwrap_err();
    assert_eq!(
        err.to_string(),
        "Event not found: Ghost at 2025-01-15T10:00"
    );
}

#[test]
fn copy_clash_in_target_adds_nothing() {
    // Not a Java case: Java kept the copies made before the clash.
    let mut book = fixture();
    book.create_calendar("Dest", tz("America/New_York"))
        .unwrap();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("A", dt(2025, 1, 15, 9, 0), dt(2025, 1, 15, 10, 0)))
        .unwrap();
    cal.add_event(single("B", dt(2025, 1, 15, 11, 0), dt(2025, 1, 15, 12, 0)))
        .unwrap();
    book.use_calendar("Dest").unwrap();
    book.active_mut()
        .unwrap()
        .add_event(single("B", dt(2025, 1, 15, 11, 0), dt(2025, 1, 15, 12, 0)))
        .unwrap();
    book.use_calendar("Default").unwrap();

    let err = book
        .copy_events_on_date(date(2025, 1, 15), "Dest", date(2025, 1, 15))
        .unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateEvent(_)));
    assert_eq!(book.calendar("Dest").unwrap().all_events().len(), 1);
}

// ---- timezones -----------------------------------------------------------

#[test]
fn update_timezone_adjusts_event_times() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Planning", start(), end()))
        .unwrap();
    book.update_calendar_timezone("Default", tz("Europe/London"))
        .unwrap();
    let events = book.active().unwrap().events_on_date(date(2025, 1, 15));
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].start(), dt(2025, 1, 15, 15, 0));
    assert_eq!(events[0].end(), dt(2025, 1, 15, 16, 0));
    assert_eq!(book.active_calendar_zone().unwrap(), tz("Europe/London"));
}

#[test]
fn update_timezone_same_zone_no_op() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Event", start(), end()))
        .unwrap();
    book.update_calendar_timezone("Default", tz("America/New_York"))
        .unwrap();
    let events = book.active().unwrap().all_events();
    assert_eq!(events[0].start(), start());
    assert_eq!(events[0].end(), end());
}

#[test]
fn update_timezone_with_recurring_event() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(monday_standup("Series", 2))
        .unwrap();
    let original = first_series(&book);
    book.update_calendar_timezone("Default", tz("UTC")).unwrap();
    let events = book.active().unwrap().all_events();
    assert_eq!(events.len(), 2);
    assert_eq!(events[0].series(), Some(original));
    assert_eq!(events[0].start(), dt(2025, 1, 6, 14, 0));
}

#[test]
fn update_timezone_unknown_calendar() {
    let mut book = fixture();
    assert!(matches!(
        book.update_calendar_timezone("Nope", tz("UTC")),
        Err(CalendarError::CalendarNotFound(_))
    ));
}

#[test]
fn update_timezone_across_dst_uses_each_events_own_offset() {
    // Not a Java case: 2025-03-20 is after New York's spring-forward, so the
    // gap to London is 4 hours then, but 5 hours on 2025-01-15.
    let mut book = fixture();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Winter", start(), end())).unwrap();
    cal.add_event(single(
        "Spring",
        dt(2025, 3, 20, 10, 0),
        dt(2025, 3, 20, 11, 0),
    ))
    .unwrap();
    book.update_calendar_timezone("Default", tz("Europe/London"))
        .unwrap();
    let starts: Vec<_> = book
        .active()
        .unwrap()
        .all_events()
        .iter()
        .map(|e| e.start())
        .collect();
    assert_eq!(starts, [dt(2025, 1, 15, 15, 0), dt(2025, 3, 20, 14, 0)]);
}

// ---- calendar management -------------------------------------------------

#[test]
fn create_calendar_empty_name() {
    let mut book = Calendars::new();
    assert!(matches!(
        book.create_calendar("", tz("UTC")),
        Err(CalendarError::InvalidInput(_))
    ));
}

#[test]
fn create_calendar_whitespace_name() {
    assert!(Calendars::new().create_calendar("   ", tz("UTC")).is_err());
}

#[test]
fn validate_calendar_name_called() {
    assert!(fixture().create_calendar("  ", tz("UTC")).is_err());
}

#[test]
fn contains_calendar() {
    let mut book = fixture();
    assert!(book.has_active_calendar());
    book.create_calendar("NewCal", tz("UTC")).unwrap();
    let err = book.create_calendar("NewCal", tz("UTC")).unwrap_err();
    assert!(matches!(err, CalendarError::DuplicateCalendar(_)));
    assert!(err.to_string().contains("already exists"));
}

#[test]
fn rename_calendar_to_duplicate_name() {
    let mut book = fixture();
    book.create_calendar("Second", tz("UTC")).unwrap();
    assert!(matches!(
        book.rename_calendar("Default", "Second"),
        Err(CalendarError::DuplicateCalendar(_))
    ));
    assert_eq!(book.list_calendars(), ["Default", "Second"]);
}

#[test]
fn rename_non_existent_calendar() {
    let mut book = fixture();
    assert!(matches!(
        book.rename_calendar("DoesNotExist", "NewName"),
        Err(CalendarError::CalendarNotFound(_))
    ));
}

#[test]
fn rename_active_calendar_updates_name() {
    let mut book = fixture();
    assert_eq!(book.active_calendar_name(), Some("Default"));
    book.rename_calendar("Default", "Renamed").unwrap();
    assert_eq!(book.active_calendar_name(), Some("Renamed"));
}

#[test]
fn rename_inactive_calendar_preserves_active() {
    let mut book = fixture();
    book.create_calendar("Other", tz("UTC")).unwrap();
    book.use_calendar("Default").unwrap();
    book.rename_calendar("Other", "OtherRenamed").unwrap();
    assert_eq!(book.active_calendar_name(), Some("Default"));
}

#[test]
fn rename_keeps_position_and_allows_same_name() {
    // Not a Java case: documents the one deliberate difference in PLAN.md.
    let mut book = fixture();
    book.create_calendar("Second", tz("UTC")).unwrap();
    book.rename_calendar("Default", "First").unwrap();
    assert_eq!(book.list_calendars(), ["First", "Second"]);
    book.rename_calendar("First", "First").unwrap();
}

#[test]
fn list_calendars() {
    let mut book = fixture();
    book.create_calendar("Cal1", tz("UTC")).unwrap();
    book.create_calendar("Cal2", tz("UTC")).unwrap();
    assert_eq!(book.list_calendars(), ["Default", "Cal1", "Cal2"]);
}

#[test]
fn get_active_calendar_zone() {
    assert_eq!(
        fixture().active_calendar_zone().unwrap(),
        tz("America/New_York")
    );
}

#[test]
fn first_calendar_created_becomes_active() {
    let mut book = Calendars::new();
    book.create_calendar("One", tz("UTC")).unwrap();
    book.create_calendar("Two", tz("UTC")).unwrap();
    assert_eq!(book.active_calendar_name(), Some("One"));
}

#[test]
fn use_calendar_not_found() {
    assert!(matches!(
        fixture().use_calendar("Nope"),
        Err(CalendarError::CalendarNotFound(_))
    ));
}

#[test]
fn has_active_calendar_when_none() {
    let book = Calendars::new();
    assert!(!book.has_active_calendar());
    assert_eq!(book.active_calendar_name(), None);
}

#[test]
fn add_event_without_active_calendar() {
    let mut book = Calendars::new();
    assert!(matches!(
        book.active_mut(),
        Err(CalendarError::NoActiveCalendar)
    ));
    assert_eq!(
        book.active_calendar_zone(),
        Err(CalendarError::NoActiveCalendar)
    );
}

#[test]
fn add_single_event_does_not_track_non_recurring_series() {
    let mut book = fixture();
    book.active_mut()
        .unwrap()
        .add_event(single("Event", start(), end()))
        .unwrap();
    let events = book.active().unwrap().all_events();
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].series(), None);
}

#[test]
fn event_status_survives_copy_and_edit() {
    // Not a Java case: status travels with the event through both operations.
    let mut book = fixture();
    book.create_calendar("Dest", tz("UTC")).unwrap();
    let cal = book.active_mut().unwrap();
    cal.add_event(single("Secret", start(), end()).with_status(EventStatus::Private))
        .unwrap();
    cal.edit_event("Secret", start(), &rename_to("Secret2"))
        .unwrap();
    book.copy_event("Secret2", start(), "Dest", start())
        .unwrap();
    assert_eq!(
        book.calendar("Dest").unwrap().all_events()[0].status(),
        EventStatus::Private
    );
}

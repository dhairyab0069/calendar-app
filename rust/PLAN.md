# Rust port plan

The Java app (CS5010 calendar, MVC, Swing GUI) stays untouched under `src/`.
This folder is the port. It is built in phases; each phase ends with a check
you can run. Phase 1 (the model) is done and lives in `rust/src`.

## 0. What the Java app is (facts the plan relies on)

- A model holds several named calendars, each with a timezone (`ZoneId`). One
  calendar is "in use". The first one created is selected automatically.
- An event has subject, start, end, description, location and status
  (`public`/`private`). Start and end are zone-less `LocalDateTime`s: the
  calendar's zone says what they mean.
- Identity is (subject, start, end). Adding an event with the same triple is a
  `DuplicateEventException`. Events are indexed by start date only.
- Recurring events (`repeats MWF for 4 times` / `until <date>`; codes
  `M T W R F S U`) are expanded at add time into ordinary events that share a
  series id. The template (days, count) is not kept. Expansion walks day by day
  and gives up after 10 000 days. The template must start and end on one day.
- Editing: `edit event` changes one event (and detaches it from its series),
  `edit events` changes this occurrence and all later ones, `edit series`
  changes all of them. Editable properties: subject, start, end, description,
  location, status.
- Copying between calendars converts through instants (`copy event` shifts the
  event, and its whole series, by the difference between the two start times;
  `copy events on/between` keeps day offsets and re-expresses times in the
  target zone). Changing a calendar's timezone converts all its events.
- Command language (one command per line, case-insensitive keywords):
  `create calendar --name N --timezone Z`, `edit calendar --name N --property
  name|timezone V`, `use calendar --name N`, `create [event] S from DT to DT
  [repeats DAYS (for N times | until D)]`, `create [event] S on D` (all-day is
  08:00-17:00), `edit event|events|series PROP S from DT with V`,
  `print [events] on D`, `print [events] from DT to DT`, `view day|week|month
  ...`, `show status on DT` / `status DT`, `copy event|events ...  --target C
  to ...`, `export [cal] FILE` (`.csv`, `.ical`, `.ics`), `exit`. Trailing
  `on <calendar>` / `at tz <zone>` clauses select a calendar or a display zone.
  Dates are `YYYY-MM-DD`, date-times `YYYY-MM-DDThh:mm`.
- Modes: `--mode headless FILE` (skips blank lines and `#` comments, warns if
  there is no `exit`), `--mode interactive` (prompt `> `), no arguments = Swing
  GUI (default calendar "Personal" in the system zone). Bad arguments print
  usage.
- Export: CSV (`Subject,Start,End,Description,Location,Status`, sorted by
  start, RFC-4180-style quoting) and iCal (`VCALENDAR` with `X-WR-CALNAME`,
  `TZID` on every `DTSTART`/`DTEND`, random UIDs). Import exists for both and is
  used by the GUI.

## 1. Java to Rust mapping

| Java | Rust module | Idiom replacing it |
|---|---|---|
| `exceptions.DuplicateEvent/EventNotFound/DuplicateCalendar/CalendarNotFound`, plus the `IllegalArgument/IllegalState` used by the model | `error.rs` `CalendarError` (done) | One enum, `Result<T, CalendarError>`, hand-written `Display` + `std::error::Error`. No `thiserror`: seven variants do not justify a proc-macro. Messages copied from Java. |
| `exceptions.InvalidCommandException`, `FileExportException` | `command::ParseError`, `export::ExportError` (phases 2, 3) | Same pattern; `From` impls so `?` converts between layers. |
| `model.Weekday` | `weekday.rs` (done) | Plain enum + `from_char`/`parse_str`; converts to `chrono::Weekday`. |
| `model.EventStatus` | `status.rs` (done) | Enum + `FromStr` + `Display`; `Default = Public`. |
| `model.CalendarEvent` + `Builder` | `event.rs` `Event` (done) | Private fields, validating `Event::new`, by-value `with_*`. No builder type: only optional fields exist and they have defaults. `from(event)` is `clone()`. |
| Nullable params of `editEvent/editSeries*` | `EventChanges` (done) | Struct of `Option`s with `..Default::default()` instead of six nullable arguments. |
| `repeatCount` + `repeatUntil` fields and the builder code that clears one when the other is set | `RecurrenceEnd` enum (done) | Make the invalid state unrepresentable. |
| `CalendarEvent.equals/hashCode` on (subject, start, end) | `EventKey` via `Event::key()` (done) | The rule is named; `Event` has no `==` that ignores half its fields. |
| `model.CalendarModel` (interface) | none | One implementation, so no trait. Add one only if a second model appears (e.g. a persisted one). |
| `model.SparseHashCalendar` | `calendars.rs` `Calendars` (done) | Owns `Vec<Calendar>` and an active index. Active calendar by index instead of by name, so rename cannot break it. |
| `model.SingleCalendarStore` + `CalendarContainer` | `calendar.rs` `Calendar` (done) | One `BTreeMap<NaiveDate, Vec<Event>>` as the only store; the second `eventsBySeries` index is derived from `Event::series`. Mutations are atomic (clone, mutate, restore on error). |
| Series UUID strings | `SeriesId(u64)` from an atomic counter (done) | `Copy` newtype; no `uuid` dependency. |
| `controller.util.DateTimeParser` | `command/datetime.rs` (phase 2) | `chrono::NaiveDateTime::parse_from_str` with the two formats. |
| `controller.parser.*` (`CommandParser`, 14 `*Params`, `ParsedCommand`, `CommandType`) | `command/parse.rs` (phase 2) | One `enum Command` whose variants carry their data; replaces the type tag plus untyped params object plus casts. |
| `controller.parser.ExportParams` (filename sanitising) | `export.rs` `sanitize_filename` (phase 3) | Plain function. |
| `controller.command.*` (`Command`, `CommandRegistry`, `CommandContext`) | `command/session.rs` (phase 2) | The registry is a map from enum tag to handler; a `match` on `Command` does that job. `CommandContext` becomes `Context { calendar: Option<String>, zone: Option<Tz> }`. |
| `controller.CommandHandler` | `command/session.rs` `Session` (phase 2) | Owns `Calendars`; `execute(&mut self, Parsed) -> Result<Output, Error>`. Returns data; it does not print. |
| `view.ConsoleView`, `DayView`, `WeekView`, `MonthView`, `CalendarView` | `render.rs` (phase 2) | Pure functions returning `String`. The view interface had one real implementation per mode; with outputs as data no trait is needed. |
| `controller.HeadlessController`, `InteractiveController`, `CalendarController` | `cli.rs` (phases 2, 4) | Two functions over `impl BufRead` / `impl Write`, both feeding `Session::run_line`. Testable without capturing process streams. |
| `CalendarApp`, `CalendarRunner` | `main.rs` | Hand-written argv match for the three accepted shapes. |
| `view.gui.*`, `controller.gui.*` (Swing, `PropertyChange` events, `PropertyType`, `*EventData`) | `gui/` (phase 5) | egui is immediate-mode: the `App` struct holds state and calls `Session`/`Calendars` directly, so the observer plumbing disappears. |

## 2. Key decisions

Time handling: `chrono` + `chrono-tz`.
- Reason: `chrono::NaiveDateTime` is exactly `LocalDateTime`, and
  `chrono-tz` is the IANA database that `ZoneId.of("America/New_York")` uses.
  `egui_extras`' date picker also uses chrono, which helps in phase 5.
- Rejected: the `time` crate (no timezone database of its own).
- Considered: `jiff` has the right gap/overlap policy built in (`compatible`
  matches Java) and may be nicer. chrono costs about 25 lines (`zone.rs`).
  Revisit if timezone code grows.
- Where zones attach: the calendar owns its `Tz`; events hold naive local
  times. Instants are computed only when crossing zones (change a calendar's
  zone, copy to another calendar). This is what Java did, so behaviour is
  identical.
- DST: Java's rules are reproduced in `zone::to_instant`. A time in a spring-
  forward gap moves forward by the gap length; a time in a fall-back overlap
  takes the earlier instant. Covered by three unit tests plus a conversion
  test across a DST change.

Event identity and storage
- An event is identified by (subject, start, end). Commands address events by
  (subject, start); there is no event id. A stable `EventId` would help the GUI
  but changes the model, so it is not part of the port.
- Storage is `BTreeMap<NaiveDate, Vec<Event>>` keyed by start date. Queries by
  day/range are cheap; output is date-ordered (Java's `HashMap` order was
  unspecified, so no test depends on it).

Recurring series
- Same as Java: expand eagerly into plain events sharing a `SeriesId`; throw
  the template away. This keeps edits and copies trivially correct.
- Cost: the app cannot say "repeats MWF" later. If the GUI needs it, add a
  `HashMap<SeriesId, Recurrence>` to `Calendar`. Not built now.

Command parsing: hand-written, in the same order as `CommandParser.parse`.
- The language is not argv-shaped: unquoted subjects may contain spaces, and
  keywords such as ` from `, ` to `, ` with ` are found by scanning. That rules
  out `clap`. A parser-combinator crate (`nom`, `pest`) is overkill for ~20
  commands and makes it harder to copy Java's error messages.
- Keywords and flags stay identical so `commands.txt` and `invalid.txt` run
  unchanged. Before porting the parser, port `CommandTest` and `CalendarTest`
  first: they pin the quirks.

Export and import
- Hand-written CSV and iCal writers. Both formats are a fixed handful of
  properties; `csv`/`ical` crates add dependencies for no gain on output.
- For import, the Java CSV reader splits on newlines before handling quotes
  (so quoted newlines break). Using the `csv` crate would fix that but changes
  behaviour; decided in phase 3 (open question 6).
- The clock and UID source are injected, so iCal output is deterministic in
  tests (Java used `now()` and random UUIDs).

Headless and interactive modes
- `calendar --mode headless FILE`, `calendar --mode interactive`, `calendar`
  (GUI). Parsed by hand; no `clap` for three shapes. Bad arguments print the
  same usage; the exit code becomes non-zero (Java returned 0; open question 8).
- Both modes read lines, call `Session::run_line`, print the returned output.
  Headless keeps Java's rules: skip blank and `#` lines, stop at `exit`, warn
  when `exit` is missing, prefix errors with `Line N:`.

GUI: recommendation is egui (eframe), added last, after a terminal-only release.
- Reason: your abiogenesis project already uses `eframe` 0.36 on edition 2024,
  so you know the toolchain; one native binary, no JVM, which is the point of
  the port.
- Month/week/day grids, dialogs and a calendar list map well to immediate mode.
- `ratatui` is a good fit only if you want a TUI instead of a window. The
  README's GUI requirements (dialogs, date picking) lean towards egui.
- The model crate stays GUI-free so either front end can sit on top. Splitting
  into a cargo workspace (`calendar` lib + `calendar-cli` + `calendar-gui`)
  is a phase 5 step, not before.
- Needs later: system timezone detection for the default calendar (a small
  crate such as `iana-time-zone`), and a file dialog for import/export (`rfd`).

## 3. Phases

| Phase | Scope | Done when |
|---|---|---|
| 1 Model + tests | `Event`, `Calendar`, `Calendars`, series, copy, timezone conversion | Done. `cargo test`, `cargo clippy --all-targets -- -D warnings`, `cargo fmt --check` pass; the model cases of `ModelTest` and `CalendarTest` are ported (table below). |
| 2 Parser + headless runner | `DateTimeParser`, `Command` enum, parser, `Session`, day/week/month rendering, `calendar --mode headless FILE` | Running `commands.txt` prints the same text as the Java app (capture a golden file from the Java run first); `invalid.txt` produces the same errors; `CommandTest`, `CalendarTest` (command cases) and the handler/view parts of `ControllerTest` are ported. |
| 3 Export and import | CSV and iCal writers, importers, `sanitize_filename`, `export` command | `ExportParamsTest` and the export/import cases of `ModelTest` ported; exported CSV/iCal match Java output (UID and DTSTAMP normalised); round trip CSV to CSV is lossless for the fields that exist. |
| 4 Interactive CLI | `--mode interactive`, argument validation, usage text | Scripted stdin session gives the same transcript as headless for the same commands; bad arguments behave as decided in open question 8. |
| 5 GUI | egui app: month/week/day views, calendar list, create/edit with this/forward/all scope, import/export | Every item in README section 1.2 and the USEME GUI workflow can be done by hand; the GUI only calls `Session`/`Calendars`, no logic of its own. |

## 4. Testing strategy

- Port Java tests case by case into Rust, keeping the name (`testEditEventSubject`
  becomes `edit_event_subject`). Unit-level cases sit next to the code
  (`weekday.rs`, `status.rs`, `event.rs`); model-behaviour cases are in
  `rust/tests/model_test.rs` and `rust/tests/calendar_test.rs` and use only the
  public API, so they are real black-box tests.
- Cases with no Rust meaning (null arguments, defensive copies) are listed as
  "not applicable" with the reason, not silently dropped.
- Where Java behaviour looked accidental and Rust does something saner, the
  Rust test says "Not a Java case" and explains. These are the only places
  where behaviour differs (open questions below).
- Phase 2 onwards: golden-file tests. Run the Java app once on `commands.txt`,
  `invalid.txt` and a few scripts, store the output under `rust/tests/golden/`,
  compare byte for byte.
- Other Java test files (not enumerated per case; ported in the phase noted):
  `CommandTest` (67) and `ControllerTest` (282), `CalendarControllerTests`
  (102), `CalendarAppTest` (34) in phases 2 and 4; `ViewTest` (72) phase 2
  for the console views; `ExportParamsTest` (6) phase 3. Swing-only tests are
  dropped and replaced by manual GUI checks in phase 5.

## 5. Risks and open questions

Risks
- Java behaviour is only partly documented; golden files from a real Java run
  are the safety net. Not captured yet.
- Parser quirks: substring scanning (`indexOf(" from ")`) has surprising edge
  cases. They only matter if a test or sample pins them.
- Timezone naming: `chrono-tz` knows IANA names only, while `ZoneId.of` also
  accepts offsets such as `+05:00` and `Z`.
- Atomic edits clone the day map per edit. Fine for personal calendars;
  re-measure if someone imports thousands of events.

Open questions for you (Java behaviour I did not want to guess at)
1. `edit events|series start ...` gives every selected occurrence the same
   new start, so for two or more occurrences the second one collides with the
   first and the command fails. Intended, or should it shift all by the same
   delta?
2. Events are indexed by start date only. A multi-day event does not appear
   on later days, and `is_busy_at` ignores it there. Kept for now
   (`multi_day_event_is_only_indexed_on_its_start_date` pins it). Fix?
3. `end == start` is accepted by the model although the message says "must be
   after". Kept.
4. Renaming a calendar moves it to the end of `listCalendars()` in Java (remove
   then put). Rust keeps its position. Which do you want?
5. Fixed-offset or alias zones (`+05:00`, `EST`): support them, or IANA names only?
6. CSV export writes `Description,Location` but import reads column 4 as
   location and column 5 as description, so a round trip swaps them. Fix on both
   sides in phase 3? Import also reads line by line, so quoted newlines break.
7. iCal import ignores `TZID` and treats a trailing `Z` as local time. Keep or
   convert properly?
8. Bad CLI arguments return exit code 0 in Java. Switch to non-zero?
9. A failed edit or copy leaves Java half-applied (series edit, copy into a
   target with a clash, and an edit whose new end is before start loses the
   event). Rust rolls back. Accepted as a deliberate fix?
10. `ExportParams` turns `/` into `_`, so exports can never go into a
    subfolder, yet the handler creates parent directories. Which is intended?
11. Should the series template (`MWF`, count) be kept so the GUI can show it?
12. Default GUI calendar is "Personal" in the system zone; confirm for Phase 5.
13. Java's own tests were not run for this plan (no Gradle run). Do the 276
    tests in `ModelTest` and `CalendarTest` all pass today?

## 6. Java test port table

Scope of the table: `ModelTest` (204 cases) and `CalendarTest` (72 cases).
Other test files are covered in section 4.

Totals: ported 123 (ModelTest 112, CalendarTest 11), not applicable 21, deferred to phase 2 76, deferred to phase 3 56. That is 276 cases. Some Java cases share a Rust test (the two `...AdjustsEventTimes` cases), and 24 of the 146 Rust tests have no Java twin (added for Rust-specific behaviour or deliberate fixes; most say "Not a Java case" in a comment).

### Ported

| Java test | Rust test | File |
|---|---|---|
| ModelTest.testWeekdayFromChar | `weekday_from_char` | `src/weekday.rs` |
| ModelTest.testWeekdayFromCharLowercase | `weekday_from_char_lowercase` | `src/weekday.rs` |
| ModelTest.testWeekdayFromCharInvalid | `weekday_from_char_invalid` | `src/weekday.rs` |
| ModelTest.testWeekdayParseString | `weekday_parse_string` | `src/weekday.rs` |
| ModelTest.testWeekdayParseStringAllDays | `weekday_parse_string_all_days` | `src/weekday.rs` |
| ModelTest.testWeekdayParseStringEmpty | `weekday_parse_string_empty` | `src/weekday.rs` |
| ModelTest.testWeekdayFromDayOfWeek | `weekday_from_day_of_week` | `src/weekday.rs` |
| ModelTest.testWeekdayGetAbbreviation | `weekday_get_abbreviation` | `src/weekday.rs` |
| ModelTest.testWeekdayGetDayOfWeek | `weekday_get_day_of_week` | `src/weekday.rs` |
| ModelTest.testWeekdayToString | `weekday_to_string` | `src/weekday.rs` |
| ModelTest.testEventStatusFromString | `event_status_from_string` | `src/status.rs` |
| ModelTest.testEventStatusFromStringCaseInsensitive | `event_status_from_string_case_insensitive` | `src/status.rs` |
| ModelTest.testEventStatusFromStringWithWhitespace | `event_status_from_string_with_whitespace` | `src/status.rs` |
| ModelTest.testEventStatusFromStringInvalid | `event_status_from_string_invalid` | `src/status.rs` |
| ModelTest.testEventStatusGetValue | `event_status_get_value` | `src/status.rs` |
| ModelTest.testEventStatusToString | `event_status_to_string` | `src/status.rs` |
| ModelTest.testCreateSingleEvent | `create_single_event` | `src/event.rs` |
| ModelTest.testCreateEventDefaults | `create_event_defaults` | `src/event.rs` |
| ModelTest.testCreateEventTrimSubject | `create_event_trim_subject` | `src/event.rs` |
| ModelTest.testCreateEventEmptySubject | `create_event_empty_subject` | `src/event.rs` |
| ModelTest.testCreateEventWhitespaceSubject | `create_event_whitespace_subject` | `src/event.rs` |
| ModelTest.testCreateEventEndBeforeStart | `create_event_end_before_start` | `src/event.rs` |
| ModelTest.testBuilderStartAfterEndThrows | `builder_start_after_end_throws` | `src/event.rs` |
| ModelTest.testBuilderEndBeforeStartThrows | `builder_end_before_start_throws` | `src/event.rs` |
| ModelTest.testCreateRecurringEvent | `create_recurring_event` | `src/event.rs` |
| ModelTest.testCreateRecurringEventWithUntil | `create_recurring_event_with_until` | `src/event.rs` |
| ModelTest.testCreateRecurringEventEmptyDays | `create_recurring_event_empty_days` | `src/event.rs` |
| ModelTest.testFromCopiesRecurringFieldsAndIsDefensive | `from_copies_recurring_fields_and_is_defensive` | `src/event.rs` |
| ModelTest.testFromCopiesRecurringEventWithRepeatUntil | `from_copies_recurring_event_with_repeat_until` | `src/event.rs` |
| ModelTest.testCreateRecurringEventZeroCount | `create_recurring_event_zero_count` | `src/event.rs` |
| ModelTest.testRepeatCountBoundaryZero | `repeat_count_boundary_zero` | `src/event.rs` |
| ModelTest.testWithDescription | `with_description` | `src/event.rs` |
| ModelTest.testWithLocation | `with_location` | `src/event.rs` |
| ModelTest.testWithStatus | `with_status` | `src/event.rs` |
| ModelTest.testWithTimes | `with_times` | `src/event.rs` |
| ModelTest.testEventEqualsEqualObjects | `event_equals_equal_objects` | `src/event.rs` |
| ModelTest.testEventEqualsDifferentSubject | `event_equals_different_subject` | `src/event.rs` |
| ModelTest.testEventEqualsDifferentStart | `event_equals_different_start` | `src/event.rs` |
| ModelTest.testEventHashCode | `event_hash_code` | `src/event.rs` |
| ModelTest.testEventToString | `event_to_string` | `src/event.rs` |
| ModelTest.testEventToStringRecurring | `event_to_string_recurring` | `src/event.rs` |
| ModelTest.testAddSingleEvent | `add_single_event` | `tests/model_test.rs` |
| ModelTest.testAddDuplicateEvent | `add_duplicate_event` | `tests/model_test.rs` |
| ModelTest.testAddMultipleEventsSameDay | `add_multiple_events_same_day` | `tests/model_test.rs` |
| ModelTest.testAddRecurringEventWithCount | `add_recurring_event_with_count` | `tests/model_test.rs` |
| ModelTest.testAddRecurringEventWithUntil | `add_recurring_event_with_until` | `tests/model_test.rs` |
| ModelTest.testAddRecurringEventDuplicate | `add_recurring_event_duplicate` | `tests/model_test.rs` |
| ModelTest.testRecurringEventSingleDayArray | `recurring_event_single_day_array` | `tests/model_test.rs` |
| ModelTest.testRecurrenceDaysLengthBoundary | `recurrence_days_length_boundary` | `src/event.rs` |
| ModelTest.testGetEventsOnDate | `get_events_on_date` | `tests/model_test.rs` |
| ModelTest.testGetEventsOnDateEmpty | `get_events_on_date_empty` | `tests/model_test.rs` |
| ModelTest.testGetEventsInRange | `get_events_in_range` | `tests/model_test.rs` |
| ModelTest.testGetEventsInRangeSingleDay | `get_events_in_range_single_day` | `tests/model_test.rs` |
| ModelTest.testGetAllEvents | `get_all_events` | `tests/model_test.rs` |
| ModelTest.testGetAllEventsEmpty | `get_all_events_empty` | `tests/model_test.rs` |
| ModelTest.testIsBusyAtDuringEvent | `is_busy_at_during_event` | `tests/model_test.rs` |
| ModelTest.testIsBusyAtBeforeEvent | `is_busy_at_before_event` | `tests/model_test.rs` |
| ModelTest.testIsBusyAtAfterEvent | `is_busy_at_after_event` | `tests/model_test.rs` |
| ModelTest.testIsBusyAtEmptyDay | `is_busy_at_empty_day` | `tests/model_test.rs` |
| ModelTest.testEditEventSubject | `edit_event_subject` | `tests/model_test.rs` |
| ModelTest.testEditEventNullDescriptionPreservesOriginal | `edit_event_null_description_preserves_original` | `tests/model_test.rs` |
| ModelTest.testEditEventNullLocationPreservesOriginal | `edit_event_null_location_preserves_original` | `tests/model_test.rs` |
| ModelTest.testEditEventNotFound | `edit_event_not_found` | `tests/model_test.rs` |
| ModelTest.testEditEventCreatesDuplicate | `edit_event_creates_duplicate` | `tests/model_test.rs` |
| ModelTest.testEditRecurringEventDetachesFromSeries | `edit_recurring_event_detaches_from_series` | `tests/model_test.rs` |
| ModelTest.testRecurringEventSeriesTracking | `recurring_event_series_tracking` | `tests/model_test.rs` |
| ModelTest.testRecurringEventSafetyLimit | `recurring_event_safety_limit` | `tests/model_test.rs` |
| ModelTest.testRecurringEventSafetyLimitExact | `recurring_event_safety_limit_exact` | `tests/model_test.rs` |
| ModelTest.testRecurringEventCountBoundary | `recurring_event_count_boundary` | `tests/model_test.rs` |
| ModelTest.testIsBusyAtBoundaryTimes | `is_busy_at_boundary_times` | `tests/model_test.rs` |
| ModelTest.testEditSeriesFromDateCreatesDuplicate | `edit_series_from_date_creates_duplicate` | `tests/model_test.rs` |
| ModelTest.testEditSeriesFromDateNullsPreserveOriginal | `edit_series_from_date_nulls_preserve_original` | `tests/model_test.rs` |
| ModelTest.testEditEntireSeriesNullsPreserveOriginal | `edit_entire_series_nulls_preserve_original` | `tests/model_test.rs` |
| ModelTest.testEditSeriesFromDateIndexBoundary | `edit_series_from_date_index_boundary` | `tests/model_test.rs` |
| ModelTest.testEditEntireSeriesIndexBoundary | `edit_entire_series_index_boundary` | `tests/model_test.rs` |
| ModelTest.testEditEntireSeriesCreatesDuplicate | `edit_entire_series_creates_duplicate` | `tests/model_test.rs` |
| ModelTest.testEditSingleOccurrenceDetachesFromSeries | `edit_single_occurrence_detaches_from_series` | `tests/model_test.rs` |
| ModelTest.testEditSeriesFromDateFilterBoundary | `edit_series_from_date_filter_boundary` | `tests/model_test.rs` |
| ModelTest.testCopyEventsOnDateBetweenCalendars | `copy_events_on_date_between_calendars` | `tests/model_test.rs` |
| ModelTest.testCopyEventToTargetCalendar | `copy_event_to_target_calendar` | `tests/model_test.rs` |
| ModelTest.testCopyRecurringEventCopiesSeries | `copy_recurring_event_copies_series` | `tests/model_test.rs` |
| ModelTest.testCopyEventsAlignDateTimeCalculation | `copy_events_align_date_time_calculation` | `tests/model_test.rs` |
| ModelTest.testUpdateTimezoneAdjustsEventTimes | `update_timezone_adjusts_event_times` | `tests/model_test.rs` |
| ModelTest.testUpdateTimezoneSameZoneNoOp | `update_timezone_same_zone_no_op` | `tests/model_test.rs` |
| ModelTest.testUpdateTimezoneWithRecurringEvent | `update_timezone_with_recurring_event` | `tests/model_test.rs` |
| ModelTest.testCreateCalendarEmptyName | `create_calendar_empty_name` | `tests/model_test.rs` |
| ModelTest.testCreateCalendarWhitespaceName | `create_calendar_whitespace_name` | `tests/model_test.rs` |
| ModelTest.testRenameCalendarToDuplicateName | `rename_calendar_to_duplicate_name` | `tests/model_test.rs` |
| ModelTest.testRenameNonExistentCalendar | `rename_non_existent_calendar` | `tests/model_test.rs` |
| ModelTest.testRenameActiveCalendarUpdatesName | `rename_active_calendar_updates_name` | `tests/model_test.rs` |
| ModelTest.testRenameInactiveCalendarPreservesActive | `rename_inactive_calendar_preserves_active` | `tests/model_test.rs` |
| ModelTest.testListCalendars | `list_calendars` | `tests/model_test.rs` |
| ModelTest.testGetActiveCalendarZone | `get_active_calendar_zone` | `tests/model_test.rs` |
| ModelTest.testCopyEventsSingleEventNoSeries | `copy_events_single_event_no_series` | `tests/model_test.rs` |
| ModelTest.testCopyEventsOnDateWithRecurringSeries | `copy_events_on_date_with_recurring_series` | `tests/model_test.rs` |
| ModelTest.testUpdateTimezoneAdjustsEventTimes2 | `update_timezone_adjusts_event_times` | `tests/model_test.rs` |
| ModelTest.testValidateCalendarNameCalled | `validate_calendar_name_called` | `tests/model_test.rs` |
| ModelTest.testCloneEventNullSeriesId | `clone_event_null_series_id` | `tests/model_test.rs` |
| ModelTest.testAddSingleEventDoesNotTrackNonRecurringSeries | `add_single_event_does_not_track_non_recurring_series` | `tests/model_test.rs` |
| ModelTest.testEditSeriesCleanupWhenLastEventRemoved | `edit_series_cleanup_when_last_event_removed` | `tests/model_test.rs` |
| ModelTest.testEditSeriesPreservesSeriesId | `edit_series_preserves_series_id` | `tests/model_test.rs` |
| ModelTest.testCopyEventsBetweenDateRange | `copy_events_between_date_range` | `tests/model_test.rs` |
| ModelTest.testCopyEventsBetweenWithRecurringSeries | `copy_events_between_with_recurring_series` | `tests/model_test.rs` |
| ModelTest.testHasActiveCalendarWhenNone | `has_active_calendar_when_none` | `tests/model_test.rs` |
| ModelTest.testAddEventWithoutActiveCalendar | `add_event_without_active_calendar` | `tests/model_test.rs` |
| ModelTest.testContainsCalendar | `contains_calendar` | `tests/model_test.rs` |
| ModelTest.testRequireCalendarNotFound | `require_calendar_not_found` | `tests/model_test.rs` |
| ModelTest.testMatchesRecurrencePatternAllDays | `matches_recurrence_pattern_all_days` | `tests/model_test.rs` |
| ModelTest.testShouldContinueGeneratingCountBoundary | `should_continue_generating_count_boundary` | `tests/model_test.rs` |
| ModelTest.testShouldContinueGeneratingUntilBoundary | `should_continue_generating_until_boundary` | `tests/model_test.rs` |
| ModelTest.testCopyEventsOnDateSortsEvents | `copy_events_on_date_sorts_events` | `tests/model_test.rs` |
| ModelTest.testResolveSeriesCreatesUniqueIds | `resolve_series_creates_unique_ids` | `tests/model_test.rs` |
| CalendarTest.testEventPersistence | `event_persistence` | `tests/calendar_test.rs` |
| CalendarTest.testEventCount | `event_count` | `tests/calendar_test.rs` |
| CalendarTest.testSeriesRelationships | `series_relationships` | `tests/calendar_test.rs` |
| CalendarTest.testMidnightEvents | `midnight_events` | `tests/calendar_test.rs` |
| CalendarTest.testEndOfDayEvents | `end_of_day_events` | `tests/calendar_test.rs` |
| CalendarTest.testYearBoundary | `year_boundary` | `tests/calendar_test.rs` |
| CalendarTest.testLeapYearDate | `leap_year_date` | `tests/calendar_test.rs` |
| CalendarTest.testEmptyDateQueries | `empty_date_queries` | `tests/calendar_test.rs` |
| CalendarTest.testSingleDayRange | `single_day_range` | `tests/calendar_test.rs` |
| CalendarTest.testBusyDayMultipleEvents | `busy_day_multiple_events` | `tests/calendar_test.rs` |
| CalendarTest.testMultipleRecurringSeries | `multiple_recurring_series` | `tests/calendar_test.rs` |

### Not applicable in Rust

| Java test | Why |
|---|---|
| ModelTest.testWeekdayParseStringNull | no null |
| ModelTest.testEventStatusFromStringNull | no null |
| ModelTest.testCreateEventNullSubject | no null in Rust; `&str` cannot be null (blank case ported) |
| ModelTest.testCreateEventNullStart | no null: `NaiveDateTime` is not optional |
| ModelTest.testCreateEventNullEnd | no null: `NaiveDateTime` is not optional |
| ModelTest.testBuilderStartNullThrows | no null; no separate start setter (see `builder_start_after_end_throws`) |
| ModelTest.testBuilderEndNullThrows | no null; no separate end setter |
| ModelTest.testCreateRecurringEventNullDays | no null: days is a slice |
| ModelTest.testCreateRecurringEventNoTermination | unrepresentable: `RecurrenceEnd` is a required enum |
| ModelTest.testCreateRecurringEventNegativeCount | unrepresentable: count is `u32` (zero case ported) |
| ModelTest.testRecurrenceDaysBuilderReturnsThis | no builder type; chaining is by value |
| ModelTest.testWithDescriptionNull | no null; empty string is the only "no description" |
| ModelTest.testWithLocationNull | no null; empty string is the only "no location" |
| ModelTest.testWithStatusNull | no null: `EventStatus` is not optional |
| ModelTest.testEventEqualsSameObject | `Event` has no `==`; identity is `key()` (reflexive by construction) |
| ModelTest.testEventEqualsNull | no null |
| ModelTest.testEventEqualsDifferentClass | no `Object.equals`; static types |
| ModelTest.testAddNullEvent | no null |
| ModelTest.testGetEventsOnDateDefensiveCopy | queries return borrows (`Vec<&Event>`); mutation is impossible |
| ModelTest.testCreateCalendarNullName | no null (blank cases ported) |
| ModelTest.testGetSeriesEventsReturnsDefensiveCopy | only asserted a series id exists; covered by `recurring_event_series_tracking` |

### Deferred to phase 2 (command layer)

Reason: they test `DateTimeParser` or go through `CommandParser`/`CommandHandler`/views, which do not exist yet.

- ModelTest (18): `ParseDateTimeValid`, `ParseDateTimeWithWhitespace`, `ParseDateTimeNull`, `ParseDateTimeEmpty`, `ParseDateTimeEmptyWhitespace`, `ParseDateTimeInvalidFormat`, `ParseDateTimeInvalidFormatDateOnly`, `ParseDateValid`, `ParseDateWithWhitespace`, `ParseDateNull`, `ParseDateEmpty`, `ParseDateInvalidFormat`, `FormatDateTime`, `FormatDateTimeNull`, `FormatDate`, `FormatDateNull`, `IsValidDateTime`, `IsValidDate`
- CalendarTest (58): `CreateAndRetrieveSingleEvent`, `CreateWithoutEventKeyword`, `CreateAndRetrieveAllDayEvent`, `CreateRecurringEventWithFor`, `CreateRecurringEventWithoutFor`, `CreateRecurringEventUntilDate`, `DuplicateEventError`, `PrintEventsOnSyntax`, `PrintEventsInRange`, `PrintEventsFromSyntax`, `PrintNoEvents`, `PrintNoEventsInRange`, `BusyStatus`, `AvailableStatus`, `QuotedSubject`, `ViewDayCommand`, `ViewWeekCommand`, `ViewMonthShortFormat`, `ViewMonthFullDate`, `ExitCommand`, `RecurringMwfPattern`, `RecurringOnlyMatchingDays`, `RecurringDuplicatePrevention`, `AllDayRecurringEvent`, `EmptyCommand`, `WhitespaceCommand`, `NullCommand`, `UnknownCommand`, `CreateWithoutTime`, `CreateInvalidDateFormat`, `CreateWithoutTo`, `CreateEndBeforeStart`, `CreateEndEqualsStart`, `CreateUnclosedQuote`, `CreateEmptySubject`, `PrintInvalidDateFormat`, `PrintRangeWithoutTo`, `StatusInvalidFormat`, `RecurringInvalidWeekdays`, `RecurringNegativeCount`, `RecurringZeroCount`, `RecurringIncompleteClause`, `ViewDayInvalidDate`, `ViewMonthInvalidFormat`, `EditSingleEventSubject`, `EditSingleEventLocation`, `EditSingleEventDescription`, `EditSingleEventStatus`, `EditSingleEventStartTime`, `EditSingleEventEndTime`, `EditNonExistentEvent`, `EditSeriesFromDate`, `EditEntireSeries`, `ViewDayWithCustomView`, `ViewWeekWithCustomView`, `ViewMonthWithCustomView`, `ViewWeekWithEventsOnMultipleDays`, `ViewMonthWithMultipleEvents`

### Deferred to phase 3 (export and import)

Reason: they check CSV/iCal text or importers.

- ModelTest (53): `ExportToCsv`, `ExportToCsvEmpty`, `ExportToCsvEscapeComma`, `ExportToCsvEscapeQuotes`, `ExportToCsvEscapeNewlines`, `ExportToCsvSorted`, `ExportToIcalProducesCalendar`, `ExportToIcalEmptyOptionalFields`, `ExportToIcalNullOptionalFields`, `ExportToIcalFormatting`, `ExportToIcalProducesCalendar2`, `IcalDateTimeFormat`, `CalendarContainerGetName`, `EscapeIcalNullValue`, `IcalExportOrderDependsOnSort`, `ImportFromCsvBasic`, `ImportFromCsvWithQuotes`, `ImportFromCsvWithLocationAndDescription`, `ImportFromCsvMultipleEvents`, `ImportFromCsvNullContent`, `ImportFromCsvEmptyContent`, `ImportFromCsvOnlyHeader`, `ImportFromCsvNoValidEvents`, `ImportFromCsvSkipsEmptyLines`, `ImportFromCsvWithDuplicateEvents`, `ImportFromIcalBasic`, `ImportFromIcalWithDescriptionAndLocation`, `ImportFromIcalWithStatus`, `ImportFromIcalMultipleEvents`, `ImportFromIcalWithDateOnly`, `ImportFromIcalWithTimezone`, `ImportFromIcalWithEscapedCharacters`, `ImportFromIcalNullContent`, `ImportFromIcalEmptyContent`, `ImportFromIcalNoValidEvents`, `ImportFromIcalSkipsInvalidEvents`, `ImportFromIcalWithInvalidStatus`, `ImportFromIcalSkipsDuplicates`, `ImportFromIcalWithWindowsLineEndings`, `ImportFromCsvWithEscapedQuotes`, `ImportFromCsvWithCommasInQuotedField`, `ImportFromCsvWithStatusField`, `ImportFromCsvWithInvalidStatus`, `ImportFromIcalExtractValueWithNoColon`, `ImportFromIcalExtractValueWithMultipleColons`, `ImportFromIcalParseDateTimeWithShortFormat`, `ImportFromIcalUnescapeAllSequences`, `ImportFromIcalWithNullSummary`, `ImportFromCsvWithEmptyOptionalFields`, `ImportFromCsvWithUnquotedFields`, `ImportFromCsvWithWhitespaceAroundFields`, `ImportFromIcalWithStartEndVariations`, `ImportFromIcalWithEmptyLines`
- CalendarTest (3): `ExportCalendar`, `ExportEmptyFilename`, `CsvExportComplexData`

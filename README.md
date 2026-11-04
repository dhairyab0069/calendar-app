# Calendar

[![CI](https://github.com/dhairyab0069/calendar-app/actions/workflows/ci.yml/badge.svg)](https://github.com/dhairyab0069/calendar-app/actions/workflows/ci.yml)
![Java 11+](https://img.shields.io/badge/Java-11%2B-blue)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-green)

A desktop calendar for people who live in more than one timezone. Keep as many calendars
as you like, each in its own timezone, add one-off and recurring events, copy them between
calendars, and import or export CSV and iCal. Use it from a graphical interface, an
interactive prompt, or a script.

<p align="center">
  <img src="archive/java/GUI%20Main.jpeg" width="760" alt="The Calendar app showing the month view with a calendar list on the left">
</p>

## Contents

- [Quick start](#quick-start)
- [Features](#features)
- [Using the app](#using-the-app)
- [Command reference](#command-reference)
- [Import and export](#import-and-export)
- [Development](#development)
- [How it is built](#how-it-is-built)
- [Roadmap: the Rust port](#roadmap-the-rust-port)
- [Known issues](#known-issues)
- [Contributing](#contributing)
- [Credits and license](#credits-and-license)

## Quick start

You need JDK 11 or newer.

```bash
git clone https://github.com/dhairyab0069/calendar-app.git
cd calendar-app/archive/java
./gradlew jar
java -jar build/libs/calendar-1.0.jar
```

That opens the app with a calendar called "Personal" in your system timezone, so you can add
an event straight away. To run it another way:

| Mode | Command |
|---|---|
| Graphical interface | `java -jar build/libs/calendar-1.0.jar` |
| Interactive prompt | `java -jar build/libs/calendar-1.0.jar --mode interactive` |
| Script file | `java -jar build/libs/calendar-1.0.jar --mode headless commands.txt` |

Any other arguments print an error and quit. The sample scripts
[`commands.txt`](archive/java/commands.txt) and [`invalid.txt`](archive/java/invalid.txt)
show a valid run and a run with invalid commands.

## Features

- **Multiple calendars, each with its own timezone.** Events keep their meaning when you
  move between zones, and copying an event to a calendar in another zone converts it.
- **Single, all-day and recurring events.** Pick the weekdays, then repeat a number of times or
  until a date. Each event has a subject, start, end, description, location, and a public or
  private status.
- **Edit one, some or all.** Change a single event, this event and every later one, or the whole
  series.
- **Copy events** one at a time, everything on a day, or a range of days, into another
  calendar.
- **Day, week and month views,** with a Today button and arrows to move through time.
- **Import and export** CSV and iCal (`.ics`) files, with keyboard shortcuts.
- **Availability checks** and event listings from the prompt or a script.
- **Friendly errors** for bad commands, clashing events, invalid dates and unreadable files.

| Create a calendar | Edit a calendar |
|---|---|
| <img src="archive/java/GUI%20Create%20Calendar.png" width="380" alt="The Create Calendar dialog with a name field and a timezone picker"> | <img src="archive/java/GUI%20Edit%20Calendar.png" width="380" alt="The Edit Calendar dialog"> |

<p align="center">
  <img src="archive/java/GUI%20Create%20Event.png" width="560" alt="The Create Event dialog, with recurring options">
</p>

## Using the app

**Everyday tasks in the GUI**

- *Create a calendar:* click **+ Create Calendar**, give it a name and pick a timezone.
- *Switch calendars:* click one in the **My Calendars** list; everything you do applies to it.
- *Add an event:* click **+ New Event** or double-click a day. Fill in the subject and times,
  and tick **Recurring** to choose weekdays and a count or an end date.
- *Edit an event:* open it, then choose **This event only**, **This and future events** or
  **All events in series**.
- *Change the view:* use the **View** menu for Day, Week or Month, and the arrows or **Today** to
  navigate.

**Keyboard shortcuts** (Cmd on macOS, Ctrl elsewhere)

| Shortcut | Action |
|---|---|
| Cmd/Ctrl + I | Import from CSV |
| Cmd/Ctrl + Shift + I | Import from iCal |
| Cmd/Ctrl + E | Export to CSV |
| Cmd/Ctrl + Shift + E | Export to iCal |
| Cmd/Ctrl + Q | Quit |

[`archive/java/USEME.md`](archive/java/USEME.md) has the full walkthrough.

## Command reference

The prompt and script modes share one command language. Dates are `YYYY-MM-DD`, times are
`HH:mm`, and date-times are `YYYY-MM-DDTHH:mm`. Weekdays are `M T W R F S U`
(R is Thursday, S Saturday, U Sunday). Event properties are `subject`, `start`, `end`,
`description`, `location` and `status`.

| Task | Command |
|---|---|
| Create a calendar | `create calendar --name "Work" --timezone America/New_York` |
| Rename a calendar | `edit calendar --name Work --property name "Work 2025"` |
| Change its timezone | `edit calendar --name "Work 2025" --property timezone Europe/London` |
| Switch calendar | `use calendar --name "Work 2025"` |
| Create an event | `create event "Team Meeting" from 2025-01-15T10:00 to 2025-01-15T11:00` |
| Create an all-day event | `create event "Company Holiday" on 2025-01-20` |
| Repeat a number of times | `create event "Standup" from 2025-01-06T09:00 to 2025-01-06T09:30 repeats MWF for 4 times` |
| Repeat until a date | `create event "Review" from 2025-01-01T15:00 to 2025-01-01T16:00 repeats M until 2025-06-01` |
| Edit one event | `edit event subject "Team Meeting" from 2025-01-15T10:00 with "Updated Team Meeting"` |
| Edit this and later events | `edit events location "Standup" from 2025-01-06T09:00 with "Room B"` |
| Edit a whole series | `edit series description "Review" from 2025-01-06T15:00 with "Monthly team review"` |
| Copy one event | `copy event "Review" on 2025-01-15T10:00 --target "Work 2025" to 2025-01-17T09:00` |
| Copy a day | `copy events on 2025-02-01 --target "Work 2025" to 2025-02-05` |
| Copy a range | `copy events between 2025-03-01 and 2025-03-31 --target "Work 2025" to 2025-08-01` |
| List a day | `print events on 2025-01-15` |
| List a range | `print events from 2025-01-15T08:00 to 2025-01-15T17:00` |
| Check availability | `show status on 2025-01-15T10:30` |
| Export | `export cal calendar_export.csv` or `export "Work 2025" calendar_export.ics` |
| Quit | `exit` |

In a script file, put one command per line and end with `exit`. Invalid commands are reported
with an error message.

## Import and export

- **CSV:** columns `Subject, Start, End, Description, Location, Status`, with ISO date-times.
- **iCal:** standard `.ics` files, as exported by most calendar apps.
- Import into the selected calendar from **File > Import Calendar**; the app reports how many
  events came in. Export from **File > Export Calendar**.
- File paths are platform independent, so it behaves the same on Windows, macOS and Linux.

## Development

```bash
cd archive/java
./gradlew test               # unit tests (839 of them)
./gradlew jacocoTestReport   # coverage report in build/reports/jacoco
./gradlew checkstyleMain     # code style
./gradlew pitest             # mutation testing
```

GitHub Actions runs the Java tests and the Rust checks on every push and pull request. The
Java build is configured to keep going when a test fails, so CI reads the test reports and
fails the job itself.

## How it is built

The app follows model-view-controller, and the Rust port keeps the same split.

| Layer | Java package | Rust module | Role |
|---|---|---|---|
| Model | `calendar.model` | `model` | Calendars, events, series, timezones, import and export. Knows nothing about the other layers. |
| View | `calendar.view` | `view` | Console views and the Swing GUI. Shows data; never changes the model. |
| Controller | `calendar.controller` | `controller` | Parses commands, runs them against the model, and drives the GUI, prompt and script modes. |

- Commands are parsed into typed parameter objects and run through a command registry, so a new
  command does not touch the parser or the views.
- The GUI controller is split into small handlers (calendars, events, views) that react to
  property-change events from the view.
- An event is identified by subject, start and end; adding the same one twice is an error.
- Recurring events are expanded into ordinary events that share a series id.
- [`archive/java/Misc.md`](archive/java/Misc.md) lists the design changes made along the way.

```
archive/java/      the Java app: src, Gradle build, docs, screenshots, sample scripts, Javadoc
rust/              the Rust port: Cargo crate and PLAN.md
.github/workflows/ CI for the Rust crate and the Java app
```

## Roadmap: the Rust port

The goal is a native version with the same MVC structure.

| Phase | Scope | Status |
|---|---|---|
| 1 | Model layer, with the Java model tests ported (146 tests) | Done |
| 2 | Command parser and script runner | Next |
| 3 | CSV and iCal import and export | Planned |
| 4 | Interactive prompt | Planned |
| 5 | GUI, probably egui | Planned |

[`rust/PLAN.md`](rust/PLAN.md) has the decisions, the Java-to-Rust mapping, the test-porting
table and the open questions. To work on it:

```bash
cd rust
cargo test
cargo clippy --all-targets -- -D warnings
```

## Known issues

These are in the Java app and were found while planning the port:

- CSV import reads the description and location columns in the opposite order from export, so
  a round trip swaps them.
- Events are indexed by their start date only, so a multi-day event does not show on its later
  days.

[`rust/PLAN.md`](rust/PLAN.md) lists more, as open questions, with how the Rust version will
handle each.

## Contributing

Issues and pull requests are welcome. For a larger change, please open an issue first. Run the
tests before sending a pull request: `./gradlew test` in `archive/java` for the Java app, and
`cargo test`, `cargo clippy --all-targets -- -D warnings` and `cargo fmt --check` in `rust`
for the port.

## Credits and license

Built by Dhairya Bhatia and Krish Nagaral ([@Blanq-one](https://github.com/Blanq-one)) for
CS5010 (Program Design Paradigms) at Northeastern University. Released under the Apache
License 2.0; see [`LICENSE`](LICENSE).

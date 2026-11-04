# Calendar

[![CI](https://github.com/dhairyab0069/calendar-app/actions/workflows/ci.yml/badge.svg)](https://github.com/dhairyab0069/calendar-app/actions/workflows/ci.yml)

A scheduling app with several calendars, each in its own timezone. Use it from a
Swing GUI, an interactive text prompt, or a script. Written in Java for Northeastern's
CS5010 (Program Design Paradigms) and now being ported to Rust.

<p align="center">
  <img src="archive/java/GUI%20Main.jpeg" width="720" alt="The month view with events on several days">
</p>

**Where things stand:** the Java app is complete and works. It is archived in
[`archive/java`](archive/java) and still builds and passes its tests. The Rust port,
in [`rust`](rust), has finished phase 1 of 5 (the model); it is not runnable yet. See
[the plan](rust/PLAN.md).

## Contents

- [Features](#features)
- [Run the Java app](#run-the-java-app)
- [Commands](#commands)
- [Design](#design)
- [The Rust port](#the-rust-port)
- [Known issues](#known-issues)
- [Repository layout](#repository-layout)
- [Credits and license](#credits-and-license)

## Features

- **Several calendars,** each with its own timezone. The GUI starts with a default
  calendar in your system timezone, so you can add events straight away.
- **Events:** single, all-day, and recurring (pick weekdays, then a number of
  occurrences or an end date). Each has a subject, start, end, description,
  location, and public or private status.
- **Editing:** change one event, this event and the ones after it, or a whole series.
- **Copying** one event, the events on a day, or a range of days into another
  calendar, converting between timezones.
- **Views:** day, week and month in the GUI; the text modes can print a day, a
  range, and a busy or available check.
- **Import and export:** CSV and iCal (`.ics`), with keyboard shortcuts in the GUI.
- **Three ways to run it:** GUI, interactive text, or a script file (headless).

| Create an event | Edit a calendar |
|---|---|
| <img src="archive/java/GUI%20Create%20Event.png" width="360" alt="The create event dialog"> | <img src="archive/java/GUI%20Edit%20Calendar.png" width="360" alt="The edit calendar dialog"> |

## Run the Java app

You need JDK 11 or newer. From the repository root:

```bash
cd archive/java
./gradlew jar

java -jar build/libs/calendar-1.0.jar                                  # GUI
java -jar build/libs/calendar-1.0.jar --mode interactive               # type commands
java -jar build/libs/calendar-1.0.jar --mode headless commands.txt     # run a script
```

Any other arguments print an error and quit. `./gradlew test` runs the tests, and
[`archive/java/USEME.md`](archive/java/USEME.md) walks through the GUI.

## Commands

The text modes and scripts share one command language:

```
create calendar --name "Work" --timezone America/New_York
create calendar --name "Personal" --timezone Europe/London
use calendar --name "Work"
create event "Team Meeting" from 2025-01-15T10:00 to 2025-01-15T11:00
create event "Company Holiday" on 2025-01-20
create event "Weekly Standup" from 2025-01-06T09:00 to 2025-01-06T09:30 repeats MWF for 4 times
copy events on 2025-02-01 --target "Personal" to 2025-02-05
export cal calendar_export.csv
```

Weekdays are `M T W R F S U`. [`commands.txt`](archive/java/commands.txt) is a valid
script and [`invalid.txt`](archive/java/invalid.txt) contains invalid commands that
should be rejected with a message. The full list is in
[`USEME.md`](archive/java/USEME.md).

## Design

The app follows model-view-controller, and the Rust port keeps the same split.

| Layer | Java package | Rust module | Role |
|---|---|---|---|
| Model | `calendar.model` | `model` | Calendars, events, series, timezones, import and export. Knows nothing about the other layers. |
| View | `calendar.view` | `view` | Console views and the Swing GUI. Shows data; never changes the model. |
| Controller | `calendar.controller` | `controller` | Parses commands, runs them against the model, and drives the headless, interactive and GUI modes. |

A few choices worth knowing about:

- Commands are parsed into typed parameter objects, then run through a command
  registry, so adding a command does not touch the parser or the views.
- The GUI controller is split into small handlers (calendars, events, views) that
  react to property-change events from the view.
- An event is identified by subject, start and end; adding the same triple twice is
  an error.
- Recurring events are expanded into ordinary events that share a series id.

[`archive/java/Misc.md`](archive/java/Misc.md) lists the design changes made during
the project and which features work. The Java side has 839 tests.

## The Rust port

The aim is to run natively and to keep the MVC structure, so the two versions can be
compared module by module.

| Phase | Scope | Status |
|---|---|---|
| 1 | Model layer, with the Java model tests ported (146 tests) | Done |
| 2 | Command parser and headless runner | Next |
| 3 | CSV and iCal import and export | Planned |
| 4 | Interactive text mode | Planned |
| 5 | GUI, probably egui | Planned |

[`rust/PLAN.md`](rust/PLAN.md) has the decisions, the mapping from each Java class to
a Rust module, the test-porting table, and open questions. To work on it:

```bash
cd rust
cargo test
cargo clippy --all-targets -- -D warnings
```

## Known issues

These are in the archived Java app and were found while planning the port:

- CSV import reads the description and location columns in the opposite order from
  export, so a round trip swaps them.
- Events are indexed by their start date only, so a multi-day event does not show
  on its later days.

[`rust/PLAN.md`](rust/PLAN.md) lists more, as open questions, along with how the
Rust version will handle each.

## Repository layout

```
archive/java/      the complete Java app: src, Gradle build, docs, screenshots,
                   sample scripts and the generated Javadoc (archive/java/javadoc)
rust/              the Rust port: Cargo crate and PLAN.md
.github/workflows/ CI for the Rust crate and the archived Java app
LICENSE
```

## Credits and license

Built by Dhairya Bhatia and Krish Nagaral ([@Blanq-one](https://github.com/Blanq-one))
for CS5010 at Northeastern University. Released under the Apache License 2.0; see
[`LICENSE`](LICENSE).

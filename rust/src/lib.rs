//! Model layer of the calendar app (phase 1 of the Rust port).
//!
//! * [`Event`], [`EventChanges`], [`Recurrence`]: the value types.
//! * [`Calendar`]: one calendar's events, duplicates and recurring series.
//! * [`Calendars`]: several calendars with timezones, an active one, and the
//!   copy operations that span two of them.
//!
//! Parsing, export/import, views and the CLI come in later phases.

mod calendar;
mod calendars;
mod error;
mod event;
mod status;
mod weekday;
pub mod zone;

pub use calendar::Calendar;
pub use calendars::Calendars;
pub use chrono_tz::Tz;
pub use error::{CalendarError, Result};
pub use event::{Event, EventChanges, EventKey, Recurrence, RecurrenceEnd, SeriesId};
pub use status::EventStatus;
pub use weekday::Weekday;

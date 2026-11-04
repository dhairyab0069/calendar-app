//! The model: everything the calendar knows, with no knowledge of how it is shown
//! or driven.
//!
//! * [`Event`], [`EventChanges`], [`Recurrence`]: the value types.
//! * [`Calendar`]: one calendar's events, duplicates and recurring series.
//! * [`Calendars`]: several calendars with timezones, an active one, and the
//!   copy operations that span two of them.
//!
//! Import and export of CSV and iCal will live here too (phase 3), as they did in
//! the Java model. Nothing in this module may depend on `controller` or `view`.

mod calendar;
mod calendars;
mod error;
mod event;
mod status;
mod weekday;
pub mod zone;

pub use calendar::Calendar;
pub use calendars::Calendars;
pub use error::{CalendarError, Result};
pub use event::{Event, EventChanges, EventKey, Recurrence, RecurrenceEnd, SeriesId};
pub use status::EventStatus;
pub use weekday::Weekday;

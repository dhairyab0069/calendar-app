//! A multi-calendar scheduling app, ported from Java to Rust and kept in the same
//! model-view-controller shape as the original.
//!
//! * [`model`]: calendars, events, recurring series, timezones (phase 1, done).
//! * [`controller`]: command parsing and execution (phase 2).
//! * [`view`]: console rendering, later a GUI (phases 2 and 5).
//!
//! Dependencies point one way: the controller uses the model and the view
//! interface, the view reads the model, and the model knows about neither.

pub mod controller;
pub mod model;
pub mod view;

pub use chrono_tz::Tz;
pub use model::zone;
pub use model::{
    Calendar, CalendarError, Calendars, Event, EventChanges, EventKey, EventStatus, Recurrence,
    RecurrenceEnd, Result, SeriesId, Weekday,
};

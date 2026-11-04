//! One error type for the whole model.
//!
//! The Java app used five checked exceptions (`DuplicateEventException`,
//! `EventNotFoundException`, ...) plus unchecked `IllegalArgumentException` and
//! `IllegalStateException` for bad input and "no calendar selected". Rust has no
//! checked exceptions, so every fallible operation returns
//! `Result<_, CalendarError>` and each exception becomes one variant. The
//! messages are copied from the Java ones so later phases can print the same
//! text the old `commands.txt` / `invalid.txt` runs produced.
//!
//! The `Display` impl is written by hand: it is seven lines, and it keeps the
//! crate free of a proc-macro dependency while the error set is this small.

use std::fmt;

/// Everything that can go wrong in the model layer.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CalendarError {
    /// Java `IllegalArgumentException`: bad subject, end before start, ...
    InvalidInput(String),
    /// Java `IllegalStateException("No calendar is currently in use")`.
    NoActiveCalendar,
    /// Java `IllegalStateException` from the 10 000-day recurrence safety limit.
    TooManyOccurrences,
    /// Java `DuplicateEventException`: same (subject, start, end) already exists.
    DuplicateEvent(String),
    /// Java `DuplicateCalendarException`.
    DuplicateCalendar(String),
    /// Java `EventNotFoundException`.
    EventNotFound(String),
    /// Java `CalendarNotFoundException`.
    CalendarNotFound(String),
}

impl fmt::Display for CalendarError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidInput(msg)
            | Self::DuplicateEvent(msg)
            | Self::DuplicateCalendar(msg)
            | Self::EventNotFound(msg)
            | Self::CalendarNotFound(msg) => f.write_str(msg),
            Self::NoActiveCalendar => f.write_str("No calendar is currently in use"),
            Self::TooManyOccurrences => {
                f.write_str("Too many recurrence iterations (safety limit)")
            }
        }
    }
}

impl std::error::Error for CalendarError {}

/// Shorthand used across the crate.
pub type Result<T> = std::result::Result<T, CalendarError>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display_uses_the_java_messages() {
        assert_eq!(
            CalendarError::NoActiveCalendar.to_string(),
            "No calendar is currently in use"
        );
        assert_eq!(
            CalendarError::EventNotFound("Event not found: X".into()).to_string(),
            "Event not found: X"
        );
    }
}

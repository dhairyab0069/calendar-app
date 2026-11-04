//! Event visibility.

use std::fmt;
use std::str::FromStr;

use super::error::CalendarError;

/// Whether an event is shown to everyone or restricted. `Public` is the
/// default for new events, as in Java.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default)]
pub enum EventStatus {
    #[default]
    Public,
    Private,
}

impl EventStatus {
    /// The lowercase word used in CSV output and the command language.
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Public => "public",
            Self::Private => "private",
        }
    }
}

impl fmt::Display for EventStatus {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}

/// Case-insensitive, whitespace-trimmed, like `EventStatus.fromString`.
impl FromStr for EventStatus {
    type Err = CalendarError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.trim().to_lowercase().as_str() {
            "public" => Ok(Self::Public),
            "private" => Ok(Self::Private),
            _ => Err(CalendarError::InvalidInput(format!(
                "Invalid status: {s}. Must be 'public' or 'private'"
            ))),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn event_status_from_string() {
        assert_eq!(
            "public".parse::<EventStatus>().unwrap(),
            EventStatus::Public
        );
        assert_eq!(
            "private".parse::<EventStatus>().unwrap(),
            EventStatus::Private
        );
    }

    #[test]
    fn event_status_from_string_case_insensitive() {
        assert_eq!(
            "PUBLIC".parse::<EventStatus>().unwrap(),
            EventStatus::Public
        );
        assert_eq!(
            "Private".parse::<EventStatus>().unwrap(),
            EventStatus::Private
        );
    }

    #[test]
    fn event_status_from_string_with_whitespace() {
        assert_eq!(
            "  private  ".parse::<EventStatus>().unwrap(),
            EventStatus::Private
        );
    }

    #[test]
    fn event_status_from_string_invalid() {
        let err = "secret".parse::<EventStatus>().unwrap_err();
        assert_eq!(
            err.to_string(),
            "Invalid status: secret. Must be 'public' or 'private'"
        );
    }

    #[test]
    fn event_status_get_value() {
        assert_eq!(EventStatus::Public.as_str(), "public");
        assert_eq!(EventStatus::Private.as_str(), "private");
    }

    #[test]
    fn event_status_to_string() {
        assert_eq!(EventStatus::Public.to_string(), "public");
        assert_eq!(EventStatus::Private.to_string(), "private");
    }
}

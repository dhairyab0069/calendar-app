//! Recurrence weekdays and their one-letter codes (`MTWRFSU`).

use std::fmt;

use super::error::{CalendarError, Result};

/// A day of the week as used by `repeats MWF ...` in the command language.
///
/// chrono already has `chrono::Weekday`, but it has no notion of the app's
/// letter codes (`R` = Thursday, `U` = Sunday), so this small enum owns the
/// parsing and converts to and from chrono's type at the edges.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Weekday {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday,
}

impl Weekday {
    const ALL: [Weekday; 7] = [
        Weekday::Monday,
        Weekday::Tuesday,
        Weekday::Wednesday,
        Weekday::Thursday,
        Weekday::Friday,
        Weekday::Saturday,
        Weekday::Sunday,
    ];

    /// The single-letter code: M, T, W, R, F, S, U.
    pub fn abbreviation(self) -> char {
        match self {
            Self::Monday => 'M',
            Self::Tuesday => 'T',
            Self::Wednesday => 'W',
            Self::Thursday => 'R',
            Self::Friday => 'F',
            Self::Saturday => 'S',
            Self::Sunday => 'U',
        }
    }

    /// Parses one code letter, case-insensitively.
    pub fn from_char(c: char) -> Result<Self> {
        let upper = c.to_ascii_uppercase();
        Self::ALL
            .into_iter()
            .find(|day| day.abbreviation() == upper)
            .ok_or_else(|| {
                CalendarError::InvalidInput(format!("Invalid weekday abbreviation: {c}"))
            })
    }

    /// Parses a run of codes such as `"MWF"`. The whole string is trimmed, but
    /// inner whitespace is rejected, exactly like the Java version.
    pub fn parse_str(codes: &str) -> Result<Vec<Self>> {
        let trimmed = codes.trim();
        if trimmed.is_empty() {
            return Err(CalendarError::InvalidInput(
                "Weekday string cannot be null or empty".into(),
            ));
        }
        trimmed.chars().map(Self::from_char).collect()
    }

    /// Converts to chrono's weekday, used when matching calendar dates.
    pub fn to_chrono(self) -> chrono::Weekday {
        match self {
            Self::Monday => chrono::Weekday::Mon,
            Self::Tuesday => chrono::Weekday::Tue,
            Self::Wednesday => chrono::Weekday::Wed,
            Self::Thursday => chrono::Weekday::Thu,
            Self::Friday => chrono::Weekday::Fri,
            Self::Saturday => chrono::Weekday::Sat,
            Self::Sunday => chrono::Weekday::Sun,
        }
    }

    /// Converts from chrono's weekday. Total: every chrono weekday has a match.
    pub fn from_chrono(day: chrono::Weekday) -> Self {
        match day {
            chrono::Weekday::Mon => Self::Monday,
            chrono::Weekday::Tue => Self::Tuesday,
            chrono::Weekday::Wed => Self::Wednesday,
            chrono::Weekday::Thu => Self::Thursday,
            chrono::Weekday::Fri => Self::Friday,
            chrono::Weekday::Sat => Self::Saturday,
            chrono::Weekday::Sun => Self::Sunday,
        }
    }
}

/// Prints the one-letter code, like Java's `Weekday.toString()`.
impl fmt::Display for Weekday {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.abbreviation())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn weekday_from_char() {
        assert_eq!(Weekday::from_char('M').unwrap(), Weekday::Monday);
        assert_eq!(Weekday::from_char('T').unwrap(), Weekday::Tuesday);
        assert_eq!(Weekday::from_char('W').unwrap(), Weekday::Wednesday);
        assert_eq!(Weekday::from_char('R').unwrap(), Weekday::Thursday);
        assert_eq!(Weekday::from_char('F').unwrap(), Weekday::Friday);
        assert_eq!(Weekday::from_char('S').unwrap(), Weekday::Saturday);
        assert_eq!(Weekday::from_char('U').unwrap(), Weekday::Sunday);
    }

    #[test]
    fn weekday_from_char_lowercase() {
        assert_eq!(Weekday::from_char('m').unwrap(), Weekday::Monday);
        assert_eq!(Weekday::from_char('r').unwrap(), Weekday::Thursday);
    }

    #[test]
    fn weekday_from_char_invalid() {
        assert!(matches!(
            Weekday::from_char('X'),
            Err(CalendarError::InvalidInput(_))
        ));
    }

    #[test]
    fn weekday_parse_string() {
        assert_eq!(
            Weekday::parse_str("MWF").unwrap(),
            vec![Weekday::Monday, Weekday::Wednesday, Weekday::Friday]
        );
    }

    #[test]
    fn weekday_parse_string_all_days() {
        assert_eq!(
            Weekday::parse_str("MTWRFSU").unwrap(),
            Weekday::ALL.to_vec()
        );
    }

    #[test]
    fn weekday_parse_string_empty() {
        assert!(Weekday::parse_str("").is_err());
        assert!(Weekday::parse_str("   ").is_err());
    }

    #[test]
    fn weekday_parse_string_rejects_inner_whitespace() {
        assert!(Weekday::parse_str("M W").is_err());
    }

    #[test]
    fn weekday_from_day_of_week() {
        assert_eq!(Weekday::from_chrono(chrono::Weekday::Mon), Weekday::Monday);
        assert_eq!(Weekday::from_chrono(chrono::Weekday::Sun), Weekday::Sunday);
    }

    #[test]
    fn weekday_get_abbreviation() {
        assert_eq!(Weekday::Monday.abbreviation(), 'M');
        assert_eq!(Weekday::Thursday.abbreviation(), 'R');
        assert_eq!(Weekday::Sunday.abbreviation(), 'U');
    }

    #[test]
    fn weekday_get_day_of_week() {
        assert_eq!(Weekday::Monday.to_chrono(), chrono::Weekday::Mon);
        assert_eq!(Weekday::Thursday.to_chrono(), chrono::Weekday::Thu);
        for day in Weekday::ALL {
            assert_eq!(Weekday::from_chrono(day.to_chrono()), day);
        }
    }

    #[test]
    fn weekday_to_string() {
        assert_eq!(Weekday::Monday.to_string(), "M");
        assert_eq!(Weekday::Thursday.to_string(), "R");
    }
}

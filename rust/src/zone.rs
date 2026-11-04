//! Timezone arithmetic with the same edge-case rules as `java.time`.
//!
//! Events store zone-less local times, so every cross-zone operation (change a
//! calendar's timezone, copy between calendars) must turn a local time into an
//! instant and back. Local times are not always valid instants:
//!
//! * *Gap* (spring forward, e.g. 02:30 on 2025-03-09 in New York does not
//!   exist): Java's `atZone` moves the time forward by the length of the gap,
//!   i.e. it uses the offset that was in force just before the gap.
//! * *Overlap* (fall back, 01:30 happens twice): Java picks the earlier of the
//!   two instants (the offset before the transition).
//!
//! chrono reports both cases through `LocalResult` and leaves the policy to the
//! caller, so the policy is written down once, here.

use chrono::{DateTime, Duration, LocalResult, NaiveDateTime, Offset, TimeZone, Utc};
use chrono_tz::Tz;

/// The instant a local wall-clock time denotes in `zone`, resolving gaps and
/// overlaps the way Java does (see module docs).
pub fn to_instant(zone: Tz, local: NaiveDateTime) -> DateTime<Utc> {
    match zone.from_local_datetime(&local) {
        LocalResult::Single(dt) => dt.with_timezone(&Utc),
        LocalResult::Ambiguous(earlier, _) => earlier.with_timezone(&Utc),
        LocalResult::None => {
            // In a gap: reuse the offset that applied a day earlier (gaps are
            // far shorter than a day), which equals "just before the gap".
            let before = zone
                .offset_from_local_datetime(&(local - Duration::days(1)))
                .earliest()
                .map_or(0, |offset| offset.fix().local_minus_utc());
            (local - Duration::seconds(i64::from(before))).and_utc()
        }
    }
}

/// The local wall-clock time of `instant` in `zone`. Always unambiguous.
pub fn to_local(zone: Tz, instant: DateTime<Utc>) -> NaiveDateTime {
    instant.with_timezone(&zone).naive_local()
}

/// Re-expresses a local time of `from` as the local time in `to` at the same
/// instant (Java: `atZone(from).withZoneSameInstant(to).toLocalDateTime()`).
pub fn convert_local(local: NaiveDateTime, from: Tz, to: Tz) -> NaiveDateTime {
    to_local(to, to_instant(from, local))
}

#[cfg(test)]
mod tests {
    use chrono::NaiveDate;

    use super::*;

    fn dt(y: i32, m: u32, d: u32, h: u32, min: u32) -> NaiveDateTime {
        NaiveDate::from_ymd_opt(y, m, d)
            .unwrap()
            .and_hms_opt(h, min, 0)
            .unwrap()
    }

    #[test]
    fn winter_new_york_to_london_is_five_hours() {
        let out = convert_local(
            dt(2025, 1, 15, 10, 0),
            Tz::America__New_York,
            Tz::Europe__London,
        );
        assert_eq!(out, dt(2025, 1, 15, 15, 0));
    }

    #[test]
    fn gap_moves_forward_by_the_gap_length() {
        // 02:30 does not exist in New York on 2025-03-09; Java reads it as
        // 03:30 EDT, which is 07:30 UTC.
        let instant = to_instant(Tz::America__New_York, dt(2025, 3, 9, 2, 30));
        assert_eq!(instant.naive_utc(), dt(2025, 3, 9, 7, 30));
    }

    #[test]
    fn overlap_picks_the_earlier_instant() {
        // 01:30 happens twice on 2025-11-02 in New York; the earlier is EDT
        // (UTC-4), i.e. 05:30 UTC.
        let instant = to_instant(Tz::America__New_York, dt(2025, 11, 2, 1, 30));
        assert_eq!(instant.naive_utc(), dt(2025, 11, 2, 5, 30));
    }
}

package calendar.model;

import java.time.DayOfWeek;

/**
 * Enum representing days of the week with single-character abbreviations.
 * Uses standard abbreviations: M, T, W, R, F, S, U
 * (R for Thursday to avoid conflict with Tuesday)
 *
 * @version 1.0
 */
public enum Weekday {
  MONDAY('M', DayOfWeek.MONDAY),
  TUESDAY('T', DayOfWeek.TUESDAY),
  WEDNESDAY('W', DayOfWeek.WEDNESDAY),
  THURSDAY('R', DayOfWeek.THURSDAY),
  FRIDAY('F', DayOfWeek.FRIDAY),
  SATURDAY('S', DayOfWeek.SATURDAY),
  SUNDAY('U', DayOfWeek.SUNDAY);

  private final char abbreviation;
  private final DayOfWeek dayOfWeek;

  /**
   * Constructor for Weekday enum.
   *
   * @param abbreviation single character abbreviation
   * @param dayOfWeek corresponding Java DayOfWeek
   */
  Weekday(char abbreviation, DayOfWeek dayOfWeek) {
    this.abbreviation = abbreviation;
    this.dayOfWeek = dayOfWeek;
  }

  /**
   * Gets the single-character abbreviation.
   *
   * @return the abbreviation character
   */
  public char getAbbreviation() {
    return abbreviation;
  }

  /**
   * Gets the corresponding Java DayOfWeek.
   *
   * @return the DayOfWeek value
   */
  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  /**
   * Parses a character to a Weekday enum.
   *
   * @param c the character to parse
   * @return the corresponding Weekday
   * @throws IllegalArgumentException if character is invalid
   */
  public static Weekday fromChar(char c) {
    char upper = Character.toUpperCase(c);
    for (Weekday day : values()) {
      if (day.abbreviation == upper) {
        return day;
      }
    }
    throw new IllegalArgumentException("Invalid weekday abbreviation: " + c);
  }

  /**
   * Parses a string of weekday abbreviations.
   * Example: "MWF" -> [MONDAY, WEDNESDAY, FRIDAY]
   *
   * @param abbreviations string of weekday characters
   * @return array of Weekday enums
   * @throws IllegalArgumentException if any character is invalid
   */
  public static Weekday[] parseString(String abbreviations) {
    if (abbreviations == null || abbreviations.trim().isEmpty()) {
      throw new IllegalArgumentException("Weekday string cannot be null or empty");
    }

    String trimmed = abbreviations.trim();
    Weekday[] result = new Weekday[trimmed.length()];

    for (int i = 0; i < trimmed.length(); i++) {
      result[i] = fromChar(trimmed.charAt(i));
    }

    return result;
  }

  /**
   * Converts a DayOfWeek to Weekday.
   *
   * @param dayOfWeek the Java DayOfWeek
   * @return the corresponding Weekday
   */
  public static Weekday fromDayOfWeek(DayOfWeek dayOfWeek) {
    for (Weekday day : values()) {
      if (day.dayOfWeek == dayOfWeek) {
        return day;
      }
    }
    throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
  }

  @Override
  public String toString() {
    return String.valueOf(abbreviation);
  }
}
package calendar.controller.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for parsing date and time strings.
 * Handles conversion between string representations and LocalDateTime/LocalDate objects.
 *
 * <p>Expected formats:
 * <ul>
 *   <li>DateTime: "YYYY-MM-DDThh:mm" (ISO 8601)</li>
 *   <li>Date: "YYYY-MM-DD" (ISO 8601)</li>
 * </ul>
 *
 * @version 1.0
 */
public class DateTimeParser {

  /**
   * Standard ISO 8601 date-time format.
   */
  public static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  /**
   * Standard ISO 8601 date format.
   */
  public static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Private constructor to prevent instantiation.
   */
  private DateTimeParser() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Parses a date-time string to LocalDateTime.
   *
   * @param dateTimeStr the date-time string in format "YYYY-MM-DDThh:mm"
   * @return the parsed LocalDateTime
   * @throws IllegalArgumentException if the string is null, empty, or invalid format
   */
  public static LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Date-time string cannot be null or empty");
    }

    try {
      return LocalDateTime.parse(dateTimeStr.trim(), DATE_TIME_FORMAT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date-time format: " + dateTimeStr
              + ". Expected format: YYYY-MM-DDThh:mm (e.g., 2025-01-15T10:30)", e);
    }
  }

  /**
   * Parses a date string to LocalDate.
   *
   * @param dateStr the date string in format "YYYY-MM-DD"
   * @return the parsed LocalDate
   * @throws IllegalArgumentException if the string is null, empty, or invalid format
   */
  public static LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Date string cannot be null or empty");
    }

    try {
      return LocalDate.parse(dateStr.trim(), DATE_FORMAT);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date format: " + dateStr
              + ". Expected format: YYYY-MM-DD (e.g., 2025-01-15)", e);
    }
  }

  /**
   * Formats a LocalDateTime to string.
   *
   * @param dateTime the LocalDateTime to format
   * @return formatted string in "YYYY-MM-DDThh:mm" format
   * @throws IllegalArgumentException if dateTime is null
   */
  public static String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      throw new IllegalArgumentException("DateTime cannot be null");
    }
    return dateTime.format(DATE_TIME_FORMAT);
  }

  /**
   * Formats a LocalDate to string.
   *
   * @param date the LocalDate to format
   * @return formatted string in "YYYY-MM-DD" format
   * @throws IllegalArgumentException if date is null
   */
  public static String formatDate(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("Date cannot be null");
    }
    return date.format(DATE_FORMAT);
  }

  /**
   * Validates if a string is a valid date-time format.
   *
   * @param dateTimeStr the string to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
      return false;
    }
    try {
      LocalDateTime.parse(dateTimeStr.trim(), DATE_TIME_FORMAT);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  /**
   * Validates if a string is a valid date format.
   *
   * @param dateStr the string to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidDate(String dateStr) {
    if (dateStr == null || dateStr.trim().isEmpty()) {
      return false;
    }
    try {
      LocalDate.parse(dateStr.trim(), DATE_FORMAT);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
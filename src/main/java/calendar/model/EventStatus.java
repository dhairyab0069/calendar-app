package calendar.model;

/**
 * Enum representing the visibility status of a calendar event.
 * PUBLIC events are visible to all, PRIVATE events are restricted.
 *
 * @version 1.0
 */
public enum EventStatus {
  PUBLIC("public"),
  PRIVATE("private");

  private final String value;

  /**
   * Constructor for EventStatus enum.
   *
   * @param value string representation of the status
   */
  EventStatus(String value) {
    this.value = value;
  }

  /**
   * Parses a string to an EventStatus enum.
   * Case-insensitive matching.
   *
   * @param status the string representation
   * @return the corresponding EventStatus
   * @throws IllegalArgumentException if status is null or invalid
   */
  public static EventStatus fromString(String status) {
    if (status == null) {
      throw new IllegalArgumentException("Status cannot be null");
    }

    String normalized = status.trim().toLowerCase();
    for (EventStatus s : values()) {
      if (s.value.equals(normalized)) {
        return s;
      }
    }

    throw new IllegalArgumentException("Invalid status: " + status
        + ". Must be 'public' or 'private'");
  }

  /**
   * Parses a string to an EventStatus enum (alias for fromString).
   * Case-insensitive matching.
   *
   * @param value the string representation
   * @return the corresponding EventStatus
   * @throws IllegalArgumentException if value is null or invalid
   */
  public static EventStatus fromValue(String value) {
    return fromString(value);
  }

  /**
   * Gets the string value of this status.
   *
   * @return the status value
   */
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
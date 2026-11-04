package calendar.exceptions;

/**
 * Thrown when attempting to create or rename a calendar to an existing name.
 */
public class DuplicateCalendarException extends Exception {

  /**
   * Constructs the exception with message.
   *
   * @param message detail message
   */
  public DuplicateCalendarException(String message) {
    super(message);
  }
}


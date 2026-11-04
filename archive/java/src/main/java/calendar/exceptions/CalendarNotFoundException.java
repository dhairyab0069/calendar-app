package calendar.exceptions;

/**
 * Thrown when a requested calendar does not exist.
 */
public class CalendarNotFoundException extends Exception {

  /**
   * Constructs the exception with message.
   *
   * @param message detail message
   */
  public CalendarNotFoundException(String message) {
    super(message);
  }
}


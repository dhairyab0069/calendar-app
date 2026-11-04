package calendar.exceptions;

/**
 * Exception thrown when attempting to add a duplicate event to the calendar.
 *
 * <p>A duplicate is defined as an event with the same subject, start time, and end time
 * as an existing event. The calendar enforces this uniqueness constraint to prevent
 * accidental duplicate entries.
 *
 * <p>Example scenarios:
 * <ul>
 *   <li>Adding an event with identical (subject, start, end) tuple</li>
 *   <li>Editing an event such that it would conflict with an existing event</li>
 *   <li>Creating a recurring series that overlaps with existing events</li>
 * </ul>
 *
 * @version 1.0
 */
public class DuplicateEventException extends Exception {

  /**
   * Creates a new DuplicateEventException with the specified message.
   *
   * @param message a description of the duplicate event conflict
   */
  public DuplicateEventException(String message) {
    super(message);
  }

  /**
   * Creates a new DuplicateEventException with the specified message and cause.
   *
   * @param message a description of the duplicate event conflict
   * @param cause   the underlying cause of this exception
   */
  public DuplicateEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
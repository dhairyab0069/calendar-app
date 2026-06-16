package calendar.exceptions;

/**
 * Exception thrown when attempting to access or modify an event that does not exist.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>Editing an event that cannot be found by subject and start time</li>
 *   <li>Querying for a specific event that doesn't exist</li>
 *   <li>Attempting to edit a series when no events match the series ID</li>
 *   <li>Referencing an event in a command that has been deleted or never existed</li>
 * </ul>
 *
 * <p>Example scenarios:
 * <ul>
 *   <li>"edit Meeting from 2025-01-15T10:00" when no such event exists</li>
 *   <li>"edit series ABC123" when series ID is invalid</li>
 *   <li>Attempting to modify a deleted event</li>
 * </ul>
 *
 * @version 1.0
 */
public class EventNotFoundException extends Exception {

  /**
   * Creates a new EventNotFoundException with the specified message.
   *
   * @param message a description of which event was not found
   */
  public EventNotFoundException(String message) {
    super(message);
  }

  /**
   * Creates a new EventNotFoundException with the specified message and cause.
   *
   * @param message a description of which event was not found
   * @param cause   the underlying cause of this exception
   */
  public EventNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
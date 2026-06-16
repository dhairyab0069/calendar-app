package calendar.exceptions;

/**
 * Exception thrown when a command cannot be parsed or executed due to invalid syntax or format.
 *
 * <p>This exception is thrown in the following scenarios:
 * <ul>
 *   <li>Unknown command type</li>
 *   <li>Missing required parameters</li>
 *   <li>Invalid date/time format</li>
 *   <li>Malformed command syntax</li>
 *   <li>Invalid parameter values (e.g., negative repeat count)</li>
 * </ul>
 *
 * <p>Example invalid commands:
 * <ul>
 *   <li>"create" (missing subject and times)</li>
 *   <li>"create Meeting from 10:00" (missing date and 'to' clause)</li>
 *   <li>"edit Nonexistent Event" (event not found)</li>
 *   <li>"print 2025-13-45" (invalid date)</li>
 * </ul>
 *
 * @version 1.0
 */
public class InvalidCommandException extends Exception {

  /**
   * Creates a new InvalidCommandException with the specified message.
   *
   * @param message a description of why the command is invalid
   */
  public InvalidCommandException(String message) {
    super(message);
  }

  /**
   * Creates a new InvalidCommandException with the specified message and cause.
   *
   * @param message a description of why the command is invalid
   * @param cause   the underlying cause of this exception (e.g., parsing error)
   */
  public InvalidCommandException(String message, Throwable cause) {
    super(message, cause);
  }
}
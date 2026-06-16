package calendar.view;

import calendar.model.CalendarEvent;
import java.util.List;

/**
 * Interface for calendar view operations.
 * Abstracts OUTPUT ONLY - no input handling.
 *
 * <p>The View layer is responsible ONLY for displaying information
 * to the user. It does NOT:
 * <ul>
 *   <li>Read user input (Controller's job)</li>
 *   <li>Parse commands (Controller's job)</li>
 *   <li>Perform business logic (Model's job)</li>
 * </ul>
 *
 * <p>The View simply formats and displays data provided by the Controller.
 *
 * @version 1.0
 */
public interface CalendarView {

  /**
   * Displays a general message to the user.
   *
   * @param message the message to display
   */
  void displayMessage(String message);

  /**
   * Displays an error message to the user.
   * Error messages should be visually distinct (e.g., "Error: " prefix).
   *
   * @param error the error message to display
   */
  void displayError(String error);

  /**
   * Displays a list of events in a formatted manner.
   * If the list is empty, displays an appropriate "no events" message.
   *
   * @param events the list of events to display
   */
  void displayEvents(List<CalendarEvent> events);
}
/**
 * Program runner for the Virtual Calendar Application.
 *
 * <p>This is the entry point that should be placed in src/java (outside the calendar package).
 * It delegates to CalendarApp in the controller package for actual execution.
 *
 * @version 1.0
 */
public class CalendarRunner {

  /**
   * Main method - entry point for the application.
   *
   * @param args command line arguments passed to CalendarApp
   */
  public static void main(String[] args) {
    calendar.controller.CalendarApp.main(args);
  }
}
package calendar.view;

import calendar.model.CalendarEvent;
import java.io.PrintStream;
import java.util.List;

/**
 * Console-based implementation of CalendarView.
 * Handles OUTPUT ONLY - text-based display to terminal.
 *
 * <p>This class does NOT read input - that's the Controller's responsibility.
 * It only formats and prints information to the console.
 *
 * <p>Delegates formatting to specialized view classes (DayView, WeekView, MonthView)
 * for complex displays while handling simple output directly.
 *
 * @version 1.0
 */
public class ConsoleView implements CalendarView {

  private final PrintStream out;
  private final PrintStream err;

  // Formatter delegates
  private final DayView dayView;
  private final WeekView weekView;
  private final MonthView monthView;

  /**
   * Creates a ConsoleView using System.out and System.err.
   */
  public ConsoleView() {
    this(System.out, System.err);
  }

  /**
   * Creates a ConsoleView with custom streams (useful for testing).
   *
   * @param out output stream for normal messages
   * @param err error stream for error messages
   */
  public ConsoleView(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
    this.dayView = new DayView();
    this.weekView = new WeekView();
    this.monthView = new MonthView();
  }

  @Override
  public void displayMessage(String message) {
    if (message != null) {
      out.println(message);
    }
  }

  @Override
  public void displayError(String error) {
    if (error != null) {
      err.println("Error: " + error);
    }
  }

  @Override
  public void displayEvents(List<CalendarEvent> events) {
    if (events == null || events.isEmpty()) {
      out.println("No events found.");
      return;
    }

    out.println("Events:");
    for (CalendarEvent event : events) {
      out.println(formatEventLine(event));
    }
  }

  /**
   * Formats a single event as a line of text.
   */
  private String formatEventLine(CalendarEvent event) {
    StringBuilder sb = new StringBuilder();
    sb.append("  - ").append(event.getSubject());
    sb.append(" (").append(event.getStart());
    sb.append(" to ").append(event.getEnd()).append(")");

    if (event.getLocation() != null && !event.getLocation().isEmpty()) {
      sb.append(" at ").append(event.getLocation());
    }

    if (event.isRecurring()) {
      sb.append(" [Recurring]");
    }

    return sb.toString();
  }

  /**
   * Gets the DayView formatter.
   * Controller can use this to display day-formatted output.
   *
   * @return the day view formatter
   */
  public DayView getDayView() {
    return dayView;
  }

  /**
   * Gets the WeekView formatter.
   * Controller can use this to display week-formatted output.
   *
   * @return the week view formatter
   */
  public WeekView getWeekView() {
    return weekView;
  }

  /**
   * Gets the MonthView formatter.
   * Controller can use this to display month-formatted output.
   *
   * @return the month view formatter
   */
  public MonthView getMonthView() {
    return monthView;
  }
}
package calendar.view;

import calendar.model.CalendarEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Formatter for displaying events in a single-day view.
 *
 * <p>Provides a chronological listing of all events on a specific date,
 * with time details and event information.
 *
 * <p>Follows Single Responsibility Principle - only formats day displays,
 * performs no business logic or I/O operations.
 *
 * @version 1.0
 */
public class DayView {

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("h:mm a");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

  /**
   * Formats events for a specific day.
   *
   * @param date the date to display
   * @param events list of events on this date
   * @return formatted string representation of the day
   */
  public String formatDay(LocalDate date, List<CalendarEvent> events) {
    StringBuilder sb = new StringBuilder();

    // Header
    sb.append("=".repeat(50)).append("\n");
    sb.append(date.format(DATE_FORMAT)).append("\n");
    sb.append("=".repeat(50)).append("\n");

    if (events == null || events.isEmpty()) {
      sb.append("No events scheduled.\n");
    } else {
      // Sort events by start time
      events.sort((e1, e2) -> e1.getStart().compareTo(e2.getStart()));

      for (CalendarEvent event : events) {
        sb.append(formatEvent(event)).append("\n");
      }

      sb.append("\nTotal events: ").append(events.size()).append("\n");
    }

    sb.append("=".repeat(50)).append("\n");
    return sb.toString();
  }

  /**
   * Formats a single event with time and details.
   */
  private String formatEvent(CalendarEvent event) {
    StringBuilder sb = new StringBuilder();

    // Time range
    sb.append(event.getStart().format(TIME_FORMAT))
        .append(" - ")
        .append(event.getEnd().format(TIME_FORMAT));

    // Subject
    sb.append(" | ").append(event.getSubject());

    // Location (if present)
    if (event.getLocation() != null && !event.getLocation().isEmpty()) {
      sb.append(" @ ").append(event.getLocation());
    }

    // Status indicator
    if (event.getStatus() != null) {
      sb.append(" [").append(event.getStatus()).append("]");
    }

    // Recurring indicator
    if (event.isRecurring()) {
      sb.append(" (recurring)");
    }

    // Description on next line if present
    if (event.getDescription() != null && !event.getDescription().isEmpty()) {
      sb.append("\n    ").append(event.getDescription());
    }

    return sb.toString();
  }

  /**
   * Creates a compact single-line representation of an event.
   *
   * @param event the event to format
   * @return compact event string
   */
  public String formatEventCompact(CalendarEvent event) {
    return String.format("%s - %s: %s",
        event.getStart().format(TIME_FORMAT),
        event.getEnd().format(TIME_FORMAT),
        event.getSubject());
  }
}
package calendar.view;

import calendar.model.CalendarEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Formatter for displaying events in a weekly view.
 *
 * <p>Groups events by day of the week, providing a 7-day overview
 * with event counts and summaries for each day.
 *
 * <p>Follows Single Responsibility Principle - only formats week displays,
 * performs no business logic or I/O operations.
 *
 * @version 1.0
 */
public class WeekView {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d");
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("h:mm a");

  /**
   * Formats a week view starting from a specific date.
   *
   * @param weekStart the first day of the week (typically Sunday or Monday)
   * @param eventsByDay map of date to events for that day
   * @return formatted string representation of the week
   */
  public String formatWeek(LocalDate weekStart, Map<LocalDate, List<CalendarEvent>> eventsByDay) {
    StringBuilder sb = new StringBuilder();

    // Header
    sb.append("=".repeat(60)).append("\n");
    sb.append("Week of ").append(weekStart.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
        .append("\n");
    sb.append("=".repeat(60)).append("\n\n");

    // Display each day of the week
    for (int i = 0; i < 7; i++) {
      LocalDate date = weekStart.plusDays(i);
      List<CalendarEvent> events = eventsByDay.get(date);

      sb.append(formatDay(date, events)).append("\n");
    }

    sb.append("=".repeat(60)).append("\n");
    return sb.toString();
  }

  /**
   * Formats a single day within the week view.
   */
  private String formatDay(LocalDate date, List<CalendarEvent> events) {
    StringBuilder sb = new StringBuilder();

    // Day header
    String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    sb.append(String.format("%-10s %s", dayName + ",", date.format(DATE_FORMAT)));

    if (events == null || events.isEmpty()) {
      sb.append("  (No events)");
    } else {
      sb.append("  (").append(events.size()).append(" event");
      if (events.size() > 1) {
        sb.append("s");
      }
      sb.append(")");

      // List events
      for (CalendarEvent event : events) {
        sb.append("\n  • ").append(formatEventSummary(event));
      }
    }

    return sb.toString();
  }

  /**
   * Formats a brief event summary for week view.
   */
  private String formatEventSummary(CalendarEvent event) {
    StringBuilder sb = new StringBuilder();

    sb.append(event.getStart().format(TIME_FORMAT))
        .append(" - ")
        .append(event.getSubject());

    if (event.getLocation() != null && !event.getLocation().isEmpty()) {
      sb.append(" @ ").append(event.getLocation());
    }

    return sb.toString();
  }

  /**
   * Creates a compact week summary showing only event counts per day.
   *
   * @param weekStart the first day of the week
   * @param eventsByDay map of date to events
   * @return compact summary string
   */
  public String formatWeekSummary(LocalDate weekStart,
                                  Map<LocalDate, List<CalendarEvent>> eventsByDay) {
    StringBuilder sb = new StringBuilder();

    sb.append("Week of ").append(weekStart.format(DATE_FORMAT)).append(": ");

    int totalEvents = 0;
    for (int i = 0; i < 7; i++) {
      LocalDate date = weekStart.plusDays(i);
      List<CalendarEvent> events = eventsByDay.get(date);
      int count = events != null ? events.size() : 0;
      totalEvents += count;
    }

    sb.append(totalEvents).append(" total event");
    if (totalEvents != 1) {
      sb.append("s");
    }

    return sb.toString();
  }
}
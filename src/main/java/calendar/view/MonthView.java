package calendar.view;

import calendar.model.CalendarEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Formatter for displaying events in a monthly calendar view.
 *
 * <p>Provides ASCII-art calendar grid with event indicators for busy days.
 * Shows monthly overview with counts or markers for days containing events.
 *
 * <p>Follows Single Responsibility Principle - only formats month displays,
 * performs no business logic or I/O operations.
 *
 * @version 1.0
 */
public class MonthView {

  /**
   * Formats a month calendar with event indicators.
   * Days with events are marked with a dash (-).
   *
   * @param yearMonth the month to display
   * @param daysWithEvents set of dates that have events
   * @return formatted calendar grid string
   */
  public String formatMonth(YearMonth yearMonth, Set<LocalDate> daysWithEvents) {
    StringBuilder sb = new StringBuilder();

    // Header
    String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int year = yearMonth.getYear();

    sb.append("=".repeat(40)).append("\n");
    sb.append(String.format("      %s %d\n", monthName, year));
    sb.append("=".repeat(40)).append("\n");

    // Day headers
    sb.append(" Su  Mo  Tu  We  Th  Fr  Sa\n");
    sb.append("-".repeat(40)).append("\n");

    // Calendar grid
    LocalDate firstDay = yearMonth.atDay(1);
    LocalDate lastDay = yearMonth.atEndOfMonth();

    // Start from the first Sunday (or the first day if it's Sunday)
    DayOfWeek firstDayOfWeek = firstDay.getDayOfWeek();
    int daysToSubtract = firstDayOfWeek == DayOfWeek.SUNDAY ? 0 :
        firstDayOfWeek.getValue() % 7;
    LocalDate calendarStart = firstDay.minusDays(daysToSubtract);

    LocalDate currentDate = calendarStart;
    int position = 0;

    while (currentDate.isBefore(lastDay.plusDays(1)) || position % 7 != 0) {
      if (position > 0 && position % 7 == 0) {
        sb.append("\n");
      }

      if (currentDate.getMonth() == yearMonth.getMonth()) {
        // Current month - show day number
        int dayNum = currentDate.getDayOfMonth();
        boolean hasEvents = daysWithEvents != null && daysWithEvents.contains(currentDate);

        if (hasEvents) {
          sb.append(String.format("%3d-", dayNum));
        } else {
          sb.append(String.format("%3d ", dayNum));
        }
      } else {
        // Previous or next month - show empty space
        sb.append("    ");
      }

      currentDate = currentDate.plusDays(1);
      position++;
    }

    sb.append("\n");
    sb.append("=".repeat(40)).append("\n");
    sb.append("Legend: XX- indicates day with events\n");
    sb.append("=".repeat(40)).append("\n");

    return sb.toString();
  }

  /**
   * Formats a month calendar with event counts per day.
   *
   * @param yearMonth the month to display
   * @param eventCountsByDay map of date to event count
   * @return formatted calendar grid with counts
   */
  public String formatMonthWithCounts(YearMonth yearMonth,
                                      Map<LocalDate, Integer> eventCountsByDay) {
    StringBuilder sb = new StringBuilder();

    // Header
    String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int year = yearMonth.getYear();

    sb.append("=".repeat(50)).append("\n");
    sb.append(String.format("      %s %d\n", monthName, year));
    sb.append("=".repeat(50)).append("\n");

    // Day headers
    sb.append("  Su   Mo   Tu   We   Th   Fr   Sa\n");
    sb.append("-".repeat(50)).append("\n");

    // Calendar grid
    LocalDate firstDay = yearMonth.atDay(1);
    LocalDate lastDay = yearMonth.atEndOfMonth();

    DayOfWeek firstDayOfWeek = firstDay.getDayOfWeek();
    int daysToSubtract = firstDayOfWeek == DayOfWeek.SUNDAY ? 0 :
        firstDayOfWeek.getValue() % 7;

    LocalDate currentDate = firstDay.minusDays(daysToSubtract);
    int position = 0;

    while (currentDate.isBefore(lastDay.plusDays(1)) || position % 7 != 0) {
      if (position > 0 && position % 7 == 0) {
        sb.append("\n");
      }

      if (currentDate.getMonth() == yearMonth.getMonth()) {
        int dayNum = currentDate.getDayOfMonth();
        Integer count = eventCountsByDay != null ? eventCountsByDay.get(currentDate) : null;

        if (count != null && count > 0) {
          if (count > 9) {
            sb.append(String.format("%2d(+)", dayNum));
          } else {
            sb.append(String.format("%2d(%d)", dayNum, count));
          }
        } else {
          sb.append(String.format("%4d ", dayNum));
        }
      } else {
        sb.append("     ");
      }

      currentDate = currentDate.plusDays(1);
      position++;
    }

    sb.append("\n");
    sb.append("=".repeat(50)).append("\n");
    sb.append("Legend: XX(N) shows N events on day XX, XX(+) shows 10+ events\n");
    sb.append("=".repeat(50)).append("\n");

    return sb.toString();
  }

  /**
   * Creates a summary line for the month.
   *
   * @param yearMonth the month
   * @param totalEvents total number of events
   * @param daysWithEvents number of days that have events
   * @return summary string
   */
  public String formatMonthSummary(YearMonth yearMonth, int totalEvents, int daysWithEvents) {
    String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    return String.format("%s %d: %d events across %d days",
        monthName, yearMonth.getYear(), totalEvents, daysWithEvents);
  }
}
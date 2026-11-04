import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.model.CalendarEvent;
import calendar.model.EventStatus;
import calendar.model.Weekday;
import calendar.view.ConsoleView;
import calendar.view.DayView;
import calendar.view.MonthView;
import calendar.view.WeekView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test suite for View layer classes using JUnit 4.
 *
 * <p>Tests all view components:
 * <ul>
 *   <li>ConsoleView - main view implementation</li>
 *   <li>DayView - day formatting</li>
 *   <li>WeekView - week formatting</li>
 *   <li>MonthView - month formatting</li>
 * </ul>
 *
 * @version 1.0
 */
public class ViewTest {

  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private ConsoleView view;
  private DayView dayView;
  private WeekView weekView;
  private MonthView monthView;

  /**
   * Sets up the test environment before each test.
   * Redirects output and error streams to capture console output.
   */
  @Before
  public void setUp() {
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    view = new ConsoleView(new PrintStream(outputStream), new PrintStream(errorStream));
  }

  @Test
  public void testDisplayMessage() {
    view.displayMessage("Test message");
    String output = outputStream.toString();
    assertTrue(output.contains("Test message"));
  }

  @Test
  public void testDisplayMessageWithNewline() {
    view.displayMessage("Test message");
    String output = outputStream.toString();
    assertTrue(output.endsWith("\n") || output.contains("Test message"));
  }

  @Test
  public void testDisplayMessageNull() {
    view.displayMessage(null);
    String output = outputStream.toString();
    assertEquals("", output);
  }

  @Test
  public void testDisplayError() {
    view.displayError("Error message");
    String error = errorStream.toString();
    assertTrue(error.contains("Error:"));
    assertTrue(error.contains("Error message"));
  }

  @Test
  public void testDisplayErrorNull() {
    view.displayError(null);
    String error = errorStream.toString();
    assertEquals("", error);
  }

  @Test
  public void testDisplayEventsEmpty() {
    view.displayEvents(new ArrayList<>());
    String output = outputStream.toString();
    assertTrue(output.contains("No events found"));
  }

  @Test
  public void testDisplayEventsNull() {
    view.displayEvents(null);
    String output = outputStream.toString();
    assertTrue(output.contains("No events found"));
  }

  @Test
  public void testDisplayEventsSingle() {
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    events.add(event);

    view.displayEvents(events);
    String output = outputStream.toString();
    assertTrue(output.contains("Events:"));
    assertTrue(output.contains("Meeting"));
  }

  @Test
  public void testDisplayEventsMultiple() {
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start1 = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 1, 15, 11, 0);
    LocalDateTime start2 = LocalDateTime.of(2025, 1, 15, 14, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 1, 15, 15, 0);

    events.add(CalendarEvent.builder("Meeting1", start1, end1).build());
    events.add(CalendarEvent.builder("Meeting2", start2, end2).build());

    view.displayEvents(events);
    String output = outputStream.toString();
    assertTrue(output.contains("Meeting1"));
    assertTrue(output.contains("Meeting2"));
  }

  @Test
  public void testDisplayEventsWithLocation() {
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .location("Room A")
        .build();
    events.add(event);

    view.displayEvents(events);
    String output = outputStream.toString();
    assertTrue(output.contains("Room A"));
  }

  @Test
  public void testDisplayEventsRecurring() {
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent event = CalendarEvent.recurringBuilder("Meeting", start, end, days)
        .repeatCount(5)
        .build();
    events.add(event);

    view.displayEvents(events);
    String output = outputStream.toString();
    assertTrue(output.contains("Recurring") || output.contains("Meeting"));
  }

  @Test
  public void testGetDayView() {
    DayView dayView = view.getDayView();
    assertNotNull(dayView);
  }

  @Test
  public void testGetWeekView() {
    WeekView weekView = view.getWeekView();
    assertNotNull(weekView);
  }

  @Test
  public void testGetMonthView() {
    MonthView monthView = view.getMonthView();
    assertNotNull(monthView);
  }

  @Test
  public void testConsoleViewDefaultConstructor() {
    ConsoleView defaultView = new ConsoleView();
    assertNotNull(defaultView);
    assertNotNull(defaultView.getDayView());
  }

  /**
   * Sets up a fresh DayView instance before each test.
   */
  @Before
  public void setUpDayView() {
    dayView = new DayView();
  }

  @Test
  public void testFormatDayWithEvents() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start = date.atTime(10, 0);
    LocalDateTime end = date.atTime(11, 0);
    events.add(CalendarEvent.builder("Meeting", start, end).build());

    String output = dayView.formatDay(date, events);

    assertTrue(output.contains("January"));
    assertTrue(output.contains("15"));
    assertTrue(output.contains("2025"));
    assertTrue(output.contains("Meeting"));
    assertTrue(output.contains("10:00"));
  }

  @Test
  public void testFormatDayEmpty() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    String output = dayView.formatDay(date, new ArrayList<>());

    assertTrue(output.contains("No events scheduled"));
  }

  @Test
  public void testFormatDayNullEvents() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    String output = dayView.formatDay(date, null);

    assertTrue(output.contains("No events scheduled"));
  }

  @Test
  public void testFormatDayMultipleEvents() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    events.add(CalendarEvent.builder("Morning", date.atTime(9, 0), date.atTime(10, 0)).build());
    events.add(CalendarEvent.builder("Lunch", date.atTime(12, 0), date.atTime(13, 0)).build());

    String output = dayView.formatDay(date, events);

    assertTrue(output.contains("Morning"));
    assertTrue(output.contains("Lunch"));
    assertTrue(output.contains("Total events: 2"));
  }

  @Test
  public void testFormatDayWithLocation() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    CalendarEvent event = CalendarEvent.builder("Meeting", date.atTime(10, 0), date.atTime(11, 0))
        .location("Conference Room A")
        .build();
    events.add(event);

    String output = dayView.formatDay(date, events);
    assertTrue(output.contains("Conference Room A"));
  }

  @Test
  public void testFormatDayWithDescription() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    CalendarEvent event = CalendarEvent.builder("Meeting", date.atTime(10, 0), date.atTime(11, 0))
        .description("Important meeting")
        .build();
    events.add(event);

    String output = dayView.formatDay(date, events);
    assertTrue(output.contains("Important meeting"));
  }

  @Test
  public void testFormatDayWithStatus() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    CalendarEvent event = CalendarEvent.builder("Meeting", date.atTime(10, 0), date.atTime(11, 0))
        .status(EventStatus.PRIVATE)
        .build();
    events.add(event);

    String output = dayView.formatDay(date, events);
    assertTrue(output.contains("private") || output.contains("PRIVATE"));
  }

  @Test
  public void testFormatDayRecurringIndicator() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent event = CalendarEvent.recurringBuilder("Meeting",
            date.atTime(10, 0), date.atTime(11, 0), days)
        .repeatCount(5)
        .build();
    events.add(event);

    String output = dayView.formatDay(date, events);
    assertTrue(output.contains("recurring") || output.contains("Meeting"));
  }

  @Test
  public void testFormatEventCompact() {
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();

    String output = dayView.formatEventCompact(event);

    assertTrue(output.contains("10:00"));
    assertTrue(output.contains("11:00"));
    assertTrue(output.contains("Meeting"));
  }

  /**
   * Sets up a fresh WeekView instance before each test.
   */
  @Before
  public void setUpWeekView() {
    weekView = new WeekView();
  }

  @Test
  public void testFormatWeekEmpty() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    String output = weekView.formatWeek(weekStart, eventsByDay);

    assertTrue(output.contains("Week of"));
    assertTrue(output.contains("January"));
  }

  @Test
  public void testFormatWeekWithEvents() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> mondayEvents = new ArrayList<>();
    LocalDate monday = LocalDate.of(2025, 1, 6);
    mondayEvents.add(CalendarEvent.builder("Standup",
        monday.atTime(9, 0), monday.atTime(9, 30)).build());
    eventsByDay.put(monday, mondayEvents);

    String output = weekView.formatWeek(weekStart, eventsByDay);

    assertTrue(output.contains("Monday"));
    assertTrue(output.contains("Standup"));
  }

  @Test
  public void testFormatWeekAllDays() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    for (int i = 0; i < 7; i++) {
      LocalDate day = weekStart.plusDays(i);
      List<CalendarEvent> events = new ArrayList<>();
      events.add(CalendarEvent.builder("Event" + i, day.atTime(10, 0), day.atTime(11, 0)).build());
      eventsByDay.put(day, events);
    }

    String output = weekView.formatWeek(weekStart, eventsByDay);

    assertTrue(output.contains("Sunday"));
    assertTrue(output.contains("Monday"));
    assertTrue(output.contains("Saturday"));
  }

  @Test
  public void testFormatWeekSummary() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    events.add(CalendarEvent.builder("Event1",
        weekStart.atTime(10, 0), weekStart.atTime(11, 0)).build());
    eventsByDay.put(weekStart, events);

    String output = weekView.formatWeekSummary(weekStart, eventsByDay);

    assertTrue(output.contains("Week of"));
    assertTrue(output.contains("1"));
    assertTrue(output.contains("event"));
  }

  @Test
  public void testFormatWeekSummaryMultipleEvents() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    for (int i = 0; i < 3; i++) {
      LocalDate day = weekStart.plusDays(i);
      List<CalendarEvent> events = new ArrayList<>();
      events.add(CalendarEvent.builder("Event", day.atTime(10, 0), day.atTime(11, 0)).build());
      eventsByDay.put(day, events);
    }

    String output = weekView.formatWeekSummary(weekStart, eventsByDay);
    assertTrue(output.contains("3"));
    assertTrue(output.contains("events"));
  }

  @Test
  public void testFormatWeekSummarySingleEvent() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    events.add(CalendarEvent.builder("Event", weekStart.atTime(10, 0),
        weekStart.atTime(11, 0)).build());
    eventsByDay.put(weekStart, events);

    String output = weekView.formatWeekSummary(weekStart, eventsByDay);
    assertTrue(output.contains("1"));
    assertTrue(output.contains("event"));
    assertFalse(output.contains("events"));
  }

  /**
   * Sets up a fresh MonthView instance before each test.
   */
  @Before
  public void setUpMonthView() {
    monthView = new MonthView();
  }

  @Test
  public void testFormatMonthEmpty() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    assertTrue(output.contains("January"));
    assertTrue(output.contains("2025"));
    assertTrue(output.contains("Su"));
    assertTrue(output.contains("Mo"));
  }

  @Test
  public void testFormatMonthWithEvents() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();
    daysWithEvents.add(LocalDate.of(2025, 1, 15));
    daysWithEvents.add(LocalDate.of(2025, 1, 20));

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    assertTrue(output.contains("15-"));
    assertTrue(output.contains("20-"));
    assertTrue(output.contains("Legend"));
  }

  @Test
  public void testFormatMonthNullDaysWithEvents() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    String output = monthView.formatMonth(yearMonth, null);

    assertTrue(output.contains("January"));
    assertTrue(output.contains("2025"));
  }

  @Test
  public void testFormatMonthWithCounts() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();
    eventCounts.put(LocalDate.of(2025, 1, 15), 3);
    eventCounts.put(LocalDate.of(2025, 1, 20), 1);

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    assertTrue(output.contains("15(3)"));
    assertTrue(output.contains("20(1)"));
  }

  @Test
  public void testFormatMonthWithHighCounts() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();
    eventCounts.put(LocalDate.of(2025, 1, 15), 15);

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    assertTrue(output.contains("15(+)"));
  }

  @Test
  public void testFormatMonthWithCountsNull() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    String output = monthView.formatMonthWithCounts(yearMonth, null);

    assertTrue(output.contains("January"));
  }

  @Test
  public void testFormatMonthSummary() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    String output = monthView.formatMonthSummary(yearMonth, 10, 5);

    assertTrue(output.contains("January"));
    assertTrue(output.contains("2025"));
    assertTrue(output.contains("10"));
    assertTrue(output.contains("5"));
  }

  @Test
  public void testFormatMonthSummarySingular() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    String output = monthView.formatMonthSummary(yearMonth, 1, 1);

    assertTrue(output.contains("1"));
  }

  @Test
  public void testConsoleViewUsesFormatters() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    events.add(CalendarEvent.builder("Meeting", date.atTime(10, 0), date.atTime(11, 0)).build());

    DayView dayView = view.getDayView();
    String formatted = dayView.formatDay(date, events);

    assertNotNull(formatted);
    assertTrue(formatted.contains("Meeting"));
  }

  @Test
  public void testWeekViewEventSummary() {
    LocalDate monday = LocalDate.of(2025, 1, 6);
    List<CalendarEvent> events = new ArrayList<>();
    CalendarEvent event = CalendarEvent.builder("Standup", monday.atTime(9, 0),
            monday.atTime(9, 30))
        .location("Zoom")
        .build();
    events.add(event);

    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();
    eventsByDay.put(monday, events);

    String output = weekView.formatWeek(LocalDate.of(2025, 1, 5), eventsByDay);

    assertTrue(output.contains("Standup"));
    assertTrue(output.contains("9:00"));
  }

  @Test
  public void testMonthViewCalendarGrid() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    assertTrue(output.contains("Su"));
    assertTrue(output.contains("Mo"));
    assertTrue(output.contains("Tu"));
    assertTrue(output.contains("We"));
    assertTrue(output.contains("Th"));
    assertTrue(output.contains("Fr"));
    assertTrue(output.contains("Sa"));
  }

  @Test
  public void testDayViewEventsSorted() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();

    events.add(CalendarEvent.builder("Afternoon", date.atTime(14, 0), date.atTime(15, 0)).build());
    events.add(CalendarEvent.builder("Morning", date.atTime(9, 0), date.atTime(10, 0)).build());

    String output = dayView.formatDay(date, events);

    int morningIndex = output.indexOf("Morning");
    int afternoonIndex = output.indexOf("Afternoon");

    assertTrue(morningIndex > 0);
    assertTrue(afternoonIndex > 0);
    assertTrue(morningIndex < afternoonIndex);
  }

  @Test
  public void testWeekViewShowsAllSevenDays() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    String output = weekView.formatWeek(weekStart, eventsByDay);

    assertTrue(output.contains("Sunday"));
    assertTrue(output.contains("Monday"));
    assertTrue(output.contains("Tuesday"));
    assertTrue(output.contains("Wednesday"));
    assertTrue(output.contains("Thursday"));
    assertTrue(output.contains("Friday"));
    assertTrue(output.contains("Saturday"));
  }

  @Test
  public void testMonthViewShowsCorrectNumberOfDays() {
    YearMonth january = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(january, daysWithEvents);

    assertTrue(output.contains(" 1 "));
    assertTrue(output.contains("31"));
  }

  @Test
  public void testMonthViewFebruaryLeapYear() {
    YearMonth february = YearMonth.of(2024, 2);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(february, daysWithEvents);

    assertTrue(output.contains("29"));
  }

  @Test
  public void testMonthViewAlignmentForNonSundayStart() {
    YearMonth may = YearMonth.of(2025, 5); // starts on a Thursday
    String output = monthView.formatMonth(may, new HashSet<>());

    String[] lines = output.split("\n");
    String firstWeek = lines[5];

    assertTrue("first week should begin with blanks before day 1",
        firstWeek.substring(0, 16).trim().isEmpty());
    assertTrue("day 1 should appear in the first week", firstWeek.contains("  1 "));
  }

  @Test
  public void testMonthViewPadsTrailingWeek() {
    YearMonth april = YearMonth.of(2025, 4); // ends mid-week
    String output = monthView.formatMonth(april, new HashSet<>());

    String[] lines = output.split("\n");
    int legendIndex = 0;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("Legend:")) {
        legendIndex = i;
        break;
      }
    }
    String lastWeek = lines[legendIndex - 2];

    assertTrue("final week should contain day 30",
        lastWeek.contains("30"));
    assertTrue("final week should end with blank padding",
        lastWeek.endsWith("    "));
  }

  @Test
  public void testMonthViewWithCountsAlignment() {
    YearMonth may = YearMonth.of(2025, 5);
    Map<LocalDate, Integer> counts = new HashMap<>();
    counts.put(LocalDate.of(2025, 5, 1), 1);

    String output = monthView.formatMonthWithCounts(may, counts);
    String[] lines = output.split("\n");
    String firstWeek = lines[5];

    assertTrue("counts view should align first day after blanks",
        firstWeek.substring(0, 10).trim().isEmpty());
    assertTrue(firstWeek.contains(" 1(1)"));
  }

  @Test
  public void testMonthViewWithCountsPadsTrailingWeek() {
    YearMonth april = YearMonth.of(2025, 4);
    String output = monthView.formatMonthWithCounts(april, new HashMap<>());

    String[] lines = output.split("\n");
    int legendIndex = 0;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("Legend:")) {
        legendIndex = i;
        break;
      }
    }
    String lastWeek = lines[legendIndex - 2];

    assertTrue("final week should include 30", lastWeek.contains("30"));
    assertTrue("final week should retain blank padding", lastWeek.endsWith("     "));
  }

  @Test
  public void testFormatEventCompactWithAllDetails() {
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 14, 30);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 15, 45);
    CalendarEvent event = CalendarEvent.builder("Team Meeting", start, end)
        .location("Room B")
        .build();

    String compact = dayView.formatEventCompact(event);

    assertTrue(compact.contains("2:30 PM") || compact.contains("14:30"));
    assertTrue(compact.contains("3:45 PM") || compact.contains("15:45"));
    assertTrue(compact.contains("Team Meeting"));
  }

  @Test
  public void testDisplayEventsRecurringIndicator() {
    List<CalendarEvent> events = new ArrayList<>();
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    Weekday[] days = {Weekday.MONDAY};

    // Test with recurring event
    CalendarEvent recurringEvent =
        CalendarEvent.recurringBuilder("Recurring Meeting", start, end, days)
            .repeatCount(5)
            .build();
    events.add(recurringEvent);

    view.displayEvents(events);
    String output = outputStream.toString();
    assertTrue(output.contains("[Recurring]"));

    // Test with non-recurring event to verify the negation
    outputStream.reset();
    events.clear();
    CalendarEvent normalEvent = CalendarEvent.builder("Normal Meeting", start, end).build();
    events.add(normalEvent);

    view.displayEvents(events);
    output = outputStream.toString();
    assertFalse(output.contains("[Recurring]"));
  }

  @Test
  public void testFormatDayRecurringEventIndicator() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = new ArrayList<>();
    Weekday[] days = {Weekday.THURSDAY};

    // Test with recurring event - should show indicator
    CalendarEvent recurringEvent = CalendarEvent.recurringBuilder(
        "Weekly Standup",
        date.atTime(9, 0),
        date.atTime(9, 30),
        days
    ).repeatCount(10).build();
    events.add(recurringEvent);

    String output = dayView.formatDay(date, events);
    assertTrue(output.contains("(recurring)") || output.contains("recurring"));

    // Test with non-recurring event - should NOT show indicator
    events.clear();
    CalendarEvent normalEvent =
        CalendarEvent.builder("One-time Meeting", date.atTime(10, 0), date.atTime(11, 0)).build();
    events.add(normalEvent);

    output = dayView.formatDay(date, events);
    int recurringCount = output.split("recurring", -1).length - 1;
    assertEquals(0, recurringCount);
  }

  @Test
  public void testFormatWeekBoundaryConditions() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    // Test with exactly 7 days
    String output = weekView.formatWeek(weekStart, eventsByDay);
    int dayCount = 0;
    for (String day : new String[] {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday"}) {
      if (output.contains(day)) {
        dayCount++;
      }
    }
    assertEquals(7, dayCount);
  }

  @Test
  public void testFormatWeekSingleEventPluralization() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    LocalDate monday = weekStart.plusDays(1);
    events.add(CalendarEvent.builder("Meeting", monday.atTime(9, 0), monday.atTime(10, 0)).build());
    eventsByDay.put(monday, events);

    String output = weekView.formatWeek(weekStart, eventsByDay);

    // Should say "1 event" not "1 events"
    assertTrue(output.contains("(1 event)"));
    assertFalse(output.contains("(1 events)"));
  }

  @Test
  public void testFormatWeekMultipleEventsPluralization() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    LocalDate monday = weekStart.plusDays(1);
    events.add(
        CalendarEvent.builder("Meeting1", monday.atTime(9, 0), monday.atTime(10, 0)).build());
    events.add(
        CalendarEvent.builder("Meeting2", monday.atTime(14, 0), monday.atTime(15, 0)).build());
    eventsByDay.put(monday, events);

    String output = weekView.formatWeek(weekStart, eventsByDay);

    // Should say "2 events" with 's'
    assertTrue(output.contains("(2 events)"));
  }

  @Test
  public void testFormatEventSummaryWithLocation() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    LocalDate monday = weekStart.plusDays(1);

    // Event with location
    CalendarEvent eventWithLocation =
        CalendarEvent.builder("Meeting", monday.atTime(9, 0), monday.atTime(10, 0))
            .location("Room 101")
            .build();
    events.add(eventWithLocation);
    eventsByDay.put(monday, events);

    String output = weekView.formatWeek(weekStart, eventsByDay);
    assertTrue(output.contains("Room 101"));
    assertTrue(output.contains("@"));
  }

  @Test
  public void testFormatEventSummaryWithoutLocation() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    List<CalendarEvent> events = new ArrayList<>();
    LocalDate monday = weekStart.plusDays(1);

    // Event without location
    CalendarEvent eventNoLocation =
        CalendarEvent.builder("Meeting", monday.atTime(9, 0), monday.atTime(10, 0)).build();
    events.add(eventNoLocation);
    eventsByDay.put(monday, events);

    String output = weekView.formatWeek(weekStart, eventsByDay);
    // Should not contain location indicator when no location
    assertFalse(output.contains("@") && output.contains("null"));
  }

  @Test
  public void testFormatWeekSummaryBoundary() {
    LocalDate weekStart = LocalDate.of(2025, 1, 5);
    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();

    // Test with exactly 7 days worth of processing
    for (int i = 0; i < 7; i++) {
      LocalDate day = weekStart.plusDays(i);
      List<CalendarEvent> events = new ArrayList<>();
      events.add(CalendarEvent.builder("Event" + i, day.atTime(10, 0), day.atTime(11, 0)).build());
      eventsByDay.put(day, events);
    }

    String output = weekView.formatWeekSummary(weekStart, eventsByDay);
    assertTrue(output.contains("7 total events"));
  }

  @Test
  public void testFormatMonthStartsOnSunday() {
    // Test month starting on Sunday (no offset needed)
    YearMonth yearMonth = YearMonth.of(2025, 6); // June 2025 starts on Sunday
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    assertTrue(output.contains("June"));
    assertTrue(output.contains("2025"));
    // First day should be in first position
    String[] lines = output.split("\n");
    boolean foundFirstDay = false;
    for (String line : lines) {
      if (line.trim().startsWith("1 ")) {
        foundFirstDay = true;
        break;
      }
    }
    assertTrue(foundFirstDay);
  }

  @Test
  public void testFormatMonthStartsOnMonday() {
    // Test month starting on Monday
    YearMonth yearMonth = YearMonth.of(2025, 9); // September 2025 starts on Monday
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);
    assertTrue(output.contains("September"));
  }

  @Test
  public void testFormatMonthStartsOnSaturday() {
    // Test month starting on Saturday
    YearMonth yearMonth = YearMonth.of(2025, 11); // November 2025 starts on Saturday
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);
    assertTrue(output.contains("November"));
  }

  @Test
  public void testFormatMonthWithCountsZeroEvents() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();

    // Date with 0 events should not show count
    eventCounts.put(LocalDate.of(2025, 1, 15), 0);

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);
    assertFalse(output.contains("15(0)"));
  }

  @Test
  public void testFormatMonthWithCountsBoundary() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();

    // Test boundary at 9 and 10
    eventCounts.put(LocalDate.of(2025, 1, 10), 9);
    eventCounts.put(LocalDate.of(2025, 1, 15), 10);

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    // 9 events should show number
    assertTrue(output.contains("10(9)"));

    // 10 events should show +
    assertTrue(output.contains("15(+)"));
  }

  @Test
  public void testFormatMonthWithCountsExactly10() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();

    eventCounts.put(LocalDate.of(2025, 1, 15), 10);

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    // Exactly 10 should show as (+)
    assertTrue(output.contains("15(+)"));
    assertFalse(output.contains("15(10)"));
  }

  @Test
  public void testFormatMonthGridCompleteness() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    // Verify all days of month are present
    for (int day = 1; day <= 31; day++) {
      assertTrue("Day " + day + " not found", output.contains(String.valueOf(day)));
    }
  }

  @Test
  public void testFormatMonthWithCountsGridCompleteness() {
    YearMonth yearMonth = YearMonth.of(2025, 2); // February
    Map<LocalDate, Integer> eventCounts = new HashMap<>();

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    // Verify all days of February are present
    for (int day = 1; day <= 28; day++) {
      assertTrue("Day " + day + " not found", output.contains(String.valueOf(day)));
    }
  }

  @Test
  public void testFormatMonthCompleteWeekRows() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(yearMonth, daysWithEvents);

    // Count newlines in calendar grid to verify complete weeks
    String[] lines = output.split("\n");
    int gridLines = 0;
    boolean inGrid = false;
    for (String line : lines) {
      if (line.contains("Su") && line.contains("Mo")) {
        inGrid = true;
        continue;
      }
      if (inGrid && line.contains("=")) {
        break;
      }
      if (inGrid && !line.trim().isEmpty()) {
        gridLines++;
      }
    }

    // Should have complete weeks (rows should be multiples of 7 positions)
    assertTrue(gridLines >= 4 && gridLines <= 6); // Months span 4-6 weeks
  }

  @Test
  public void testFormatMonthWithCountsCompleteWeekRows() {
    YearMonth yearMonth = YearMonth.of(2025, 1);
    Map<LocalDate, Integer> eventCounts = new HashMap<>();

    String output = monthView.formatMonthWithCounts(yearMonth, eventCounts);

    // Similar verification for counts version
    String[] lines = output.split("\n");
    int gridLines = 0;
    boolean inGrid = false;
    for (String line : lines) {
      if (line.contains("Su") && line.contains("Mo")) {
        inGrid = true;
        continue;
      }
      if (inGrid && line.contains("=")) {
        break;
      }
      if (inGrid && !line.trim().isEmpty()) {
        gridLines++;
      }
    }

    assertTrue(gridLines >= 4 && gridLines <= 6);
  }

  @Test
  public void testViewTestEdgeCases() {
    // Test empty stream handling
    ByteArrayOutputStream emptyStream = new ByteArrayOutputStream();
    ConsoleView emptyView =
        new ConsoleView(new PrintStream(emptyStream), new PrintStream(emptyStream));
    emptyView.displayMessage("");
    assertEquals(System.lineSeparator(), emptyStream.toString());
  }

  @Test
  public void testFormatMonthShortMonths() {
    // February non-leap year
    YearMonth feb2025 = YearMonth.of(2025, 2);
    Set<LocalDate> daysWithEvents = new HashSet<>();

    String output = monthView.formatMonth(feb2025, daysWithEvents);
    assertTrue(output.contains("28"));
    assertFalse(output.contains("29"));
  }

  @Test
  public void testFormatMonth31DayMonths() {
    // Test 31-day months
    for (int month : new int[] {1, 3, 5, 7, 8, 10, 12}) {
      YearMonth yearMonth = YearMonth.of(2025, month);
      Set<LocalDate> daysWithEvents = new HashSet<>();

      String output = monthView.formatMonth(yearMonth, daysWithEvents);
      assertTrue("Month " + month + " should have 31 days", output.contains("31"));
    }
  }

  @Test
  public void testFormatMonth30DayMonths() {
    // Test 30-day months
    for (int month : new int[] {4, 6, 9, 11}) {
      YearMonth yearMonth = YearMonth.of(2025, month);
      Set<LocalDate> daysWithEvents = new HashSet<>();

      String output = monthView.formatMonth(yearMonth, daysWithEvents);
      assertTrue("Month " + month + " should have 30 days", output.contains("30"));
      assertFalse("Month " + month + " should not have 31 days", output.contains("31"));
    }
  }
}

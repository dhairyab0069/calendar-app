import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.controller.CommandHandler;
import calendar.controller.parser.CommandParser;
import calendar.controller.parser.ParsedCommand;
import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.InvalidCommandException;
import calendar.model.CalendarEvent;
import calendar.model.CalendarModel;
import calendar.model.EventStatus;
import calendar.model.SparseHashCalendar;
import calendar.model.Weekday;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the Calendar Application using JUnit 4.
 *
 * <p>Tests the complete application workflow including:
 * <ul>
 *   <li>End-to-end command processing</li>
 *   <li>Parser → Handler → Model → View integration</li>
 *   <li>Real-world usage scenarios</li>
 *   <li>Error handling across layers</li>
 * </ul>
 *
 * @version 1.1
 */
public class CalendarTest {

  private CalendarModel model;
  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private CalendarView view;
  private CommandParser parser;
  private CommandHandler handler;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    model = new SparseHashCalendar();
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    view = new ConsoleView(new PrintStream(outputStream), new PrintStream(errorStream));
    parser = new CommandParser();
    handler = new CommandHandler(model, view);
    try {
      model.createCalendar("Default", ZoneId.of("America/New_York"));
      model.useCalendar("Default");
    } catch (DuplicateCalendarException | CalendarNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testCreateAndRetrieveSingleEvent() throws InvalidCommandException {
    String createCommand = "create event Meeting from 2025-01-15T10:00 to 2025-01-15T11:00";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Meeting"));

    outputStream.reset();
    String printCommand = "print 2025-01-15";
    parsed = parser.parse(printCommand);
    handler.execute(parsed);

    output = outputStream.toString();
    assertTrue(output.contains("Meeting"));
  }

  @Test
  public void testCreateWithoutEventKeyword() throws InvalidCommandException {
    String createCommand = "create Meeting from 2025-01-15T10:00 to 2025-01-15T11:00";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Meeting"));
  }

  @Test
  public void testCreateAndRetrieveAllDayEvent() throws InvalidCommandException {
    String createCommand = "create event Holiday on 2025-01-20";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Holiday"));

    outputStream.reset();
    String printCommand = "print 2025-01-20";
    parsed = parser.parse(printCommand);
    handler.execute(parsed);

    output = outputStream.toString();
    assertTrue(output.contains("Holiday"));
  }

  @Test
  public void testCreateRecurringEventWithFor() throws InvalidCommandException {
    String createCommand = "create event Standup from 2025-01-06T09:00 to 2025-01-06T09:30 "
        + "repeats MWF for 4 times";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(4, allEvents.size());
  }

  @Test
  public void testCreateRecurringEventWithoutFor() throws InvalidCommandException {
    String createCommand = "create event Standup from 2025-01-06T09:00 to 2025-01-06T09:30 "
        + "repeats MWF 4 times";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(4, allEvents.size());
  }

  @Test
  public void testCreateRecurringEventUntilDate() throws InvalidCommandException {
    String createCommand = "create event Daily from 2025-01-01T08:00 to 2025-01-01T08:30 "
        + "repeats MTWRFSU until 2025-01-07";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(7, allEvents.size());
  }

  @Test
  public void testDuplicateEventError() throws InvalidCommandException {
    String createCommand = "create event Meeting from 2025-01-15T10:00 to 2025-01-15T11:00";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    outputStream.reset();
    errorStream.reset();
    parsed = parser.parse(createCommand);
    handler.execute(parsed);

    String error = errorStream.toString();
    assertTrue(error.contains("Duplicate event"));
  }

  @Test
  public void testPrintEventsOnSyntax() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String printCommand = "print events on 2025-01-15";
    ParsedCommand parsed = parser.parse(printCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Meeting"));
  }

  @Test
  public void testPrintEventsInRange() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Event1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());
    model.addEvent(CalendarEvent.builder("Event2",
        LocalDateTime.of(2025, 1, 17, 10, 0),
        LocalDateTime.of(2025, 1, 17, 11, 0)).build());

    String printCommand = "print from 2025-01-15 to 2025-01-20";
    ParsedCommand parsed = parser.parse(printCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event1"));
    assertTrue(output.contains("Event2"));
  }

  @Test
  public void testPrintEventsFromSyntax() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Event1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String printCommand = "print events from 2025-01-15 to 2025-01-20";
    ParsedCommand parsed = parser.parse(printCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event1"));
  }

  @Test
  public void testPrintNoEvents() throws InvalidCommandException {
    String printCommand = "print 2025-01-15";
    ParsedCommand parsed = parser.parse(printCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("No events on 2025-01-15"));
  }

  @Test
  public void testPrintNoEventsInRange() throws InvalidCommandException {
    String printCommand = "print from 2025-01-15 to 2025-01-20";
    ParsedCommand parsed = parser.parse(printCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("No events from"));
  }

  @Test
  public void testBusyStatus() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String statusCommand = "status 2025-01-15T10:30";
    ParsedCommand parsed = parser.parse(statusCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("busy"));
  }

  @Test
  public void testAvailableStatus() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String statusCommand = "status 2025-01-15T12:00";
    ParsedCommand parsed = parser.parse(statusCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("available"));
  }

  @Test
  public void testExportCalendar() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String exportCommand = "export test_calendar.csv";
    ParsedCommand parsed = parser.parse(exportCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Calendar exported to: test_calendar.csv"));
  }

  @Test
  public void testQuotedSubject() throws InvalidCommandException {
    String createCommand = "create event \"Team Meeting\" from 2025-01-15T10:00 "
        + "to 2025-01-15T11:00";
    ParsedCommand parsed = parser.parse(createCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Team Meeting"));

    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals("Team Meeting", events.get(0).getSubject());
  }

  @Test
  public void testViewDayCommand() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String viewCommand = "view day 2025-01-15";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("2025") || output.contains("Meeting"));
  }

  @Test
  public void testViewWeekCommand() throws InvalidCommandException {
    String viewCommand = "view week 2025-01-06";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  @Test
  public void testViewMonthShortFormat() throws InvalidCommandException {
    String viewCommand = "view month 2025-01";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  @Test
  public void testViewMonthFullDate() throws InvalidCommandException {
    String viewCommand = "view month 2025-01-15";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  @Test
  public void testExitCommand() throws InvalidCommandException {
    String exitCommand = "exit";
    ParsedCommand parsed = parser.parse(exitCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Goodbye"));
  }

  @Test
  public void testRecurringMwfPattern() throws InvalidCommandException {
    String command = "create event Standup from 2025-01-06T09:00 to 2025-01-06T09:30 "
        + "repeats MWF for 6 times";
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(6, allEvents.size());

    String seriesId = allEvents.get(0).getSeriesId();
    assertNotNull(seriesId);
    for (CalendarEvent event : allEvents) {
      assertEquals("Standup", event.getSubject());
      assertEquals(seriesId, event.getSeriesId());
    }
  }

  @Test
  public void testRecurringOnlyMatchingDays() throws InvalidCommandException {
    String command = "create event Meeting from 2025-01-06T10:00 to 2025-01-06T11:00 "
        + "repeats MF for 4 times";
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);

    assertEquals(1, model.getEventsOnDate(LocalDate.of(2025, 1, 6)).size());
    assertEquals(0, model.getEventsOnDate(LocalDate.of(2025, 1, 7)).size());
    assertEquals(0, model.getEventsOnDate(LocalDate.of(2025, 1, 8)).size());
    assertEquals(1, model.getEventsOnDate(LocalDate.of(2025, 1, 10)).size());
  }

  @Test
  public void testRecurringDuplicatePrevention() throws InvalidCommandException {
    String command = "create event Standup from 2025-01-06T09:00 to 2025-01-06T09:30 "
        + "repeats M for 2 times";
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);

    ByteArrayOutputStream errorStream2 = new ByteArrayOutputStream();
    CalendarView errorView = new ConsoleView(new PrintStream(new ByteArrayOutputStream()),
        new PrintStream(errorStream2));
    CommandHandler errorHandler = new CommandHandler(model, errorView);

    errorHandler.execute(parsed);

    String error = errorStream2.toString();
    assertTrue(error.contains("Duplicate"));
  }

  @Test
  public void testAllDayRecurringEvent() throws InvalidCommandException {
    String command = "create event Weekend on 2025-01-04 repeats SU for 4 times";
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(4, allEvents.size());
  }

  @Test(expected = InvalidCommandException.class)
  public void testEmptyCommand() throws InvalidCommandException {
    parser.parse("");
  }

  @Test(expected = InvalidCommandException.class)
  public void testWhitespaceCommand() throws InvalidCommandException {
    parser.parse("   ");
  }

  @Test(expected = InvalidCommandException.class)
  public void testNullCommand() throws InvalidCommandException {
    parser.parse(null);
  }

  @Test(expected = InvalidCommandException.class)
  public void testUnknownCommand() throws InvalidCommandException {
    parser.parse("invalid command");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateWithoutTime() throws InvalidCommandException {
    parser.parse("create event Meeting");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateInvalidDateFormat() throws InvalidCommandException {
    parser.parse("create event Meeting from 01/15/2025T10:00 to 01/15/2025T11:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateWithoutTo() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-15T10:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateEndBeforeStart() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-15T11:00 to 2025-01-15T10:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateEndEqualsStart() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-15T10:00 to 2025-01-15T10:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateUnclosedQuote() throws InvalidCommandException {
    parser.parse("create event \"Meeting from 2025-01-15T10:00 to 2025-01-15T11:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testCreateEmptySubject() throws InvalidCommandException {
    parser.parse("create event \"\" from 2025-01-15T10:00 to 2025-01-15T11:00");
  }

  @Test(expected = InvalidCommandException.class)
  public void testPrintInvalidDateFormat() throws InvalidCommandException {
    parser.parse("print 2025-13-45");
  }

  @Test(expected = InvalidCommandException.class)
  public void testPrintRangeWithoutTo() throws InvalidCommandException {
    parser.parse("print from 2025-01-15");
  }

  @Test(expected = InvalidCommandException.class)
  public void testStatusInvalidFormat() throws InvalidCommandException {
    parser.parse("status 2025-01-15");
  }

  @Test(expected = InvalidCommandException.class)
  public void testExportEmptyFilename() throws InvalidCommandException {
    parser.parse("export ");
  }

  @Test(expected = InvalidCommandException.class)
  public void testRecurringInvalidWeekdays() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-06T10:00 to 2025-01-06T11:00 "
        + "repeats XYZ for 5 times");
  }

  @Test(expected = InvalidCommandException.class)
  public void testRecurringNegativeCount() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-06T10:00 to 2025-01-06T11:00 "
        + "repeats MWF for -5 times");
  }

  @Test(expected = InvalidCommandException.class)
  public void testRecurringZeroCount() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-06T10:00 to 2025-01-06T11:00 "
        + "repeats MWF for 0 times");
  }

  @Test(expected = InvalidCommandException.class)
  public void testRecurringIncompleteClause() throws InvalidCommandException {
    parser.parse("create event Meeting from 2025-01-06T10:00 to 2025-01-06T11:00 "
        + "repeats MWF");
  }

  @Test(expected = InvalidCommandException.class)
  public void testViewDayInvalidDate() throws InvalidCommandException {
    parser.parse("view day invalid-date");
  }

  @Test(expected = InvalidCommandException.class)
  public void testViewMonthInvalidFormat() throws InvalidCommandException {
    parser.parse("view month 01/2025");
  }

  @Test
  public void testEventPersistence() throws DuplicateEventException {
    LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime end = LocalDateTime.of(2025, 1, 15, 11, 0);
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();

    model.addEvent(event);

    List<CalendarEvent> events1 = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    List<CalendarEvent> events2 = model.getEventsOnDate(LocalDate.of(2025, 1, 15));

    assertEquals(1, events1.size());
    assertEquals(1, events2.size());
    assertEquals("Meeting", events1.get(0).getSubject());
  }

  @Test
  public void testEventCount() throws DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Event1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());
    model.addEvent(CalendarEvent.builder("Event2",
        LocalDateTime.of(2025, 1, 16, 10, 0),
        LocalDateTime.of(2025, 1, 16, 11, 0)).build());

    assertEquals(2, model.getAllEvents().size());
  }

  @Test
  public void testSeriesRelationships() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY, Weekday.FRIDAY};
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup",
            LocalDateTime.of(2025, 1, 6, 9, 0),
            LocalDateTime.of(2025, 1, 6, 9, 30),
            days)
        .repeatCount(4)
        .build();

    model.addEvent(event);

    List<CalendarEvent> allEvents = model.getAllEvents();
    String seriesId = allEvents.get(0).getSeriesId();

    for (CalendarEvent e : allEvents) {
      assertEquals(seriesId, e.getSeriesId());
      assertTrue(e.isRecurring());
    }
  }

  @Test
  public void testMidnightEvents() throws DuplicateEventException {
    LocalDateTime midnight = LocalDateTime.of(2025, 1, 15, 0, 0);
    LocalDateTime afterMidnight = midnight.plusHours(1);

    CalendarEvent event = CalendarEvent.builder("Midnight Event", midnight, afterMidnight).build();
    model.addEvent(event);

    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
  }

  @Test
  public void testEndOfDayEvents() throws DuplicateEventException {
    LocalDateTime lateEvening = LocalDateTime.of(2025, 1, 15, 23, 0);
    LocalDateTime endOfDay = LocalDateTime.of(2025, 1, 15, 23, 59);

    CalendarEvent event = CalendarEvent.builder("Late Event", lateEvening, endOfDay).build();
    model.addEvent(event);

    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
  }

  @Test
  public void testYearBoundary() throws DuplicateEventException {
    LocalDateTime newYearsEve = LocalDateTime.of(2024, 12, 31, 23, 0);
    LocalDateTime newYearsDay = LocalDateTime.of(2025, 1, 1, 1, 0);

    model.addEvent(CalendarEvent.builder("NYE Event", newYearsEve,
        LocalDateTime.of(2024, 12, 31, 23, 59)).build());
    model.addEvent(CalendarEvent.builder("NY Event", newYearsDay,
        LocalDateTime.of(2025, 1, 1, 2, 0)).build());

    assertEquals(1, model.getEventsOnDate(LocalDate.of(2024, 12, 31)).size());
    assertEquals(1, model.getEventsOnDate(LocalDate.of(2025, 1, 1)).size());
  }

  @Test
  public void testLeapYearDate() throws DuplicateEventException {
    LocalDateTime leapDay = LocalDateTime.of(2024, 2, 29, 10, 0);
    LocalDateTime leapDayEnd = LocalDateTime.of(2024, 2, 29, 11, 0);

    CalendarEvent event = CalendarEvent.builder("Leap Day Event", leapDay, leapDayEnd).build();
    model.addEvent(event);

    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2024, 2, 29));
    assertEquals(1, events.size());
  }

  @Test
  public void testEmptyDateQueries() {
    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertNotNull(events);
    assertTrue(events.isEmpty());
  }

  @Test
  public void testSingleDayRange() throws DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    List<CalendarEvent> events = model.getEventsInRange(
        LocalDate.of(2025, 1, 15),
        LocalDate.of(2025, 1, 15)
    );
    assertEquals(1, events.size());
  }

  @Test
  public void testBusyDayMultipleEvents() throws DuplicateEventException {
    LocalDate busyDay = LocalDate.of(2025, 1, 15);

    model.addEvent(CalendarEvent.builder("Morning Meeting",
        busyDay.atTime(9, 0), busyDay.atTime(10, 0)).build());
    model.addEvent(CalendarEvent.builder("Lunch",
        busyDay.atTime(12, 0), busyDay.atTime(13, 0)).build());
    model.addEvent(CalendarEvent.builder("Afternoon Meeting",
        busyDay.atTime(14, 0), busyDay.atTime(15, 0)).build());

    List<CalendarEvent> events = model.getEventsOnDate(busyDay);
    assertEquals(3, events.size());
  }

  @Test
  public void testMultipleRecurringSeries() throws DuplicateEventException {
    model.addEvent(CalendarEvent.recurringBuilder("Standup",
            LocalDateTime.of(2025, 1, 6, 9, 0),
            LocalDateTime.of(2025, 1, 6, 9, 30),
            new Weekday[] {Weekday.MONDAY})
        .repeatCount(4)
        .build());

    model.addEvent(CalendarEvent.recurringBuilder("Review",
            LocalDateTime.of(2025, 1, 8, 14, 0),
            LocalDateTime.of(2025, 1, 8, 15, 0),
            new Weekday[] {Weekday.WEDNESDAY})
        .repeatCount(4)
        .build());

    List<CalendarEvent> allEvents = model.getAllEvents();
    assertEquals(8, allEvents.size());
  }

  @Test
  public void testCsvExportComplexData() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting, \"Important\"",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0))
        .description("Line1\nLine2, with comma")
        .location("Room \"A\", Building 1")
        .status(EventStatus.PRIVATE)
        .build();

    model.addEvent(event);

    String csv = model.exportToCsv();

    assertTrue(csv.startsWith("Subject,Start,End,Description,Location,Status"));
    assertTrue(csv.contains("\"Meeting, \"\"Important\"\"\""));
    assertTrue(csv.contains("private"));
  }

  // NEW TESTS TO KILL MUTATIONS

  /**
   * Test edit single event functionality.
   */
  @Test
  public void testEditSingleEventSubject() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand = "edit event subject Meeting from 2025-01-15T10:00 with \"Team Standup\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));

    List<CalendarEvent> events = model.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals("Team Standup", events.get(0).getSubject());
  }

  /**
   * Test edit single event location.
   */
  @Test
  public void testEditSingleEventLocation()
      throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand = "edit event location Meeting from 2025-01-15T10:00 with \"Room 405\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));
  }

  /**
   * Test edit single event description.
   */
  @Test
  public void testEditSingleEventDescription()
      throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand =
        "edit event description Meeting from 2025-01-15T10:00 with \"Important discussion\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));
  }

  /**
   * Test edit single event status.
   */
  @Test
  public void testEditSingleEventStatus() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand = "edit event status Meeting from 2025-01-15T10:00 with \"private\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));
  }

  /**
   * Test edit single event start time.
   */
  @Test
  public void testEditSingleEventStartTime()
      throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand = "edit event start Meeting from 2025-01-15T10:00 with \"2025-01-15T09:00\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));
  }

  /**
   * Test edit single event end time.
   */
  @Test
  public void testEditSingleEventEndTime() throws InvalidCommandException, DuplicateEventException {
    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String editCommand = "edit event end Meeting from 2025-01-15T10:00 with \"2025-01-15T12:00\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated successfully"));
  }

  /**
   * Test edit event that doesn't exist.
   */
  @Test
  public void testEditNonExistentEvent() throws InvalidCommandException {
    String editCommand = "edit event subject Meeting from 2025-01-15T10:00 with \"New Subject\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String error = errorStream.toString();
    assertTrue(error.contains("not found"));
  }

  /**
   * Test edit series from date functionality.
   */
  @Test
  public void testEditSeriesFromDate() throws InvalidCommandException, DuplicateEventException {
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup",
            LocalDateTime.of(2025, 1, 6, 9, 0),
            LocalDateTime.of(2025, 1, 6, 9, 30),
            new Weekday[] {Weekday.MONDAY, Weekday.WEDNESDAY})
        .repeatCount(4)
        .build();
    model.addEvent(event);

    String editCommand =
        "edit events subject Standup from 2025-01-13T09:00 with \"Updated Standup\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("updated from"));
  }

  /**
   * Test edit entire series functionality.
   */
  @Test
  public void testEditEntireSeries() throws InvalidCommandException, DuplicateEventException {
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup",
            LocalDateTime.of(2025, 1, 6, 9, 0),
            LocalDateTime.of(2025, 1, 6, 9, 30),
            new Weekday[] {Weekday.MONDAY})
        .repeatCount(3)
        .build();
    model.addEvent(event);

    String editCommand =
        "edit series description Standup from 2025-01-06T09:00 with \"Daily sync\"";
    ParsedCommand parsed = parser.parse(editCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Entire series"));
  }

  /**
   * Test view with non-ConsoleView to trigger else branch.
   */
  @Test
  public void testViewDayWithCustomView() throws InvalidCommandException, DuplicateEventException {
    // Create a minimal custom view implementation
    CalendarView customView = new CalendarView() {
      private final PrintStream out = new PrintStream(outputStream);
      private final PrintStream err = new PrintStream(errorStream);

      @Override
      public void displayMessage(String message) {
        out.println(message);
      }

      @Override
      public void displayError(String error) {
        err.println(error);
      }

      @Override
      public void displayEvents(List<CalendarEvent> events) {
        for (CalendarEvent event : events) {
          out.println(event.getSubject());
        }
      }
    };

    CommandHandler customHandler = new CommandHandler(model, customView);

    model.addEvent(CalendarEvent.builder("Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());

    String viewCommand = "view day 2025-01-15";
    ParsedCommand parsed = parser.parse(viewCommand);
    customHandler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Events on") || output.contains("Meeting"));
  }

  /**
   * Test view week with non-ConsoleView.
   */
  @Test
  public void testViewWeekWithCustomView() throws InvalidCommandException {
    CalendarView customView = new CalendarView() {
      private final PrintStream out = new PrintStream(outputStream);
      private final PrintStream err = new PrintStream(errorStream);

      @Override
      public void displayMessage(String message) {
        out.println(message);
      }

      @Override
      public void displayError(String error) {
        err.println(error);
      }

      @Override
      public void displayEvents(List<CalendarEvent> events) {
        for (CalendarEvent event : events) {
          out.println(event.getSubject());
        }
      }
    };

    CommandHandler customHandler = new CommandHandler(model, customView);

    String viewCommand = "view week 2025-01-06";
    ParsedCommand parsed = parser.parse(viewCommand);
    customHandler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Week starting"));
  }

  /**
   * Test view month with non-ConsoleView.
   */
  @Test
  public void testViewMonthWithCustomView() throws InvalidCommandException {
    CalendarView customView = new CalendarView() {
      private final PrintStream out = new PrintStream(outputStream);
      private final PrintStream err = new PrintStream(errorStream);

      @Override
      public void displayMessage(String message) {
        out.println(message);
      }

      @Override
      public void displayError(String error) {
        err.println(error);
      }

      @Override
      public void displayEvents(List<CalendarEvent> events) {
        for (CalendarEvent event : events) {
          out.println(event.getSubject());
        }
      }
    };

    CommandHandler customHandler = new CommandHandler(model, customView);

    String viewCommand = "view month 2025-01";
    ParsedCommand parsed = parser.parse(viewCommand);
    customHandler.execute(parsed);

    String output = outputStream.toString();
    assertTrue(output.contains("Month:") || output.contains("Days with events:"));
  }

  /**
   * Test view week with events to ensure loop processes all 7 days.
   */
  @Test
  public void testViewWeekWithEventsOnMultipleDays()
      throws InvalidCommandException, DuplicateEventException {
    // Add events on different days of the week
    LocalDate weekStart = LocalDate.of(2025, 1, 6); // Monday
    model.addEvent(CalendarEvent.builder("Monday Event",
        weekStart.atTime(10, 0), weekStart.atTime(11, 0)).build());
    model.addEvent(CalendarEvent.builder("Wednesday Event",
        weekStart.plusDays(2).atTime(10, 0), weekStart
            .plusDays(2).atTime(11, 0)).build());
    model.addEvent(CalendarEvent.builder("Saturday Event",
        weekStart.plusDays(5).atTime(10, 0), weekStart.plusDays(5)
            .atTime(11, 0)).build());

    String viewCommand = "view week 2025-01-06";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
    // Verify the view processes the full week
    assertTrue(output.length() > 100); // Should have substantial output for week view
  }

  /**
   * Test view month with events to verify set collection works.
   */
  @Test
  public void testViewMonthWithMultipleEvents() throws InvalidCommandException,
      DuplicateEventException {
    // Add multiple events throughout the month
    model.addEvent(CalendarEvent.builder("Event1",
        LocalDateTime.of(2025, 1, 5, 10, 0),
        LocalDateTime.of(2025, 1, 5, 11, 0)).build());
    model.addEvent(CalendarEvent.builder("Event2",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build());
    model.addEvent(CalendarEvent.builder("Event3",
        LocalDateTime.of(2025, 1, 25, 10, 0),
        LocalDateTime.of(2025, 1, 25, 11, 0)).build());

    String viewCommand = "view month 2025-01";
    ParsedCommand parsed = parser.parse(viewCommand);
    handler.execute(parsed);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }
}
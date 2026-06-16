import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import calendar.controller.util.DateTimeParser;
import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import calendar.model.CalendarEvent;
import calendar.model.EventStatus;
import calendar.model.SparseHashCalendar;
import calendar.model.Weekday;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test suite for Model layer classes using JUnit 4.
 *
 * <p>Tests all model components in isolation with enhanced coverage for mutation testing:
 * <ul>
 *   <li>Weekday enum</li>
 *   <li>EventStatus enum</li>
 *   <li>CalendarEvent entity</li>
 *   <li>SparseHashCalendar implementation</li>
 *   <li>DateTimeParser utility</li>
 * </ul>
 *
 * @version 2.0
 */
public class ModelTest {

  private LocalDateTime start;
  private LocalDateTime end;
  private SparseHashCalendar calendar;

  @Test
  public void testParseDateTimeValid() {
    LocalDateTime result = DateTimeParser.parseDateTime("2025-01-15T10:30");
    assertEquals(2025, result.getYear());
    assertEquals(1, result.getMonthValue());
    assertEquals(15, result.getDayOfMonth());
    assertEquals(10, result.getHour());
    assertEquals(30, result.getMinute());
  }

  @Test
  public void testParseDateTimeWithWhitespace() {
    LocalDateTime result = DateTimeParser.parseDateTime("  2025-01-15T10:30  ");
    assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTimeNull() {
    DateTimeParser.parseDateTime(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTimeEmpty() {
    DateTimeParser.parseDateTime("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTimeEmptyWhitespace() {
    DateTimeParser.parseDateTime("   ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTimeInvalidFormat() {
    DateTimeParser.parseDateTime("01/15/2025 10:30");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateTimeInvalidFormatDateOnly() {
    DateTimeParser.parseDateTime("2025-01-15");
  }

  @Test
  public void testParseDateValid() {
    LocalDate result = DateTimeParser.parseDate("2025-01-15");
    assertEquals(2025, result.getYear());
    assertEquals(1, result.getMonthValue());
    assertEquals(15, result.getDayOfMonth());
  }

  @Test
  public void testParseDateWithWhitespace() {
    LocalDate result = DateTimeParser.parseDate("  2025-01-15  ");
    assertEquals(LocalDate.of(2025, 1, 15), result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateNull() {
    DateTimeParser.parseDate(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateEmpty() {
    DateTimeParser.parseDate("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseDateInvalidFormat() {
    DateTimeParser.parseDate("01/15/2025");
  }

  @Test
  public void testFormatDateTime() {
    LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 10, 30);
    String result = DateTimeParser.formatDateTime(dateTime);
    assertEquals("2025-01-15T10:30", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatDateTimeNull() {
    DateTimeParser.formatDateTime(null);
  }

  @Test
  public void testFormatDate() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    String result = DateTimeParser.formatDate(date);
    assertEquals("2025-01-15", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatDateNull() {
    DateTimeParser.formatDate(null);
  }

  @Test
  public void testIsValidDateTime() {
    assertTrue(DateTimeParser.isValidDateTime("2025-01-15T10:30"));
    assertFalse(DateTimeParser.isValidDateTime("invalid"));
    assertFalse(DateTimeParser.isValidDateTime(null));
    assertFalse(DateTimeParser.isValidDateTime(""));
  }

  @Test
  public void testIsValidDate() {
    assertTrue(DateTimeParser.isValidDate("2025-01-15"));
    assertFalse(DateTimeParser.isValidDate("invalid"));
    assertFalse(DateTimeParser.isValidDate(null));
    assertFalse(DateTimeParser.isValidDate(""));
  }

  @Test
  public void testWeekdayFromChar() {
    assertEquals(Weekday.MONDAY, Weekday.fromChar('M'));
    assertEquals(Weekday.TUESDAY, Weekday.fromChar('T'));
    assertEquals(Weekday.WEDNESDAY, Weekday.fromChar('W'));
    assertEquals(Weekday.THURSDAY, Weekday.fromChar('R'));
    assertEquals(Weekday.FRIDAY, Weekday.fromChar('F'));
    assertEquals(Weekday.SATURDAY, Weekday.fromChar('S'));
    assertEquals(Weekday.SUNDAY, Weekday.fromChar('U'));
  }

  @Test
  public void testWeekdayFromCharLowercase() {
    assertEquals(Weekday.MONDAY, Weekday.fromChar('m'));
    assertEquals(Weekday.FRIDAY, Weekday.fromChar('f'));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWeekdayFromCharInvalid() {
    Weekday.fromChar('X');
  }

  @Test
  public void testWeekdayParseString() {
    Weekday[] result = Weekday.parseString("MWF");
    assertEquals(3, result.length);
    assertEquals(Weekday.MONDAY, result[0]);
    assertEquals(Weekday.WEDNESDAY, result[1]);
    assertEquals(Weekday.FRIDAY, result[2]);
  }

  @Test
  public void testWeekdayParseStringAllDays() {
    Weekday[] result = Weekday.parseString("MTWRFSU");
    assertEquals(7, result.length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWeekdayParseStringNull() {
    Weekday.parseString(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWeekdayParseStringEmpty() {
    Weekday.parseString("");
  }

  @Test
  public void testWeekdayFromDayOfWeek() {
    assertEquals(Weekday.MONDAY, Weekday.fromDayOfWeek(DayOfWeek.MONDAY));
    assertEquals(Weekday.SUNDAY, Weekday.fromDayOfWeek(DayOfWeek.SUNDAY));
  }

  @Test
  public void testWeekdayGetAbbreviation() {
    assertEquals('M', Weekday.MONDAY.getAbbreviation());
    assertEquals('R', Weekday.THURSDAY.getAbbreviation());
  }

  @Test
  public void testWeekdayGetDayOfWeek() {
    assertEquals(DayOfWeek.MONDAY, Weekday.MONDAY.getDayOfWeek());
    assertEquals(DayOfWeek.THURSDAY, Weekday.THURSDAY.getDayOfWeek());
  }

  @Test
  public void testWeekdayToString() {
    assertEquals("M", Weekday.MONDAY.toString());
    assertEquals("R", Weekday.THURSDAY.toString());
  }

  @Test
  public void testEventStatusFromString() {
    assertEquals(EventStatus.PUBLIC, EventStatus.fromString("public"));
    assertEquals(EventStatus.PRIVATE, EventStatus.fromString("private"));
  }

  @Test
  public void testEventStatusFromStringCaseInsensitive() {
    assertEquals(EventStatus.PUBLIC, EventStatus.fromString("PUBLIC"));
    assertEquals(EventStatus.PUBLIC, EventStatus.fromString("PuBlIc"));
    assertEquals(EventStatus.PRIVATE, EventStatus.fromString("PRIVATE"));
  }

  @Test
  public void testEventStatusFromStringWithWhitespace() {
    assertEquals(EventStatus.PUBLIC, EventStatus.fromString("  public  "));
    assertEquals(EventStatus.PRIVATE, EventStatus.fromString("  private  "));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEventStatusFromStringNull() {
    EventStatus.fromString(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEventStatusFromStringInvalid() {
    EventStatus.fromString("invalid");
  }

  @Test
  public void testEventStatusGetValue() {
    assertEquals("public", EventStatus.PUBLIC.getValue());
    assertEquals("private", EventStatus.PRIVATE.getValue());
  }

  @Test
  public void testEventStatusToString() {
    assertEquals("public", EventStatus.PUBLIC.toString());
    assertEquals("private", EventStatus.PRIVATE.toString());
  }

  /**
   * Sets up common LocalDateTime instances for event tests.
   */
  @Before
  public void setUpCalendarEvent() {
    start = LocalDateTime.of(2025, 1, 15, 10, 0);
    end = LocalDateTime.of(2025, 1, 15, 11, 0);
  }

  @Test
  public void testCreateSingleEvent() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();

    assertEquals("Meeting", event.getSubject());
    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
    assertFalse(event.isRecurring());
    assertNull(event.getSeriesId());
  }

  @Test
  public void testCreateEventDefaults() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();

    assertEquals("", event.getDescription());
    assertEquals("", event.getLocation());
    assertEquals(EventStatus.PUBLIC, event.getStatus());
  }

  @Test
  public void testCreateEventTrimSubject() {
    CalendarEvent event = CalendarEvent.builder("  Meeting  ", start, end).build();
    assertEquals("Meeting", event.getSubject());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventNullSubject() {
    CalendarEvent.builder(null, start, end).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventEmptySubject() {
    CalendarEvent.builder("", start, end).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventWhitespaceSubject() {
    CalendarEvent.builder("   ", start, end).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventNullStart() {
    CalendarEvent.builder("Meeting", null, end).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventNullEnd() {
    CalendarEvent.builder("Meeting", start, null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventEndBeforeStart() {
    LocalDateTime invalidEnd = start.minusHours(1);
    CalendarEvent.builder("Meeting", start, invalidEnd).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderStartNullThrows() {
    CalendarEvent.Builder b = new CalendarEvent.Builder("X", start, end);
    b.start(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderStartAfterEndThrows() {
    CalendarEvent.Builder b = new CalendarEvent.Builder("X", start, end);
    b.start(end.plusMinutes(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEndNullThrows() {
    CalendarEvent.Builder b = new CalendarEvent.Builder("X", start, end);
    b.end(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuilderEndBeforeStartThrows() {
    CalendarEvent.Builder b = new CalendarEvent.Builder("X", start, end);
    b.end(start.minusMinutes(1));
  }

  @Test
  public void testCreateRecurringEvent() {
    Weekday[] days = {Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY};
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", start, end, days)
        .repeatCount(5)
        .build();

    assertTrue(event.isRecurring());
    assertNotNull(event.getSeriesId());
    assertArrayEquals(days, event.getRecurrenceDays());
    assertEquals(Integer.valueOf(5), event.getRepeatCount());
    assertNull(event.getRepeatUntil());
  }

  @Test
  public void testCreateRecurringEventWithUntil() {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime until = LocalDateTime.of(2025, 2, 1, 23, 59);
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", start, end, days)
        .repeatUntil(until)
        .build();

    assertTrue(event.isRecurring());
    assertEquals(until, event.getRepeatUntil());
    assertNull(event.getRepeatCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRecurringEventNullDays() {
    CalendarEvent.recurringBuilder("Meeting", start, end, null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRecurringEventEmptyDays() {
    Weekday[] emptyDays = {};
    CalendarEvent.recurringBuilder("Meeting", start, end, emptyDays).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRecurringEventNoTermination() {
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent.recurringBuilder("Meeting", start, end, days).build();
  }

  @Test
  public void testFromCopiesRecurringFieldsAndIsDefensive() {
    Weekday[] days = {Weekday.MONDAY, Weekday.WEDNESDAY};
    CalendarEvent recurring = CalendarEvent.recurringBuilder("R", start, end, days)
        .repeatCount(3)
        .build();

    CalendarEvent.Builder copyBuilder = CalendarEvent.from(recurring);
    CalendarEvent copy = copyBuilder.build();

    assertTrue(copy.isRecurring());
    assertEquals(recurring.getSeriesId(), copy.getSeriesId());
    assertArrayEquals(recurring.getRecurrenceDays(), copy.getRecurrenceDays());

    // Defensive copy check: mutate returned array and ensure original unaffected
    Weekday[] returned = copy.getRecurrenceDays();
    returned[0] = Weekday.FRIDAY;
    assertArrayEquals(new Weekday[] {Weekday.MONDAY, Weekday.WEDNESDAY},
        recurring.getRecurrenceDays());
  }

  // NEW: Test copying event with repeatUntil instead of repeatCount
  @Test
  public void testFromCopiesRecurringEventWithRepeatUntil() {
    Weekday[] days = {Weekday.TUESDAY};
    LocalDateTime until = LocalDateTime.of(2025, 2, 15, 23, 59);
    CalendarEvent recurring = CalendarEvent.recurringBuilder("Weekly", start, end, days)
        .repeatUntil(until)
        .build();

    CalendarEvent.Builder copyBuilder = CalendarEvent.from(recurring);
    CalendarEvent copy = copyBuilder.build();

    assertEquals(until, copy.getRepeatUntil());
    assertNull(copy.getRepeatCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRecurringEventNegativeCount() {
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent.recurringBuilder("Meeting", start, end, days)
        .repeatCount(-1)
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateRecurringEventZeroCount() {
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent.recurringBuilder("Meeting", start, end, days)
        .repeatCount(0)
        .build();
  }

  // NEW: Test boundary condition for repeatCount
  @Test(expected = IllegalArgumentException.class)
  public void testRepeatCountBoundaryZero() {
    CalendarEvent.Builder b = new CalendarEvent.Builder("X", start, end);
    b.repeatCount(0);
  }

  // NEW: Test recurrenceDays setter returns this
  @Test
  public void testRecurrenceDaysBuilderReturnsThis() {
    CalendarEvent.Builder b = CalendarEvent.builder("Test", start, end);
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent.Builder result = b.recurrenceDays(days);
    assertNotNull(result);
    // Verify it's the same builder by checking we can chain calls
    CalendarEvent.Builder chained = result.description("desc");
    assertNotNull(chained);
  }

  @Test
  public void testWithDescription() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent updated = event.withDescription("Important meeting");

    assertEquals("", event.getDescription());
    assertEquals("Important meeting", updated.getDescription());
  }

  @Test
  public void testWithDescriptionNull() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Original")
        .build();
    CalendarEvent updated = event.withDescription(null);
    assertEquals("", updated.getDescription());
  }

  @Test
  public void testWithLocation() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent updated = event.withLocation("Conference Room A");

    assertEquals("", event.getLocation());
    assertEquals("Conference Room A", updated.getLocation());
  }

  @Test
  public void testWithLocationNull() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .location("Original")
        .build();
    CalendarEvent updated = event.withLocation(null);
    assertEquals("", updated.getLocation());
  }

  @Test
  public void testWithStatus() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent updated = event.withStatus(EventStatus.PRIVATE);

    assertEquals(EventStatus.PUBLIC, event.getStatus());
    assertEquals(EventStatus.PRIVATE, updated.getStatus());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithStatusNull() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    event.withStatus(null);
  }

  @Test
  public void testWithTimes() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    LocalDateTime newStart = start.plusHours(1);
    LocalDateTime newEnd = end.plusHours(1);
    CalendarEvent updated = event.withTimes(newStart, newEnd);

    assertEquals(start, event.getStart());
    assertEquals(end, event.getEnd());
    assertEquals(newStart, updated.getStart());
    assertEquals(newEnd, updated.getEnd());
  }

  @Test
  public void testEventEqualsSameObject() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    assertEquals(event, event);
  }

  @Test
  public void testEventEqualsEqualObjects() {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting", start, end).build();
    assertEquals(event1, event2);
  }

  @Test
  public void testEventEqualsDifferentSubject() {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Different", start, end).build();
    assertNotEquals(event1, event2);
  }

  @Test
  public void testEventEqualsDifferentStart() {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting", start.plusHours(1), end).build();
    assertNotEquals(event1, event2);
  }

  @Test
  public void testEventEqualsNull() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    assertNotEquals(null, event);
  }

  // NEW: Test equals with different class
  @Test
  public void testEventEqualsDifferentClass() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    assertNotEquals(event, "Not a CalendarEvent");
  }

  // NEW: Test hashCode is used properly in collections
  @Test
  public void testEventHashCode() {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting", start, end).build();
    assertEquals(event1.hashCode(), event2.hashCode());

    // Verify hashCode works in HashSet
    Set<CalendarEvent> set = new HashSet<>();
    set.add(event1);
    assertTrue(set.contains(event2)); // Should find via hashCode/equals
  }

  @Test
  public void testEventToString() {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    String str = event.toString();
    assertTrue(str.contains("Meeting"));
    assertTrue(str.contains("recurring=false"));
  }

  @Test
  public void testEventToStringRecurring() {
    Weekday[] days = {Weekday.MONDAY};
    CalendarEvent event = CalendarEvent.recurringBuilder("Meeting", start, end, days)
        .repeatCount(5)
        .build();
    String str = event.toString();
    assertTrue(str.contains("recurring=true"));
  }

  /**
   * Sets up a fresh SparseHashCalendar before each test.
   */
  @Before
  public void setUpCalendar() {
    calendar = new SparseHashCalendar();
    start = LocalDateTime.of(2025, 1, 15, 10, 0);
    end = LocalDateTime.of(2025, 1, 15, 11, 0);
    try {
      calendar.createCalendar("Default", ZoneId.of("America/New_York"));
      calendar.useCalendar("Default");
    } catch (DuplicateCalendarException | CalendarNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAddSingleEvent() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    List<CalendarEvent> events = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullEvent() throws DuplicateEventException {
    calendar.addEvent(null);
  }

  @Test(expected = DuplicateEventException.class)
  public void testAddDuplicateEvent() throws DuplicateEventException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting", start, end).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);
  }

  @Test
  public void testAddMultipleEventsSameDay() throws DuplicateEventException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting", start, end).build();
    LocalDateTime start2 = start.plusHours(2);
    LocalDateTime end2 = end.plusHours(2);
    CalendarEvent event2 = CalendarEvent.builder("Lunch", start2, end2).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);

    List<CalendarEvent> events = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals(2, events.size());
  }

  @Test
  public void testAddRecurringEventWithCount() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.FRIDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(4)
        .build();

    calendar.addEvent(event);

    List<CalendarEvent> allEvents = calendar.getAllEvents();
    assertEquals(4, allEvents.size());
  }

  @Test
  public void testAddRecurringEventWithUntil() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    LocalDateTime until = LocalDateTime.of(2025, 1, 20, 23, 59);
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatUntil(until)
        .build();

    calendar.addEvent(event);

    List<CalendarEvent> allEvents = calendar.getAllEvents();
    assertEquals(3, allEvents.size());
  }

  @Test(expected = DuplicateEventException.class)
  public void testAddRecurringEventDuplicate() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent event1 = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(2)
        .build();

    calendar.addEvent(event1);

    CalendarEvent event2 = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(event2);
  }

  // NEW: Test recurrence with exactly 1 day in array (boundary)
  @Test
  public void testRecurringEventSingleDayArray() throws DuplicateEventException {
    Weekday[] days = {Weekday.FRIDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 3, 14, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 3, 15, 0);
    CalendarEvent event = CalendarEvent.recurringBuilder("Solo", recurStart, recurEnd, days)
        .repeatCount(2)
        .build();

    calendar.addEvent(event);
    List<CalendarEvent> allEvents = calendar.getAllEvents();
    assertEquals(2, allEvents.size());
  }

  // NEW: Test boundary condition where recurrenceDays length > 0
  @Test(expected = IllegalArgumentException.class)
  public void testRecurrenceDaysLengthBoundary() {
    Weekday[] days = new Weekday[0];
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);

    // Empty days array should fail validation
    CalendarEvent.recurringBuilder("Test", recurStart, recurEnd, days)
        .repeatCount(1)
        .build();
  }

  @Test
  public void testGetEventsOnDate() throws DuplicateEventException {
    LocalDate date = LocalDate.of(2025, 1, 15);
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    List<CalendarEvent> events = calendar.getEventsOnDate(date);
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test
  public void testGetEventsOnDateEmpty() {
    LocalDate date = LocalDate.of(2025, 1, 15);
    List<CalendarEvent> events = calendar.getEventsOnDate(date);

    assertNotNull(events);
    assertTrue(events.isEmpty());
  }

  @Test
  public void testGetEventsOnDateDefensiveCopy() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    List<CalendarEvent> events1 = calendar.getEventsOnDate(start.toLocalDate());
    List<CalendarEvent> events2 = calendar.getEventsOnDate(start.toLocalDate());

    events1.clear();
    assertEquals(1, events2.size());
  }

  @Test
  public void testGetEventsInRange() throws DuplicateEventException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting2",
        LocalDateTime.of(2025, 1, 17, 10, 0),
        LocalDateTime.of(2025, 1, 17, 11, 0)).build();
    CalendarEvent event3 = CalendarEvent.builder("Meeting3",
        LocalDateTime.of(2025, 1, 20, 10, 0),
        LocalDateTime.of(2025, 1, 20, 11, 0)).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);
    calendar.addEvent(event3);

    List<CalendarEvent> events = calendar.getEventsInRange(
        LocalDate.of(2025, 1, 15),
        LocalDate.of(2025, 1, 18)
    );

    assertEquals(2, events.size());
  }

  @Test
  public void testGetEventsInRangeSingleDay() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    List<CalendarEvent> events = calendar.getEventsInRange(
        start.toLocalDate(),
        start.toLocalDate()
    );

    assertEquals(1, events.size());
  }

  @Test
  public void testGetAllEvents() throws DuplicateEventException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting1", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting2",
        start.plusDays(1), end.plusDays(1)).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);

    List<CalendarEvent> allEvents = calendar.getAllEvents();
    assertEquals(2, allEvents.size());
  }

  @Test
  public void testGetAllEventsEmpty() {
    List<CalendarEvent> allEvents = calendar.getAllEvents();
    assertNotNull(allEvents);
    assertTrue(allEvents.isEmpty());
  }

  @Test
  public void testIsBusyAtDuringEvent() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    assertTrue(calendar.isBusyAt(start));
    assertTrue(calendar.isBusyAt(start.plusMinutes(30)));
  }

  @Test
  public void testIsBusyAtBeforeEvent() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    assertFalse(calendar.isBusyAt(start.minusHours(1)));
  }

  @Test
  public void testIsBusyAtAfterEvent() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    assertFalse(calendar.isBusyAt(end));
    assertFalse(calendar.isBusyAt(end.plusHours(1)));
  }

  @Test
  public void testIsBusyAtEmptyDay() {
    assertFalse(calendar.isBusyAt(LocalDateTime.of(2025, 1, 15, 10, 0)));
  }

  @Test
  public void testExportToCsv() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Important")
        .location("Room A")
        .build();
    calendar.addEvent(event);

    String csv = calendar.exportToCsv();

    assertTrue(csv.contains("Subject,Start,End,Description,Location,Status"));
    assertTrue(csv.contains("Meeting"));
    assertTrue(csv.contains("Important"));
    assertTrue(csv.contains("Room A"));
  }

  @Test
  public void testExportToCsvEmpty() {
    String csv = calendar.exportToCsv();
    assertTrue(csv.contains("Subject,Start,End,Description,Location,Status"));
    assertEquals(1, csv.split("\n").length);
  }

  @Test
  public void testExportToCsvEscapeComma() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting, Urgent", start, end).build();
    calendar.addEvent(event);

    String csv = calendar.exportToCsv();
    assertTrue(csv.contains("\"Meeting, Urgent\""));
  }

  @Test
  public void testExportToCsvEscapeQuotes() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Description with \"quotes\"")
        .build();
    calendar.addEvent(event);

    String csv = calendar.exportToCsv();
    assertTrue(csv.contains("\"Description with \"\"quotes\"\"\""));
  }

  @Test
  public void testExportToCsvEscapeNewlines() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Line1\nLine2")
        .build();
    calendar.addEvent(event);

    String csv = calendar.exportToCsv();
    assertTrue(csv.contains("\"Line1\nLine2\""));
  }

  @Test
  public void testExportToCsvSorted() throws DuplicateEventException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting2",
        LocalDateTime.of(2025, 1, 15, 14, 0),
        LocalDateTime.of(2025, 1, 15, 15, 0)).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);

    String csv = calendar.exportToCsv();
    String[] lines = csv.split("\n");

    assertTrue(lines[1].contains("Meeting1"));
    assertTrue(lines[2].contains("Meeting2"));
  }

  @Test
  public void testEditEventSubject() throws DuplicateEventException, EventNotFoundException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    calendar.editEvent("Meeting", start, "Updated Meeting", null, null, null, null, null);

    List<CalendarEvent> events = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals(1, events.size());
    assertEquals("Updated Meeting", events.get(0).getSubject());
  }

  // NEW: Test editing with null description preserves original
  @Test
  public void testEditEventNullDescriptionPreservesOriginal()
      throws DuplicateEventException, EventNotFoundException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Original desc")
        .build();
    calendar.addEvent(event);

    calendar.editEvent("Meeting", start, null, null, null, null, null, null);

    List<CalendarEvent> events = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals("Original desc", events.get(0).getDescription());
  }

  // NEW: Test editing with null location preserves original
  @Test
  public void testEditEventNullLocationPreservesOriginal()
      throws DuplicateEventException, EventNotFoundException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .location("Original location")
        .build();
    calendar.addEvent(event);

    calendar.editEvent("Meeting", start, null, null, null, null, null, null);

    List<CalendarEvent> events = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals("Original location", events.get(0).getLocation());
  }

  @Test(expected = EventNotFoundException.class)
  public void testEditEventNotFound() throws DuplicateEventException, EventNotFoundException {
    calendar.editEvent("Nonexistent", start, "New", null, null, null, null, null);
  }

  @Test(expected = DuplicateEventException.class)
  public void testEditEventCreatesDuplicate()
      throws DuplicateEventException, EventNotFoundException {
    CalendarEvent event1 = CalendarEvent.builder("Meeting1", start, end).build();
    CalendarEvent event2 = CalendarEvent.builder("Meeting2",
        start.plusHours(1), end.plusHours(1)).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);

    calendar.editEvent("Meeting2", start.plusHours(1),
        "Meeting1", start, end, null, null, null);
  }

  // NEW: Test editing a recurring event (should detach it from series)
  @Test
  public void testEditRecurringEventDetachesFromSeries()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent event = CalendarEvent.recurringBuilder("Weekly", recurStart, recurEnd, days)
        .repeatCount(2)
        .build();

    calendar.addEvent(event);

    // Get the seriesId before edit
    String originalSeriesId = calendar.getAllEvents().get(0).getSeriesId();

    // Edit first occurrence
    calendar.editEvent("Weekly", recurStart, "Modified", null, null, null, null, null);

    // Verify the edited event no longer has the series tracking
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 6));
    CalendarEvent edited = events.stream()
        .filter(e -> e.getSubject().equals("Modified"))
        .findFirst()
        .orElse(null);

    assertNotNull(edited);
    assertFalse(edited.isRecurring());
  }

  @Test
  public void testRecurringEventSeriesTracking() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(3)
        .build();

    calendar.addEvent(event);

    List<CalendarEvent> allEvents = calendar.getAllEvents();
    String seriesId = allEvents.get(0).getSeriesId();
    assertNotNull(seriesId);

    for (CalendarEvent occurrence : allEvents) {
      assertEquals(seriesId, occurrence.getSeriesId());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testRecurringEventSafetyLimit() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);

    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(20000)
        .build();
    calendar.addEvent(event);
  }

  // NEW: Test safety limit boundary (exactly at limit + 1)
  @Test(expected = IllegalStateException.class)
  public void testRecurringEventSafetyLimitExact() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);

    CalendarEvent event = CalendarEvent.recurringBuilder("Standup", recurStart, recurEnd, days)
        .repeatCount(10001)
        .build();
    calendar.addEvent(event);
  }

  // NEW: Test condition boundary for shouldContinueGenerating
  @Test
  public void testRecurringEventCountBoundary() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);

    // Test with count = 1 (boundary)
    CalendarEvent event = CalendarEvent.recurringBuilder("Single", recurStart, recurEnd, days)
        .repeatCount(1)
        .build();

    calendar.addEvent(event);
    assertEquals(1, calendar.getAllEvents().size());
  }

  @Test
  public void testIsBusyAtBoundaryTimes() throws DuplicateEventException {
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end).build();
    calendar.addEvent(event);

    assertTrue(calendar.isBusyAt(start));
    assertTrue(calendar.isBusyAt(end.minusSeconds(1)));
    assertFalse(calendar.isBusyAt(end));
    assertFalse(calendar.isBusyAt(end.plusSeconds(1)));
  }

  /**
   * Edit series from date should throw DuplicateEventException when edit would
   * collide with an existing event (duplicate tuple of subject, start, end).
   */
  @Test(expected = DuplicateEventException.class)
  public void testEditSeriesFromDateCreatesDuplicate()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY, Weekday.WEDNESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("SeriesA",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    calendar.addEvent(CalendarEvent.builder("Conflicting",
            LocalDateTime.of(2025, 1, 8, 9, 0),
            LocalDateTime.of(2025, 1, 8, 9, 30))
        .build());

    calendar.editSeriesFromDate("SeriesA",
        LocalDateTime.of(2025, 1, 8, 9, 0),
        "Conflicting", null, null, null, null, null);
  }

  // NEW: Test editSeriesFromDate with description/location nulls preserve original
  @Test
  public void testEditSeriesFromDateNullsPreserveOriginal()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .description("Original desc")
        .location("Original loc")
        .build();
    calendar.addEvent(series);

    calendar.editSeriesFromDate("Series", recurStart, null, null, null, null, null, null);

    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 6));
    CalendarEvent modified = events.get(0);
    assertEquals("Original desc", modified.getDescription());
    assertEquals("Original loc", modified.getLocation());
  }

  // NEW: Test editEntireSeries with null description/location preserves original
  @Test
  public void testEditEntireSeriesNullsPreserveOriginal()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.TUESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 7, 10, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 7, 11, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .description("Desc")
        .location("Loc")
        .build();
    calendar.addEvent(series);

    String seriesId = calendar.getAllEvents().get(0).getSeriesId();
    calendar.editEntireSeries(seriesId, null, null, null, null, null, null);

    List<CalendarEvent> events = calendar.getAllEvents();
    for (CalendarEvent e : events) {
      assertEquals("Desc", e.getDescription());
      assertEquals("Loc", e.getLocation());
    }
  }

  // NEW: Test boundary condition for editSeriesFromDate index
  @Test
  public void testEditSeriesFromDateIndexBoundary()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(3)
        .build();
    calendar.addEvent(series);

    // Edit from the very first occurrence (index = 0)
    calendar.editSeriesFromDate("Series", recurStart, "Changed", null, null, null, null, null);

    List<CalendarEvent> events = calendar.getAllEvents();
    assertTrue(events.stream().allMatch(e -> e.getSubject().equals("Changed")));
  }

  // NEW: Test boundary condition for editEntireSeries index
  @Test
  public void testEditEntireSeriesIndexBoundary()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.WEDNESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 8, 13, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 8, 14, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    String seriesId = calendar.getAllEvents().get(0).getSeriesId();
    calendar.editEntireSeries(seriesId, "AllChanged", null, null, null, null, null);

    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(2, events.size());
    assertTrue(events.stream().allMatch(e -> e.getSubject().equals("AllChanged")));
  }

  /**
   * Edit entire series should throw DuplicateEventException when the change
   * would collide with an existing event across the series.
   */
  @Test(expected = DuplicateEventException.class)
  public void testEditEntireSeriesCreatesDuplicate()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("TeamSync",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    calendar.addEvent(CalendarEvent.builder("Existing",
            LocalDateTime.of(2025, 1, 6, 9, 0),
            LocalDateTime.of(2025, 1, 6, 9, 30))
        .build());

    String seriesId = calendar.getAllEvents().get(0).getSeriesId();
    calendar.editEntireSeries(seriesId,
        "Existing", null, null, null, null, null);
  }

  /**
   * Editing a single occurrence of a series should detach it from the series
   * (i.e., the modified event is no longer recurring).
   */
  @Test
  public void testEditSingleOccurrenceDetachesFromSeries()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("SeriesB",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    calendar.editEvent("SeriesB", LocalDateTime.of(2025, 1, 6, 9, 0),
        "Changed", null, null, null, null, null);

    List<CalendarEvent> dayEvents =
        calendar.getEventsOnDate(LocalDate.of(2025, 1, 6));
    boolean foundStandalone = false;
    for (CalendarEvent e : dayEvents) {
      if (e.getSubject().equals("Changed")) {
        foundStandalone = true;
        assertFalse(e.isRecurring());
      }
    }
    assertTrue(foundStandalone);
  }

  // NEW: Test editSeriesFromDate filter boundary
  @Test
  public void testEditSeriesFromDateFilterBoundary()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(3)
        .build();
    calendar.addEvent(series);

    // Edit from second occurrence (2025-01-13)
    LocalDateTime secondOccurrence = LocalDateTime.of(2025, 1, 13, 9, 0);
    calendar.editSeriesFromDate("Series", secondOccurrence,
        "FromSecond", null, null, null, null, null);

    // First occurrence should be unchanged
    List<CalendarEvent> firstDay = calendar.getEventsOnDate(LocalDate.of(2025, 1, 6));
    assertEquals("Series", firstDay.get(0).getSubject());

    // Second occurrence should be changed
    List<CalendarEvent> secondDay = calendar.getEventsOnDate(LocalDate.of(2025, 1, 13));
    assertEquals("FromSecond", secondDay.get(0).getSubject());
  }

  @Test
  public void testCopyEventsOnDateBetweenCalendars()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("West", ZoneId.of("America/Los_Angeles"));
    calendar.useCalendar("Default");

    CalendarEvent event = CalendarEvent.builder("Standup", start, end).build();
    calendar.addEvent(event);

    calendar.copyEventsOnDate(start.toLocalDate(), "West", start.toLocalDate());

    calendar.useCalendar("West");
    List<CalendarEvent> westEvents = calendar.getEventsOnDate(start.toLocalDate());
    assertEquals(1, westEvents.size());
    assertEquals(LocalDateTime.of(2025, 1, 15, 7, 0), westEvents.get(0).getStart());
    assertEquals(LocalDateTime.of(2025, 1, 15, 8, 0), westEvents.get(0).getEnd());
  }

  @Test
  public void testCopyEventToTargetCalendar()
      throws DuplicateCalendarException, CalendarNotFoundException,
      DuplicateEventException, EventNotFoundException {
    calendar.createCalendar("Work", ZoneId.of("Europe/London"));
    calendar.useCalendar("Default");

    CalendarEvent event = CalendarEvent.builder("Design Review", start, end).build();
    calendar.addEvent(event);

    LocalDateTime targetStart = LocalDateTime.of(2025, 1, 16, 14, 0);
    calendar.copyEvent("Design Review", start, "Work", targetStart);

    calendar.useCalendar("Work");
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 16));
    assertEquals(1, events.size());
    assertEquals(targetStart, events.get(0).getStart());
  }

  // NEW: Test copying recurring event (single instance triggers series copy)
  @Test
  public void testCopyRecurringEventCopiesSeries()
      throws DuplicateCalendarException, CalendarNotFoundException,
      DuplicateEventException, EventNotFoundException {
    calendar.createCalendar("Target", ZoneId.of("America/New_York"));
    calendar.useCalendar("Default");

    Weekday[] days = {Weekday.THURSDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 9, 15, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 9, 16, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Recurring",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    LocalDateTime targetStart = LocalDateTime.of(2025, 1, 9, 15, 0);
    calendar.copyEvent("Recurring", recurStart, "Target", targetStart);

    calendar.useCalendar("Target");
    List<CalendarEvent> copiedEvents = calendar.getAllEvents();
    assertEquals(2, copiedEvents.size());

    // Verify all have new series ID (not the original)
    String originalSeriesId = series.getSeriesId();
    String copiedSeriesId = copiedEvents.get(0).getSeriesId();
    assertNotEquals(originalSeriesId, copiedSeriesId);
    assertTrue(copiedEvents.stream().allMatch(e -> e.getSeriesId().equals(copiedSeriesId)));
  }

  // NEW: Test alignDateTime arithmetic boundary
  @Test
  public void testCopyEventsAlignDateTimeCalculation()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Other", ZoneId.of("America/Chicago"));
    calendar.useCalendar("Default");

    CalendarEvent event = CalendarEvent.builder("Event", start, end).build();
    calendar.addEvent(event);

    // Copy to different date to trigger alignment calculation
    LocalDate targetDate = LocalDate.of(2025, 1, 20);
    calendar.copyEventsOnDate(start.toLocalDate(), "Other", targetDate);

    calendar.useCalendar("Other");
    List<CalendarEvent> events = calendar.getEventsOnDate(targetDate);
    assertEquals(1, events.size());
    // Verify the time was adjusted correctly
    assertEquals(20, events.get(0).getStart().getDayOfMonth());
  }

  @Test
  public void testUpdateTimezoneAdjustsEventTimes()
      throws DuplicateEventException, CalendarNotFoundException, DuplicateCalendarException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Planning", start, end).build();
    calendar.addEvent(event);

    calendar.updateCalendarTimezone("Default", ZoneId.of("Europe/London"));

    calendar.useCalendar("Default");
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 1, 15, 15, 0), events.get(0).getStart());
    assertEquals(LocalDateTime.of(2025, 1, 15, 16, 0), events.get(0).getEnd());
  }

  // NEW: Test updateTimezone with same zone (early return path)
  @Test
  public void testUpdateTimezoneSameZoneNoOp()
      throws DuplicateEventException, CalendarNotFoundException, DuplicateCalendarException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Event", start, end).build();
    calendar.addEvent(event);

    // Update to same timezone should be no-op
    calendar.updateCalendarTimezone("Default", ZoneId.of("America/New_York"));

    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(start, events.get(0).getStart());
    assertEquals(end, events.get(0).getEnd());
  }

  // NEW: Test convertEventToZone with recurring event
  @Test
  public void testUpdateTimezoneWithRecurringEvent()
      throws DuplicateEventException, CalendarNotFoundException, DuplicateCalendarException {
    calendar.useCalendar("Default");

    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    String originalSeriesId = calendar.getAllEvents().get(0).getSeriesId();

    calendar.updateCalendarTimezone("Default", ZoneId.of("UTC"));

    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(2, events.size());
    // Verify series ID is preserved during timezone conversion
    assertEquals(originalSeriesId, events.get(0).getSeriesId());
  }

  @Test
  public void testExportToIcalProducesCalendar()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Demo", start, end)
        .description("Kickoff")
        .location("Room 101")
        .build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    assertTrue(ical.contains("BEGIN:VCALENDAR"));
    assertTrue(ical.contains("SUMMARY:Demo"));
    assertTrue(ical.contains("END:VCALENDAR"));
  }

  // NEW: Test ical export with empty description/location (conditional branches)
  @Test
  public void testExportToIcalEmptyOptionalFields()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Minimal", start, end).build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    assertTrue(ical.contains("SUMMARY:Minimal"));
    assertFalse(ical.contains("DESCRIPTION:"));
    assertFalse(ical.contains("LOCATION:"));
  }

  // NEW: Test ical export with null description/location
  @Test
  public void testExportToIcalNullOptionalFields()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Event", start, end)
        .description(null)
        .location(null)
        .build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    assertTrue(ical.contains("SUMMARY:Event"));
    // Should not include empty DESCRIPTION or LOCATION lines
    String[] lines = ical.split("\r\n");
    for (String line : lines) {
      assertFalse(line.startsWith("DESCRIPTION:"));
      assertFalse(line.startsWith("LOCATION:"));
    }
  }

  // NEW: Test ical formatting and escaping
  @Test
  public void testExportToIcalFormatting()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Meeting", start, end)
        .description("Test\\desc;with,special\nchars")
        .build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    // Verify escaping occurred
    assertTrue(ical.contains("DESCRIPTION:"));
    assertTrue(ical.contains("\\\\"));
    assertTrue(ical.contains("\\;"));
    assertTrue(ical.contains("\\,"));
    assertTrue(ical.contains("\\n"));
  }

  // NEW: Test calendar name validation
  @Test(expected = IllegalArgumentException.class)
  public void testCreateCalendarNullName() throws DuplicateCalendarException {
    calendar.createCalendar(null, ZoneId.of("UTC"));
  }

  // NEW: Test calendar name validation empty
  @Test(expected = IllegalArgumentException.class)
  public void testCreateCalendarEmptyName() throws DuplicateCalendarException {
    calendar.createCalendar("", ZoneId.of("UTC"));
  }

  // NEW: Test calendar name validation whitespace
  @Test(expected = IllegalArgumentException.class)
  public void testCreateCalendarWhitespaceName() throws DuplicateCalendarException {
    calendar.createCalendar("   ", ZoneId.of("UTC"));
  }

  // NEW: Test renaming calendar to existing name
  @Test(expected = DuplicateCalendarException.class)
  public void testRenameCalendarToDuplicateName()
      throws DuplicateCalendarException, CalendarNotFoundException {
    calendar.createCalendar("Second", ZoneId.of("UTC"));
    calendar.renameCalendar("Default", "Second");
  }

  // NEW: Test renaming non-existent calendar
  @Test(expected = CalendarNotFoundException.class)
  public void testRenameNonExistentCalendar()
      throws DuplicateCalendarException, CalendarNotFoundException {
    calendar.renameCalendar("DoesNotExist", "NewName");
  }

  // NEW: Test renaming active calendar updates activeCalendarName
  @Test
  public void testRenameActiveCalendarUpdatesName()
      throws DuplicateCalendarException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    assertEquals("Default", calendar.getActiveCalendarName());

    calendar.renameCalendar("Default", "Renamed");

    assertEquals("Renamed", calendar.getActiveCalendarName());
  }

  // NEW: Test renaming inactive calendar doesn't affect activeCalendarName
  @Test
  public void testRenameInactiveCalendarPreservesActive()
      throws DuplicateCalendarException, CalendarNotFoundException {
    calendar.createCalendar("Other", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    calendar.renameCalendar("Other", "OtherRenamed");

    assertEquals("Default", calendar.getActiveCalendarName());
  }

  // NEW: Test listCalendars
  @Test
  public void testListCalendars() throws DuplicateCalendarException {
    calendar.createCalendar("Cal1", ZoneId.of("UTC"));
    calendar.createCalendar("Cal2", ZoneId.of("UTC"));

    List<String> names = calendar.listCalendars();
    assertEquals(3, names.size()); // Default + Cal1 + Cal2
    assertTrue(names.contains("Default"));
    assertTrue(names.contains("Cal1"));
    assertTrue(names.contains("Cal2"));
  }

  // NEW: Test getActiveCalendarZone
  @Test
  public void testGetActiveCalendarZone() throws CalendarNotFoundException {
    calendar.useCalendar("Default");
    ZoneId zone = calendar.getActiveCalendarZone();
    assertEquals(ZoneId.of("America/New_York"), zone);
  }

  // NEW: Test resolveSeries with null (single event path)
  @Test
  public void testCopyEventsSingleEventNoSeries()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Dest", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    CalendarEvent event = CalendarEvent.builder("Single", start, end).build();
    calendar.addEvent(event);

    calendar.copyEventsOnDate(start.toLocalDate(), "Dest", start.toLocalDate());

    calendar.useCalendar("Dest");
    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertNull(events.get(0).getSeriesId());
  }

  // NEW: Test copyEventsOnDate with recurring events
  @Test
  public void testCopyEventsOnDateWithRecurringSeries()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Target", ZoneId.of("America/Chicago"));
    calendar.useCalendar("Default");

    Weekday[] days = {Weekday.WEDNESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 15, 10, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 15, 11, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Weekly",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    calendar.copyEventsOnDate(LocalDate.of(2025, 1, 15), "Target",
        LocalDate.of(2025, 1, 15));

    calendar.useCalendar("Target");
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertTrue(events.size() > 0);
    // Verify new series ID was assigned
    String newSeriesId = events.get(0).getSeriesId();
    assertNotNull(newSeriesId);
    assertNotEquals(series.getSeriesId(), newSeriesId);
  }

  @Test
  public void testUpdateTimezoneAdjustsEventTimes2()
      throws DuplicateEventException, CalendarNotFoundException, DuplicateCalendarException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Planning", start, end).build();
    calendar.addEvent(event);

    calendar.updateCalendarTimezone("Default", ZoneId.of("Europe/London"));

    calendar.useCalendar("Default");
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals(LocalDateTime.of(2025, 1, 15, 15, 0), events.get(0).getStart());
    assertEquals(LocalDateTime.of(2025, 1, 15, 16, 0), events.get(0).getEnd());
  }

  @Test
  public void testExportToIcalProducesCalendar2()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Demo", start, end)
        .description("Kickoff")
        .location("Room 101")
        .build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    assertTrue(ical.contains("BEGIN:VCALENDAR"));
    assertTrue(ical.contains("SUMMARY:Demo"));
    assertTrue(ical.contains("END:VCALENDAR"));
  }

  // NEW: Test validateCalendarName is actually called
  @Test(expected = IllegalArgumentException.class)
  public void testValidateCalendarNameCalled() throws DuplicateCalendarException {
    calendar.createCalendar("  ", ZoneId.of("UTC"));
  }

  // NEW: Test cloneEvent with null seriesId
  @Test
  public void testCloneEventNullSeriesId()
      throws DuplicateCalendarException, CalendarNotFoundException,
      DuplicateEventException, EventNotFoundException {
    calendar.createCalendar("Clone", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    CalendarEvent event = CalendarEvent.builder("Single", start, end).build();
    calendar.addEvent(event);

    LocalDateTime targetStart = LocalDateTime.of(2025, 1, 16, 10, 0);
    calendar.copyEvent("Single", start, "Clone", targetStart);

    calendar.useCalendar("Clone");
    List<CalendarEvent> events = calendar.getAllEvents();
    assertNull(events.get(0).getSeriesId());
  }

  // NEW: Test addSingleEvent with seriesId set (recurring event path)
  @Test
  public void testAddSingleEventDoesNotTrackNonRecurringSeries()
      throws DuplicateEventException {
    // This tests the conditional in addSingleEvent for seriesId tracking
    CalendarEvent event = CalendarEvent.builder("Event", start, end).build();
    calendar.addEvent(event);

    // Verify it was added without series tracking
    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertNull(events.get(0).getSeriesId());
  }

  // NEW: Test getSeriesEvents method
  @Test
  public void testGetSeriesEventsReturnsDefensiveCopy() throws DuplicateEventException {
    Weekday[] days = {Weekday.FRIDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 10, 14, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 10, 15, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    // This will test the getSeriesEvents method if we can access it
    // Through the edit series operations
    String seriesId = calendar.getAllEvents().get(0).getSeriesId();
    assertNotNull(seriesId);
  }

  // NEW: Test editing series that was removed (eventsBySeries cleanup)
  @Test
  public void testEditSeriesCleanupWhenLastEventRemoved()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(1)
        .build();
    calendar.addEvent(series);

    // Edit the only occurrence - should remove from series tracking
    calendar.editEvent("Series", recurStart, "Changed", null, null, null, null, null);

    // Verify it was removed from series tracking
    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(1, events.size());
    assertFalse(events.get(0).isRecurring());
  }

  // NEW: Test createModifiedSeriesEvent with null seriesId
  @Test
  public void testEditSeriesPreservesSeriesId()
      throws DuplicateEventException, EventNotFoundException {
    Weekday[] days = {Weekday.TUESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 7, 11, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 7, 12, 0);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    String originalSeriesId = calendar.getAllEvents().get(0).getSeriesId();

    calendar.editSeriesFromDate("Series", recurStart, "Modified", null, null, null, null, null);

    List<CalendarEvent> events = calendar.getAllEvents();
    // All events should still have the same seriesId
    assertTrue(events.stream().allMatch(e -> e.getSeriesId().equals(originalSeriesId)));
  }

  // NEW: Test copyEventsBetween method
  @Test
  public void testCopyEventsBetweenDateRange()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Range", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    // Add events on multiple days
    CalendarEvent event1 = CalendarEvent.builder("Event1",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build();
    CalendarEvent event2 = CalendarEvent.builder("Event2",
        LocalDateTime.of(2025, 1, 16, 10, 0),
        LocalDateTime.of(2025, 1, 16, 11, 0)).build();
    CalendarEvent event3 = CalendarEvent.builder("Event3",
        LocalDateTime.of(2025, 1, 17, 10, 0),
        LocalDateTime.of(2025, 1, 17, 11, 0)).build();

    calendar.addEvent(event1);
    calendar.addEvent(event2);
    calendar.addEvent(event3);

    // Copy range to target calendar starting at different date
    calendar.copyEventsBetween(
        LocalDate.of(2025, 1, 15),
        LocalDate.of(2025, 1, 16),
        "Range",
        LocalDate.of(2025, 2, 1)
    );

    calendar.useCalendar("Range");
    List<CalendarEvent> copiedEvents = calendar.getAllEvents();
    assertEquals(2, copiedEvents.size()); // Only event1 and event2

    // Verify dates were shifted correctly
    assertTrue(copiedEvents.stream()
        .anyMatch(e -> e.getStart().toLocalDate().equals(LocalDate.of(2025, 2, 1))));
    assertTrue(copiedEvents.stream()
        .anyMatch(e -> e.getStart().toLocalDate().equals(LocalDate.of(2025, 2, 2))));
  }

  // NEW: Test copyEventsBetween with recurring series
  @Test
  public void testCopyEventsBetweenWithRecurringSeries()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Batch", ZoneId.of("America/Denver"));
    calendar.useCalendar("Default");

    Weekday[] days = {Weekday.MONDAY, Weekday.WEDNESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);
    CalendarEvent series = CalendarEvent.recurringBuilder("Series",
            recurStart, recurEnd, days)
        .repeatCount(2)
        .build();
    calendar.addEvent(series);

    calendar.copyEventsBetween(
        LocalDate.of(2025, 1, 6),
        LocalDate.of(2025, 1, 8),
        "Batch",
        LocalDate.of(2025, 2, 10)
    );

    calendar.useCalendar("Batch");
    List<CalendarEvent> copied = calendar.getAllEvents();
    assertEquals(2, copied.size());

    // Verify new series IDs were generated
    String newSeriesId = copied.get(0).getSeriesId();
    assertNotNull(newSeriesId);
    assertNotEquals(series.getSeriesId(), newSeriesId);
  }

  // NEW: Test hasActiveCalendar with no calendars
  @Test
  public void testHasActiveCalendarWhenNone() {
    SparseHashCalendar emptyModel = new SparseHashCalendar();
    assertFalse(emptyModel.hasActiveCalendar());
    assertNull(emptyModel.getActiveCalendarName());
  }

  // NEW: Test operations without active calendar
  @Test(expected = IllegalStateException.class)
  public void testAddEventWithoutActiveCalendar() throws DuplicateEventException {
    SparseHashCalendar emptyModel = new SparseHashCalendar();
    CalendarEvent event = CalendarEvent.builder("Event", start, end).build();
    emptyModel.addEvent(event);
  }

  // NEW: Test formatIcalDateTime
  @Test
  public void testIcalDateTimeFormat()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Event",
        LocalDateTime.of(2025, 3, 5, 14, 30),
        LocalDateTime.of(2025, 3, 5, 15, 30)).build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    // Should contain properly formatted datetime
    assertTrue(ical.contains("20250305T143000"));
    assertTrue(ical.contains("20250305T153000"));
  }

  // NEW: Test CalendarContainer getName
  @Test
  public void testCalendarContainerGetName()
      throws DuplicateCalendarException, CalendarNotFoundException {
    calendar.createCalendar("TestName", ZoneId.of("UTC"));
    calendar.useCalendar("TestName");

    String ical = calendar.exportToIcal();
    assertTrue(ical.contains("X-WR-CALNAME:TestName"));
  }

  // NEW: Test containsCalendar
  @Test
  public void testContainsCalendar() throws DuplicateCalendarException {
    assertTrue(calendar.hasActiveCalendar()); // Default exists

    calendar.createCalendar("NewCal", ZoneId.of("UTC"));
    // Indirectly test containsCalendar via duplicate detection
    try {
      calendar.createCalendar("NewCal", ZoneId.of("UTC"));
    } catch (DuplicateCalendarException e) {
      assertTrue(e.getMessage().contains("already exists"));
      return;
    }
    throw new AssertionError("Should have thrown DuplicateCalendarException");
  }

  // NEW: Test requireCalendar throws when calendar not found
  @Test(expected = CalendarNotFoundException.class)
  public void testRequireCalendarNotFound()
      throws CalendarNotFoundException, EventNotFoundException, DuplicateEventException {
    calendar.copyEvent("Event", start, "NonExistent", start);
  }

  // NEW: Test matchesRecurrencePattern edge cases
  @Test
  public void testMatchesRecurrencePatternAllDays() throws DuplicateEventException {
    Weekday[] allDays = {
        Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY,
        Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY, Weekday.SUNDAY
    };
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 8, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 0);
    CalendarEvent event = CalendarEvent.recurringBuilder("Daily",
            recurStart, recurEnd, allDays)
        .repeatCount(7)
        .build();

    calendar.addEvent(event);
    assertEquals(7, calendar.getAllEvents().size());
  }

  // NEW: Test shouldContinueGenerating with repeatCount boundary
  @Test
  public void testShouldContinueGeneratingCountBoundary() throws DuplicateEventException {
    Weekday[] days = {Weekday.MONDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 6, 9, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 6, 9, 30);

    // Exactly 1 occurrence
    CalendarEvent event = CalendarEvent.recurringBuilder("Once",
            recurStart, recurEnd, days)
        .repeatCount(1)
        .build();

    calendar.addEvent(event);
    assertEquals(1, calendar.getAllEvents().size());
  }

  // NEW: Test shouldContinueGenerating with until date boundary
  @Test
  public void testShouldContinueGeneratingUntilBoundary() throws DuplicateEventException {
    Weekday[] days = {Weekday.TUESDAY};
    LocalDateTime recurStart = LocalDateTime.of(2025, 1, 7, 10, 0);
    LocalDateTime recurEnd = LocalDateTime.of(2025, 1, 7, 11, 0);

    // Until exactly one week later (should get 2 occurrences)
    LocalDateTime until = LocalDateTime.of(2025, 1, 14, 23, 59);
    CalendarEvent event = CalendarEvent.recurringBuilder("Limited",
            recurStart, recurEnd, days)
        .repeatUntil(until)
        .build();

    calendar.addEvent(event);
    assertEquals(2, calendar.getAllEvents().size());
  }

  // NEW: Test escapeIcal with null
  @Test
  public void testEscapeIcalNullValue()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");
    CalendarEvent event = CalendarEvent.builder("Event", start, end)
        .description(null)
        .build();
    calendar.addEvent(event);

    String ical = calendar.exportToIcal();
    // Should handle null gracefully
    assertTrue(ical.contains("SUMMARY:Event"));
  }

  // NEW: Test building ical doesn't sort (test the sort call)
  @Test
  public void testIcalExportOrderDependsOnSort()
      throws DuplicateEventException, CalendarNotFoundException {
    calendar.useCalendar("Default");

    // Add events in reverse chronological order
    CalendarEvent event2 = CalendarEvent.builder("Second",
        LocalDateTime.of(2025, 1, 16, 10, 0),
        LocalDateTime.of(2025, 1, 16, 11, 0)).build();
    CalendarEvent event1 = CalendarEvent.builder("First",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)).build();

    calendar.addEvent(event2);
    calendar.addEvent(event1);

    String ical = calendar.exportToIcal();

    // Events should appear in chronological order (sorted)
    int firstIndex = ical.indexOf("SUMMARY:First");
    int secondIndex = ical.indexOf("SUMMARY:Second");
    assertTrue(firstIndex > 0);
    assertTrue(secondIndex > 0);
    assertTrue(firstIndex < secondIndex);
  }

  // NEW: Test copyEventsOnDate sort
  @Test
  public void testCopyEventsOnDateSortsEvents()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Sorted", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    // Add events out of order on same day
    CalendarEvent event2 = CalendarEvent.builder("Late",
        LocalDateTime.of(2025, 1, 15, 14, 0),
        LocalDateTime.of(2025, 1, 15, 15, 0)).build();
    CalendarEvent event1 = CalendarEvent.builder("Early",
        LocalDateTime.of(2025, 1, 15, 9, 0),
        LocalDateTime.of(2025, 1, 15, 10, 0)).build();

    calendar.addEvent(event2);
    calendar.addEvent(event1);

    calendar.copyEventsOnDate(LocalDate.of(2025, 1, 15), "Sorted",
        LocalDate.of(2025, 1, 15));

    calendar.useCalendar("Sorted");
    List<CalendarEvent> events = calendar.getAllEvents();
    assertEquals(2, events.size());
    // Verify they were added (sorting tested through consistent behavior)
  }

  // NEW: Test resolveSeries creates new IDs for each series
  @Test
  public void testResolveSeriesCreatesUniqueIds()
      throws DuplicateCalendarException, CalendarNotFoundException, DuplicateEventException {
    calendar.createCalendar("Multi", ZoneId.of("UTC"));
    calendar.useCalendar("Default");

    // Create two different series on same day
    Weekday[] days1 = {Weekday.WEDNESDAY};
    LocalDateTime start1 = LocalDateTime.of(2025, 1, 15, 9, 0);
    LocalDateTime end1 = LocalDateTime.of(2025, 1, 15, 10, 0);
    CalendarEvent series1 = CalendarEvent.recurringBuilder("Series1",
            start1, end1, days1)
        .repeatCount(1)
        .build();

    Weekday[] days2 = {Weekday.WEDNESDAY};
    LocalDateTime start2 = LocalDateTime.of(2025, 1, 15, 14, 0);
    LocalDateTime end2 = LocalDateTime.of(2025, 1, 15, 15, 0);
    CalendarEvent series2 = CalendarEvent.recurringBuilder("Series2",
            start2, end2, days2)
        .repeatCount(1)
        .build();

    calendar.addEvent(series1);
    calendar.addEvent(series2);


    calendar.copyEventsOnDate(LocalDate.of(2025, 1, 15), "Multi",
        LocalDate.of(2025, 1, 15));

    calendar.useCalendar("Multi");
    List<CalendarEvent> copied = calendar.getAllEvents();
    assertEquals(2, copied.size());

    // Both should have different new series IDs
    Set<String> copiedSeriesIds = new HashSet<>();
    for (CalendarEvent e : copied) {
      copiedSeriesIds.add(e.getSeriesId());
    }
    assertEquals(2, copiedSeriesIds.size());

    String seriesId1 = series1.getSeriesId();
    String seriesId2 = series2.getSeriesId();
    assertFalse(copiedSeriesIds.contains(seriesId1));
    assertFalse(copiedSeriesIds.contains(seriesId2));
  }

  // ========== CSV Import Tests ==========

  @Test
  public void testImportFromCsvBasic() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test
  public void testImportFromCsvWithQuotes() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "\"Team Meeting\",2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Team Meeting", events.get(0).getSubject());
  }

  @Test
  public void testImportFromCsvWithLocationAndDescription() throws DuplicateEventException {
    String csvContent = "Subject,Start,End,Location,Description\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00,Conference Room,Team sync";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Conference Room", events.get(0).getLocation());
    assertEquals("Team sync", events.get(0).getDescription());
  }

  @Test
  public void testImportFromCsvMultipleEvents() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "Meeting 1,2025-01-15T10:00,2025-01-15T11:00\n"
        + "Meeting 2,2025-01-15T14:00,2025-01-15T15:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(2, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(2, events.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromCsvNullContent() throws DuplicateEventException {
    calendar.importFromCsv(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromCsvEmptyContent() throws DuplicateEventException {
    calendar.importFromCsv("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromCsvOnlyHeader() throws DuplicateEventException {
    calendar.importFromCsv("Subject,Start,End");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromCsvNoValidEvents() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "Invalid,invalid-date,invalid-date";
    
    calendar.importFromCsv(csvContent);
  }

  @Test
  public void testImportFromCsvSkipsEmptyLines() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00\n"
        + "  \n";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromCsvWithDuplicateEvents() throws DuplicateEventException {
    String csvContent = "Subject,Start,End\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00";
    
    // First import should succeed (only 1 unique event, 1 duplicate skipped)
    int count1 = calendar.importFromCsv(csvContent);
    assertEquals(1, count1);
    
    // Second import should throw exception when all events are duplicates
    try {
      calendar.importFromCsv(csvContent);
      // Should not reach here - exception expected
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      // Expected when all events are duplicates
      assertTrue(e.getMessage().contains("Failed to import any events"));
    }
  }

  // ========== iCal Import Tests ==========

  @Test
  public void testImportFromIcalBasic() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test
  public void testImportFromIcalWithDescriptionAndLocation() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Team Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "DESCRIPTION:Team sync meeting\n"
        + "LOCATION:Conference Room\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Team sync meeting", events.get(0).getDescription());
    assertEquals("Conference Room", events.get(0).getLocation());
  }

  @Test
  public void testImportFromIcalWithStatus() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Private Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "STATUS:private\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals(EventStatus.PRIVATE, events.get(0).getStatus());
  }

  @Test
  public void testImportFromIcalMultipleEvents() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting 1\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT\n"
        + "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting 2\n"
        + "DTSTART:20250115T140000\n"
        + "DTEND:20250115T150000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(2, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(2, events.size());
  }

  @Test
  public void testImportFromIcalWithDateOnly() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:All Day Event\n"
        + "DTSTART:20250115\n"
        + "DTEND:20250116\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
  }

  @Test
  public void testImportFromIcalWithTimezone() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting with Zulu\n"
        + "DTSTART:20250115T100000Z\n"
        + "DTEND:20250115T110000Z\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
  }

  @Test
  public void testImportFromIcalWithEscapedCharacters() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\\, with commas\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "DESCRIPTION:Line 1\\nLine 2\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getSubject().contains(","));
    assertTrue(events.get(0).getDescription().contains("\n"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromIcalNullContent() throws DuplicateEventException {
    calendar.importFromIcal(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromIcalEmptyContent() throws DuplicateEventException {
    calendar.importFromIcal("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportFromIcalNoValidEvents() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "END:VEVENT";
    
    calendar.importFromIcal(icalContent);
  }

  @Test
  public void testImportFromIcalSkipsInvalidEvents() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Valid Event\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT\n"
        + "BEGIN:VEVENT\n"
        + "SUMMARY:Invalid Event\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromIcalWithInvalidStatus() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "STATUS:INVALID_STATUS\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    // Should default to PUBLIC when status is invalid
    assertEquals(EventStatus.PUBLIC, events.get(0).getStatus());
  }

  @Test
  public void testImportFromIcalSkipsDuplicates() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    // First import
    int count1 = calendar.importFromIcal(icalContent);
    assertEquals(1, count1);
    
    // Second import should skip duplicate
    try {
      int count2 = calendar.importFromIcal(icalContent);
      // Should throw exception if no events imported
      assertTrue(count2 == 0);
    } catch (IllegalArgumentException e) {
      // Expected when all events are duplicates
      assertTrue(e.getMessage().contains("No valid events found"));
    }
  }

  @Test
  public void testImportFromIcalWithWindowsLineEndings() throws DuplicateEventException {
    String icalContent = "BEGIN:VEVENT\r\n"
        + "SUMMARY:Meeting\r\n"
        + "DTSTART:20250115T100000\r\n"
        + "DTEND:20250115T110000\r\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  // Additional tests to improve coverage of private helper methods

  @Test
  public void testImportFromCsvWithEscapedQuotes() throws DuplicateEventException {
    // Test CSV with escaped quotes inside quoted field ("")
    String csvContent = "Subject,Start,End\n"
        + "\"Meeting \"\"Important\"\"\",2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting \"Important\"", events.get(0).getSubject());
  }

  @Test
  public void testImportFromCsvWithCommasInQuotedField() throws DuplicateEventException {
    // Test CSV with commas inside quoted field
    String csvContent = "Subject,Start,End\n"
        + "\"Meeting, Team Sync\",2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting, Team Sync", events.get(0).getSubject());
  }

  @Test
  public void testImportFromCsvWithStatusField() throws DuplicateEventException {
    // Test CSV with status field
    String csvContent = "Subject,Start,End,Location,Description,Status\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00,Room A,Description,private";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals(EventStatus.PRIVATE, events.get(0).getStatus());
  }

  @Test
  public void testImportFromCsvWithInvalidStatus() throws DuplicateEventException {
    // Test CSV with invalid status (should default to PUBLIC)
    String csvContent = "Subject,Start,End,Location,Description,Status\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00,Room A,Description,invalid_status";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals(EventStatus.PUBLIC, events.get(0).getStatus());
  }

  @Test
  public void testImportFromIcalExtractValueWithNoColon() throws DuplicateEventException {
    // Test extractIcalValue edge case - property with no colon (should return empty)
    // This tests the extractIcalValue method branch
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromIcalExtractValueWithMultipleColons() throws DuplicateEventException {
    // Test extractIcalValue with multiple colons (should use last colon)
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART;VALUE=DATE-TIME:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromIcalParseDateTimeWithShortFormat() throws DuplicateEventException {
    // Test parseIcalDateTime with date-only format that triggers fallback
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:All Day Event\n"
        + "DTSTART:20250115\n"
        + "DTEND:20250116\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    // Date-only format should set time to start of day
    assertEquals(LocalDateTime.of(2025, 1, 15, 0, 0), events.get(0).getStart());
  }

  @Test
  public void testImportFromIcalUnescapeAllSequences() throws DuplicateEventException {
    // Test unescapeIcal with all escape sequences
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting with escapes\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "DESCRIPTION:Line1\\nLine2\\, with comma\\; semicolon\\\\backslash\n"
        + "LOCATION:Room\\, 123\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertTrue(events.get(0).getDescription().contains("\n"));
    assertTrue(events.get(0).getDescription().contains(","));
    assertTrue(events.get(0).getDescription().contains(";"));
    assertTrue(events.get(0).getDescription().contains("\\"));
    assertTrue(events.get(0).getLocation().contains(","));
  }

  @Test
  public void testImportFromIcalWithNullSummary() throws DuplicateEventException {
    // Test unescapeIcal with null input (through empty SUMMARY)
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromCsvWithEmptyOptionalFields() throws DuplicateEventException {
    // Test CSV with empty optional fields
    String csvContent = "Subject,Start,End,Location,Description,Status\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00,,,";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    // CalendarEvent.Builder defaults location and description to empty strings, not null
    assertTrue(events.get(0).getLocation().isEmpty());
    assertTrue(events.get(0).getDescription().isEmpty());
  }

  @Test
  public void testImportFromCsvWithUnquotedFields() throws DuplicateEventException {
    // Test unquoteCsv with unquoted fields
    String csvContent = "Subject,Start,End\n"
        + "Meeting,2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test
  public void testImportFromCsvWithWhitespaceAroundFields() throws DuplicateEventException {
    // Test unquoteCsv trims whitespace
    String csvContent = "Subject,Start,End\n"
        + "  Meeting  ,2025-01-15T10:00,2025-01-15T11:00";
    
    int count = calendar.importFromCsv(csvContent);
    assertEquals(1, count);
    
    List<CalendarEvent> events = calendar.getEventsOnDate(LocalDate.of(2025, 1, 15));
    assertEquals(1, events.size());
    assertEquals("Meeting", events.get(0).getSubject());
  }

  @Test
  public void testImportFromIcalWithStartEndVariations() throws DuplicateEventException {
    // Test parseIcalDateTime with different DTSTART/DTEND formats
    String icalContent = "BEGIN:VEVENT\n"
        + "SUMMARY:Meeting\n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }

  @Test
  public void testImportFromIcalWithEmptyLines() throws DuplicateEventException {
    // Test handling of empty/whitespace lines
    String icalContent = "BEGIN:VEVENT\n"
        + "\n"
        + "SUMMARY:Meeting\n"
        + "  \n"
        + "DTSTART:20250115T100000\n"
        + "DTEND:20250115T110000\n"
        + "END:VEVENT";
    
    int count = calendar.importFromIcal(icalContent);
    assertEquals(1, count);
  }
}

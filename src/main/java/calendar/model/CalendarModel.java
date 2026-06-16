package calendar.model;

import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Interface defining the contract for calendar data models.
 * Provides methods for adding, querying, editing, and exporting events.
 *
 * <p>Follows Dependency Inversion Principle - controllers depend on this
 * abstraction rather than concrete implementations.
 *
 * @version 1.0
 */
public interface CalendarModel {

  /**
   * Creates a new calendar with the provided name and timezone.
   *
   * @param name the unique calendar name
   * @param zone the timezone to associate with the calendar
   * @throws DuplicateCalendarException if a calendar with the same name already exists
   * @throws IllegalArgumentException if name or zone is invalid
   */
  void createCalendar(String name, ZoneId zone) throws DuplicateCalendarException;

  /**
   * Renames an existing calendar.
   *
   * @param currentName the existing calendar name
   * @param newName the desired new name
   * @throws CalendarNotFoundException if currentName does not exist
   * @throws DuplicateCalendarException if newName already exists
   * @throws IllegalArgumentException if inputs are invalid
   */
  void renameCalendar(String currentName, String newName)
      throws CalendarNotFoundException, DuplicateCalendarException;

  /**
   * Updates the timezone associated with an existing calendar.
   *
   * @param calendarName calendar to update
   * @param newZone new timezone
   * @throws CalendarNotFoundException if calendar does not exist
   * @throws DuplicateEventException if conversion introduces overlapping duplicates
   */
  void updateCalendarTimezone(String calendarName, ZoneId newZone)
      throws CalendarNotFoundException, DuplicateEventException;

  /**
   * Selects the active calendar for subsequent operations.
   *
   * @param calendarName the calendar to activate
   * @throws CalendarNotFoundException if calendar does not exist
   */
  void useCalendar(String calendarName) throws CalendarNotFoundException;

  /**
   * Checks if a calendar context is currently selected.
   *
   * @return true if an active calendar is currently selected.
   */
  boolean hasActiveCalendar();

  /**
   * Retrieves the name of the active calendar if one is selected.
   *
   * @return the active calendar name, or {@code null} if none selected.
   */
  String getActiveCalendarName();

  /**
   * Obtains the timezone associated with the active calendar.
   *
   * @return the active calendar timezone
   * @throws IllegalStateException if no calendar is active
   */
  ZoneId getActiveCalendarZone();

  /**
   * Lists the names of all calendars managed by this model in insertion order.
   *
  * @return ordered list of all calendar names
   */
  List<String> listCalendars();

  /**
   * Adds an event to the calendar.
   *
   * @param event the event to add
   * @throws DuplicateEventException if an event with same (subject, start, end) exists
   * @throws IllegalArgumentException if event is null or invalid
   */
  void addEvent(CalendarEvent event) throws DuplicateEventException;

  /**
   * Edits a single event occurrence.
   *
   * @param originalSubject the original subject of the event
   * @param originalStart the original start time of the event
   * @param newSubject new subject (null to keep current)
   * @param newStart new start time (null to keep current)
   * @param newEnd new end time (null to keep current)
   * @param newDescription new description (null to keep current)
   * @param newLocation new location (null to keep current)
   * @param newStatus new status (null to keep current)
   * @throws EventNotFoundException if event not found
   * @throws DuplicateEventException if edit would create duplicate
   */
  void editEvent(String originalSubject, LocalDateTime originalStart,
                 String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                 String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException;

  /**
   * Edits all events in a series starting from a specific date (inclusive).
   *
   * @param originalSubject the subject of an event in the series
   * @param startFrom the date to start editing from (edits this and future)
   * @param newSubject new subject (null to keep current)
   * @param newStart new start time (null to keep current)
   * @param newEnd new end time (null to keep current)
   * @param newDescription new description (null to keep current)
   * @param newLocation new location (null to keep current)
   * @param newStatus new status (null to keep current)
   * @throws EventNotFoundException if no events found in series
   * @throws DuplicateEventException if edit would create duplicates
   */
  void editSeriesFromDate(String originalSubject, LocalDateTime startFrom,
                          String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                          String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException;

  /**
   * Edits all events in a series.
   *
   * @param seriesId the series identifier
   * @param newSubject new subject (null to keep current)
   * @param newStart new start time (null to keep current)
   * @param newEnd new end time (null to keep current)
   * @param newDescription new description (null to keep current)
   * @param newLocation new location (null to keep current)
   * @param newStatus new status (null to keep current)
   * @throws EventNotFoundException if series not found
   * @throws DuplicateEventException if edit would create duplicates
   */
  void editEntireSeries(String seriesId,
                        String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                        String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException;

  /**
   * Gets all events on a specific date.
   *
   * @param date the date to query
   * @return list of events on that date (may be empty)
   */
  List<CalendarEvent> getEventsOnDate(LocalDate date);

  /**
   * Gets all events within a date range (inclusive).
   *
   * @param startDate the start of the range
   * @param endDate the end of the range
   * @return list of events in range (may be empty)
   */
  List<CalendarEvent> getEventsInRange(LocalDate startDate, LocalDate endDate);

  /**
   * Gets all events in the calendar.
   *
   * @return list of all events
   */
  List<CalendarEvent> getAllEvents();

  /**
   * Checks if the user is busy at a specific date and time.
   *
   * @param dateTime the date-time to check
   * @return true if any event overlaps this time, false if available
   */
  boolean isBusyAt(LocalDateTime dateTime);

  /**
   * Exports all events to CSV format.
   *
   * @return CSV string representation of all events
   */
  String exportToCsv();

  /**
   * Exports all events to iCal format.
   *
  * @return iCal string representation of all events
  */
  String exportToIcal();

  /**
   * Copies a single event (or its entire series) to another calendar at a target start time.
   *
   * @param subject event subject
   * @param sourceStart start time in source calendar timezone
   * @param targetCalendarName destination calendar name
   * @param targetStart new start time in target calendar timezone
   * @throws CalendarNotFoundException if source or target calendar missing
   * @throws EventNotFoundException if source event missing
   * @throws DuplicateEventException if copy introduces duplicates in target
   */
  void copyEvent(String subject, LocalDateTime sourceStart,
                 String targetCalendarName, LocalDateTime targetStart)
      throws CalendarNotFoundException, EventNotFoundException, DuplicateEventException;

  /**
   * Copies all events on a specific date to another calendar.
   *
   * @param sourceDate source date (in active calendar timezone)
   * @param targetCalendarName destination calendar
   * @param targetDate target date (target calendar timezone)
   * @throws CalendarNotFoundException if calendars missing
   * @throws DuplicateEventException if copy introduces duplicates
   */
  void copyEventsOnDate(LocalDate sourceDate, String targetCalendarName, LocalDate targetDate)
      throws CalendarNotFoundException, DuplicateEventException;

  /**
   * Copies all events overlapping the provided interval (inclusive) to another calendar.
   *
   * @param startDate inclusive start date
   * @param endDate inclusive end date
   * @param targetCalendarName destination calendar
   * @param targetStartDate start date of the copied interval in target timezone
   * @throws CalendarNotFoundException if calendars missing
   * @throws DuplicateEventException if copy introduces duplicates
   */
  void copyEventsBetween(LocalDate startDate, LocalDate endDate,
                         String targetCalendarName, LocalDate targetStartDate)
      throws CalendarNotFoundException, DuplicateEventException;

  /**
   * Imports events from CSV format into the active calendar.
   *
   * @param csvContent the CSV content to import
   * @return number of events imported
   * @throws IllegalStateException if no calendar is active
   * @throws DuplicateEventException if imported events conflict with existing ones
   */
  int importFromCsv(String csvContent) throws DuplicateEventException;

  /**
   * Imports events from iCal format into the active calendar.
   *
   * @param icalContent the iCal content to import
   * @return number of events imported
   * @throws IllegalStateException if no calendar is active
   * @throws DuplicateEventException if imported events conflict with existing ones
   */
  int importFromIcal(String icalContent) throws DuplicateEventException;
}
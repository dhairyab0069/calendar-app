package calendar.model;

import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Multi-calendar in-memory model implementation backed by sparse hash storage per calendar.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Manage lifecycle of named calendars and their timezones</li>
 *   <li>Delegate single-calendar operations to {@link SingleCalendarStore}</li>
 *   <li>Provide cross-calendar capabilities such as copying and exporting</li>
 * </ul>
 *
 * <p>This class maintains MVC and SOLID principles by separating calendar management logic
 * from the storage details encapsulated inside {@link SingleCalendarStore}.</p>
 */
public class SparseHashCalendar implements CalendarModel {

  private static final DateTimeFormatter ICAL_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
  private static final String PROD_ID = "-//team_top//Virtual Calendar//EN";

  private final Map<String, CalendarContainer> calendars;
  private String activeCalendarName;

  /**
   * Constructs an empty multi-calendar model.
   */
  public SparseHashCalendar() {
    this.calendars = new LinkedHashMap<>();
  }

  @Override
  public void createCalendar(String name, ZoneId zone) throws DuplicateCalendarException {
    validateCalendarName(name);
    Objects.requireNonNull(zone, "Timezone cannot be null");

    if (containsCalendar(name)) {
      throw new DuplicateCalendarException("Calendar name already exists: " + name);
    }

    calendars.put(name, new CalendarContainer(name, zone));
    if (activeCalendarName == null) {
      activeCalendarName = name;
    }
  }

  @Override
  public int importFromCsv(String csvContent) throws DuplicateEventException {
    CalendarContainer container = requireActiveCalendar();

    if (csvContent == null || csvContent.trim().isEmpty()) {
      throw new IllegalArgumentException("CSV content cannot be empty");
    }

    String[] lines = csvContent.split("\r?\n");
    if (lines.length < 2) {
      throw new IllegalArgumentException("CSV must contain header and at least one event");
    }

    int importedCount = 0;
    List<String> errors = new ArrayList<>();

    // Skip header line (line 0)
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isEmpty()) {
        continue;
      }

      try {
        CalendarEvent event = parseCsvLine(line);
        container.getStore().addEvent(event);
        importedCount++;
      } catch (DuplicateEventException e) {
        // Skip duplicates
        errors.add("Line " + (i + 1) + ": Duplicate event skipped");
      } catch (Exception e) {
        errors.add("Line " + (i + 1) + ": " + e.getMessage());
      }
    }

    if (importedCount == 0) {
      throw new IllegalArgumentException("Failed to import any events. Errors: "
          + String.join("; ", errors));
    }

    return importedCount;
  }

  @Override
  public int importFromIcal(String icalContent) throws DuplicateEventException {
    CalendarContainer container = requireActiveCalendar();

    if (icalContent == null || icalContent.trim().isEmpty()) {
      throw new IllegalArgumentException("iCal content cannot be empty");
    }

    String[] lines = icalContent.split("\r?\n");
    int importedCount = 0;

    String subject = null;
    LocalDateTime start = null;
    LocalDateTime end = null;
    String description = null;
    String location = null;
    EventStatus status = EventStatus.PUBLIC;

    boolean inEvent = false;

    for (String line : lines) {
      line = line.trim();

      if (line.equals("BEGIN:VEVENT")) {
        inEvent = true;
        // Reset all fields
        subject = null;
        start = null;
        end = null;
        description = null;
        location = null;
        status = EventStatus.PUBLIC;

      } else if (line.equals("END:VEVENT") && inEvent) {
        // Build and add event
        if (subject != null && start != null && end != null) {
          try {
            // Use default subject if empty
            String eventSubject = subject.trim().isEmpty() ? "(No Subject)" : subject;
            CalendarEvent.Builder builder = CalendarEvent.builder(eventSubject, start, end);
            if (description != null) {
              builder.description(description);
            }
            if (location != null) {
              builder.location(location);
            }
            builder.status(status);

            container.getStore().addEvent(builder.build());
            importedCount++;
          } catch (DuplicateEventException e) {
            // Skip duplicates
          } catch (Exception e) {
            // Skip invalid events
          }
        }
        inEvent = false;

      } else if (inEvent) {
        // Parse event properties
        if (line.startsWith("SUMMARY:")) {
          subject = unescapeIcal(line.substring(8));
        } else if (line.startsWith("DTSTART")) {
          start = parseIcalDateTime(extractIcalValue(line));
        } else if (line.startsWith("DTEND")) {
          end = parseIcalDateTime(extractIcalValue(line));
        } else if (line.startsWith("DESCRIPTION:")) {
          description = unescapeIcal(line.substring(12));
        } else if (line.startsWith("LOCATION:")) {
          location = unescapeIcal(line.substring(9));
        } else if (line.startsWith("STATUS:")) {
          try {
            status = EventStatus.fromValue(line.substring(7).toLowerCase());
          } catch (Exception e) {
            status = EventStatus.PUBLIC;
          }
        }
      }
    }

    if (importedCount == 0) {
      throw new IllegalArgumentException("No valid events found in iCal file");
    }

    return importedCount;
  }

  /**
   * Parses a single CSV line into a CalendarEvent.
   */
  private CalendarEvent parseCsvLine(String line) {
    // Split by comma, but respect quoted fields
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    char[] chars = line.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (c == '"') {
        // Check if this is an escaped quote ("" inside quoted field)
        if (inQuotes && i + 1 < chars.length && chars[i + 1] == '"') {
          // This is an escaped quote, add a literal quote and skip the next character
          current.append('"');
          i++; // Skip the next quote
        } else {
          // This is a quote delimiter
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        parts.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    parts.add(current.toString());

    if (parts.size() < 3) {
      throw new IllegalArgumentException("Invalid CSV format: need at least Subject,Start,End");
    }

    String subject = unquoteCsv(parts.get(0));
    LocalDateTime start = LocalDateTime.parse(unquoteCsv(parts.get(1)));
    LocalDateTime end = LocalDateTime.parse(unquoteCsv(parts.get(2)));

    CalendarEvent.Builder builder = CalendarEvent.builder(subject, start, end);

    if (parts.size() > 3 && !parts.get(3).trim().isEmpty()) {
      builder.location(unquoteCsv(parts.get(3)));
    }
    if (parts.size() > 4 && !parts.get(4).trim().isEmpty()) {
      builder.description(unquoteCsv(parts.get(4)));
    }
    if (parts.size() > 5 && !parts.get(5).trim().isEmpty()) {
      try {
        builder.status(EventStatus.fromValue(unquoteCsv(parts.get(5)).toLowerCase()));
      } catch (Exception e) {
        builder.status(EventStatus.PUBLIC);
      }
    }

    return builder.build();
  }

  /**
   * Removes quotes from CSV field if present.
   */
  private String unquoteCsv(String value) {
    if (value == null) {
      return "";
    }
    value = value.trim();
    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
      return value.substring(1, value.length() - 1).replace("\"\"", "\"");
    }
    return value;
  }

  /**
   * Extracts the value from an iCal property line.
   */
  private String extractIcalValue(String line) {
    int colonIndex = line.lastIndexOf(':');
    if (colonIndex == -1) {
      return "";
    }
    return line.substring(colonIndex + 1);
  }

  /**
   * Parses an iCal datetime string.
   */
  private LocalDateTime parseIcalDateTime(String icalDateTime) {
    // Remove timezone suffix if present (Z)
    String cleaned = icalDateTime.replace("Z", "").replace("T", "");

    // Try date-only format first (8 characters: yyyyMMdd)
    if (cleaned.length() == 8) {
      try {
        LocalDate date = LocalDate.parse(cleaned.substring(0, 8),
            DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date.atStartOfDay();
      } catch (Exception e) {
        // Fall through to error
      }
    }

    // Try full datetime format (14+ characters: yyyyMMddHHmmss)
    if (cleaned.length() >= 14) {
      try {
        return LocalDateTime.parse(cleaned.substring(0, 14),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      } catch (Exception e) {
        // Fall through to error
      }
    }

    throw new IllegalArgumentException("Invalid iCal datetime: " + icalDateTime);
  }

  /**
   * Unescapes iCal special characters.
   */
  private String unescapeIcal(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\");
  }

  @Override
  public void renameCalendar(String currentName, String newName)
      throws CalendarNotFoundException, DuplicateCalendarException {
    validateCalendarName(newName);
    CalendarContainer container = calendars.remove(currentName);
    if (container == null) {
      throw new CalendarNotFoundException("Calendar not found: " + currentName);
    }

    if (containsCalendar(newName)) {
      // Restore original mapping before throwing
      calendars.put(currentName, container);
      throw new DuplicateCalendarException("Calendar name already exists: " + newName);
    }

    container.setName(newName);
    calendars.put(newName, container);
    if (Objects.equals(activeCalendarName, currentName)) {
      activeCalendarName = newName;
    }
  }

  @Override
  public void updateCalendarTimezone(String calendarName, ZoneId newZone)
      throws CalendarNotFoundException, DuplicateEventException {
    Objects.requireNonNull(newZone, "Timezone cannot be null");
    CalendarContainer container = requireCalendar(calendarName);

    if (container.getZone().equals(newZone)) {
      return;
    }

    List<CalendarEvent> events = container.getStore().getAllEvents();
    List<CalendarEvent> converted = new ArrayList<>(events.size());
    for (CalendarEvent event : events) {
      converted.add(convertEventToZone(event, container.getZone(), newZone));
    }

    SingleCalendarStore replacement = new SingleCalendarStore();
    for (CalendarEvent event : converted) {
      replacement.addEvent(event);
    }

    container.setZone(newZone);
    container.setStore(replacement);
  }

  @Override
  public void useCalendar(String calendarName) throws CalendarNotFoundException {
    requireCalendar(calendarName);
    activeCalendarName = calendarName;
  }

  @Override
  public boolean hasActiveCalendar() {
    return activeCalendarName != null && calendars.containsKey(activeCalendarName);
  }

  @Override
  public String getActiveCalendarName() {
    return hasActiveCalendar() ? activeCalendarName : null;
  }

  @Override
  public ZoneId getActiveCalendarZone() {
    return requireActiveCalendar().getZone();
  }

  @Override
  public List<String> listCalendars() {
    return new ArrayList<>(calendars.keySet());
  }

  @Override
  public void addEvent(CalendarEvent event) throws DuplicateEventException {
    requireActiveCalendar().getStore().addEvent(event);
  }

  @Override
  public void editEvent(String originalSubject, LocalDateTime originalStart,
                        String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                        String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {
    requireActiveCalendar().getStore().editEvent(
        originalSubject, originalStart,
        newSubject, newStart, newEnd,
        newDescription, newLocation, newStatus);
  }

  @Override
  public void editSeriesFromDate(String originalSubject, LocalDateTime startFrom,
                                 String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                                 String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {
    requireActiveCalendar().getStore().editSeriesFromDate(
        originalSubject, startFrom,
        newSubject, newStart, newEnd,
        newDescription, newLocation, newStatus);
  }

  @Override
  public void editEntireSeries(String seriesId, String newSubject, LocalDateTime newStart,
                               LocalDateTime newEnd, String newDescription,
                               String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {
    requireActiveCalendar().getStore().editEntireSeries(
        seriesId, newSubject, newStart, newEnd, newDescription, newLocation, newStatus);
  }

  @Override
  public List<CalendarEvent> getEventsOnDate(LocalDate date) {
    return requireActiveCalendar().getStore().getEventsOnDate(date);
  }

  @Override
  public List<CalendarEvent> getEventsInRange(LocalDate startDate, LocalDate endDate) {
    return requireActiveCalendar().getStore().getEventsInRange(startDate, endDate);
  }

  @Override
  public List<CalendarEvent> getAllEvents() {
    return requireActiveCalendar().getStore().getAllEvents();
  }

  @Override
  public boolean isBusyAt(LocalDateTime dateTime) {
    return requireActiveCalendar().getStore().isBusyAt(dateTime);
  }

  @Override
  public String exportToCsv() {
    return requireActiveCalendar().getStore().exportToCsv();
  }

  @Override
  public String exportToIcal() {
    CalendarContainer active = requireActiveCalendar();
    return buildIcal(active);
  }

  @Override
  public void copyEvent(String subject, LocalDateTime sourceStart,
                        String targetCalendarName, LocalDateTime targetStart)
      throws CalendarNotFoundException, EventNotFoundException, DuplicateEventException {
    CalendarContainer source = requireActiveCalendar();
    CalendarContainer target = requireCalendar(targetCalendarName);

    CalendarEvent pivot = source.getStore().findEvent(subject, sourceStart);
    if (pivot == null) {
      throw new EventNotFoundException(
          String.format("Event not found: %s at %s", subject, sourceStart));
    }

    List<CalendarEvent> eventsToCopy;
    Map<String, String> seriesIdMap = new java.util.HashMap<>();
    if (pivot.getSeriesId() != null) {
      eventsToCopy = source.getStore().getSeriesEvents(pivot.getSeriesId());
      eventsToCopy.sort(Comparator.comparing(CalendarEvent::getStart));
      seriesIdMap.put(pivot.getSeriesId(), UUID.randomUUID().toString());
    } else {
      eventsToCopy = List.of(pivot);
    }

    ZonedDateTime sourcePivotStart = pivot.getStart().atZone(source.getZone());
    ZonedDateTime targetPivotStart = targetStart.atZone(target.getZone());
    Duration shift = Duration.between(
        sourcePivotStart.toInstant(), targetPivotStart.toInstant());

    for (CalendarEvent event : eventsToCopy) {
      String newSeriesId = event.getSeriesId() == null
          ? null
          : seriesIdMap.computeIfAbsent(event.getSeriesId(), id -> UUID.randomUUID().toString());
      CalendarEvent cloned = shiftEvent(
          event, shift, source.getZone(), target.getZone(), newSeriesId);
      target.getStore().addEvent(cloned);
    }
  }

  @Override
  public void copyEventsOnDate(LocalDate sourceDate, String targetCalendarName,
                               LocalDate targetDate)
      throws CalendarNotFoundException, DuplicateEventException {
    CalendarContainer source = requireActiveCalendar();
    CalendarContainer target = requireCalendar(targetCalendarName);

    List<CalendarEvent> events = source.getStore().getEventsOnDate(sourceDate);
    events.sort(Comparator.comparing(CalendarEvent::getStart));
    Map<String, String> seriesIdMap = new java.util.HashMap<>();

    for (CalendarEvent event : events) {
      String newSeriesId = resolveSeries(seriesIdMap, event.getSeriesId());
      LocalDateTime newStart = alignDateTime(
          event.getStart(), source.getZone(), target.getZone(), sourceDate, targetDate);
      LocalDateTime newEnd = alignDateTime(
          event.getEnd(), source.getZone(), target.getZone(), sourceDate, targetDate);

      CalendarEvent copy = cloneEvent(event, newStart, newEnd, newSeriesId);
      target.getStore().addEvent(copy);
    }
  }

  @Override
  public void copyEventsBetween(LocalDate startDate, LocalDate endDate,
                                String targetCalendarName, LocalDate targetStartDate)
      throws CalendarNotFoundException, DuplicateEventException {
    CalendarContainer source = requireActiveCalendar();
    CalendarContainer target = requireCalendar(targetCalendarName);

    List<CalendarEvent> events = source.getStore().getEventsInRange(startDate, endDate);
    events.sort(Comparator.comparing(CalendarEvent::getStart));
    Map<String, String> seriesIdMap = new java.util.HashMap<>();

    for (CalendarEvent event : events) {
      String newSeriesId = resolveSeries(seriesIdMap, event.getSeriesId());
      LocalDateTime newStart = alignDateTime(event.getStart(), source.getZone(),
          target.getZone(), startDate, targetStartDate);
      LocalDateTime newEnd = alignDateTime(event.getEnd(), source.getZone(),
          target.getZone(), startDate, targetStartDate);

      CalendarEvent copy = cloneEvent(event, newStart, newEnd, newSeriesId);
      target.getStore().addEvent(copy);
    }
  }

  private CalendarContainer requireActiveCalendar() {
    if (!hasActiveCalendar()) {
      throw new IllegalStateException("No calendar is currently in use");
    }
    return calendars.get(activeCalendarName);
  }

  private CalendarContainer requireCalendar(String name) throws CalendarNotFoundException {
    CalendarContainer container = calendars.get(name);
    if (container == null) {
      throw new CalendarNotFoundException("Calendar not found: " + name);
    }
    return container;
  }

  private boolean containsCalendar(String name) {
    return calendars.containsKey(name);
  }

  private void validateCalendarName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Calendar name cannot be blank");
    }
  }

  private CalendarEvent convertEventToZone(CalendarEvent event, ZoneId fromZone, ZoneId toZone) {
    LocalDateTime newStart = LocalDateTime.ofInstant(
        event.getStart().atZone(fromZone).toInstant(), toZone);
    LocalDateTime newEnd = LocalDateTime.ofInstant(
        event.getEnd().atZone(fromZone).toInstant(), toZone);

    CalendarEvent.Builder builder = CalendarEvent.builder(
            event.getSubject(), newStart, newEnd)
        .description(event.getDescription())
        .location(event.getLocation())
        .status(event.getStatus());

    if (event.getSeriesId() != null) {
      builder.seriesId(event.getSeriesId());
    }
    return builder.build();
  }

  private CalendarEvent shiftEvent(CalendarEvent event, Duration shift,
                                   ZoneId sourceZone, ZoneId targetZone,
                                   String newSeriesId) {
    Instant shiftedStart = event.getStart().atZone(sourceZone).toInstant().plus(shift);
    Instant shiftedEnd = event.getEnd().atZone(sourceZone).toInstant().plus(shift);

    LocalDateTime newStart = LocalDateTime.ofInstant(shiftedStart, targetZone);
    LocalDateTime newEnd = LocalDateTime.ofInstant(shiftedEnd, targetZone);

    return cloneEvent(event, newStart, newEnd, newSeriesId);
  }

  private CalendarEvent cloneEvent(CalendarEvent original, LocalDateTime newStart,
                                   LocalDateTime newEnd, String newSeriesId) {
    CalendarEvent.Builder builder = CalendarEvent.builder(
            original.getSubject(), newStart, newEnd)
        .description(original.getDescription())
        .location(original.getLocation())
        .status(original.getStatus());

    if (newSeriesId != null) {
      builder.seriesId(newSeriesId);
    }
    return builder.build();
  }

  private LocalDateTime alignDateTime(LocalDateTime sourceDateTime,
                                      ZoneId sourceZone, ZoneId targetZone,
                                      LocalDate sourceBaseDate, LocalDate targetBaseDate) {
    long dayOffset = ChronoUnit.DAYS.between(sourceBaseDate, sourceDateTime.toLocalDate());

    ZonedDateTime sourceZdt = sourceDateTime.atZone(sourceZone);
    ZonedDateTime converted = sourceZdt.withZoneSameInstant(targetZone);
    long conversionOffset = ChronoUnit.DAYS.between(
        sourceDateTime.toLocalDate(), converted.toLocalDate());

    LocalDate targetDate = targetBaseDate.plusDays(dayOffset + conversionOffset);
    return LocalDateTime.of(targetDate, converted.toLocalTime());
  }

  private String resolveSeries(Map<String, String> seriesIdMap, String originalSeriesId) {
    if (originalSeriesId == null) {
      return null;
    }
    return seriesIdMap.computeIfAbsent(originalSeriesId, id -> UUID.randomUUID().toString());
  }

  private String buildIcal(CalendarContainer container) {
    StringBuilder builder = new StringBuilder();
    builder.append("BEGIN:VCALENDAR").append("\r\n");
    builder.append("VERSION:2.0").append("\r\n");
    builder.append("PRODID:").append(PROD_ID).append("\r\n");
    builder.append("CALSCALE:GREGORIAN").append("\r\n");
    builder.append("METHOD:PUBLISH").append("\r\n");
    builder.append("X-WR-CALNAME:").append(escapeIcal(container.getName())).append("\r\n");
    builder.append("X-WR-TIMEZONE:").append(container.getZone().getId()).append("\r\n");

    List<CalendarEvent> events = container.getStore().getAllEvents();
    events.sort(Comparator.comparing(CalendarEvent::getStart));
    String tz = container.getZone().getId();
    String dtStamp = ICAL_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)) + "Z";

    for (CalendarEvent event : events) {
      builder.append("BEGIN:VEVENT").append("\r\n");
      builder.append("UID:").append(UUID.randomUUID()).append("@team-top").append("\r\n");
      builder.append("DTSTAMP:").append(dtStamp).append("\r\n");
      builder.append("DTSTART;TZID=").append(tz).append(":")
          .append(formatIcalDateTime(event.getStart())).append("\r\n");
      builder.append("DTEND;TZID=").append(tz).append(":")
          .append(formatIcalDateTime(event.getEnd())).append("\r\n");
      builder.append("SUMMARY:").append(escapeIcal(event.getSubject())).append("\r\n");
      if (event.getDescription() != null && !event.getDescription().isEmpty()) {
        builder.append("DESCRIPTION:").append(escapeIcal(event.getDescription())).append("\r\n");
      }
      if (event.getLocation() != null && !event.getLocation().isEmpty()) {
        builder.append("LOCATION:").append(escapeIcal(event.getLocation())).append("\r\n");
      }
      builder.append("STATUS:").append(event.getStatus().getValue().toUpperCase()).append("\r\n");
      builder.append("END:VEVENT").append("\r\n");
    }

    builder.append("END:VCALENDAR").append("\r\n");
    return builder.toString();
  }


  private String formatIcalDateTime(LocalDateTime dateTime) {
    return ICAL_DATE_TIME.format(dateTime);
  }

  private String escapeIcal(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n");
  }

  /**
   * Container tying a calendar name, timezone, and backing store together.
   */
  private static final class CalendarContainer {
    private String name;
    private ZoneId zone;
    private SingleCalendarStore store;

    CalendarContainer(String name, ZoneId zone) {
      this.name = name;
      this.zone = zone;
      this.store = new SingleCalendarStore();
    }

    String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    ZoneId getZone() {
      return zone;
    }

    void setZone(ZoneId zone) {
      this.zone = zone;
    }

    SingleCalendarStore getStore() {
      return store;
    }

    void setStore(SingleCalendarStore store) {
      this.store = store;
    }
  }
}

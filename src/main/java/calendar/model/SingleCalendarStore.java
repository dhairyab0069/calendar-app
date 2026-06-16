package calendar.model;

import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory storage for a single calendar.
 *
 * <p>This class encapsulates the prior SparseHashCalendar behaviour and is composed
 * by the multi-calendar system. It remains package-private to avoid leaking
 * persistence details outside the model package.</p>
 */
class SingleCalendarStore {

  private final Map<LocalDate, List<CalendarEvent>> eventsByDate;
  private final Map<String, List<CalendarEvent>> eventsBySeries;

  SingleCalendarStore() {
    this.eventsByDate = new HashMap<>();
    this.eventsBySeries = new HashMap<>();
  }

  void addEvent(CalendarEvent event) throws DuplicateEventException {
    if (event == null) {
      throw new IllegalArgumentException("Event cannot be null");
    }

    boolean hasRecurrenceDays = event.getRecurrenceDays() != null
        && event.getRecurrenceDays().length > 0;
    boolean hasStopCondition = event.getRepeatCount() != null
        || event.getRepeatUntil() != null;

    if (hasRecurrenceDays && hasStopCondition) {
      addRecurringEvent(event);
    } else {
      if (isDuplicate(event)) {
        throw new DuplicateEventException(
            String.format("Event already exists: %s at %s",
                event.getSubject(), event.getStart()));
      }
      addSingleEvent(event);
    }
  }

  void editEvent(String originalSubject, LocalDateTime originalStart,
                 String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                 String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {

    CalendarEvent event = findEvent(originalSubject, originalStart);
    if (event == null) {
      throw new EventNotFoundException(
          String.format("Event not found: %s at %s", originalSubject, originalStart));
    }

    LocalDate oldDate = event.getStart().toLocalDate();
    List<CalendarEvent> events = eventsByDate.get(oldDate);
    if (events != null) {
      events.remove(event);
      if (events.isEmpty()) {
        eventsByDate.remove(oldDate);
      }
    }

    String originalSeriesId = event.getSeriesId();
    if (originalSeriesId != null) {
      List<CalendarEvent> seriesEvents = eventsBySeries.get(originalSeriesId);
      if (seriesEvents != null) {
        seriesEvents.remove(event);
        if (seriesEvents.isEmpty()) {
          eventsBySeries.remove(originalSeriesId);
        }
      }
    }

    CalendarEvent modified = CalendarEvent.builder(
            newSubject != null ? newSubject : event.getSubject(),
            newStart != null ? newStart : event.getStart(),
            newEnd != null ? newEnd : event.getEnd()
        )
        .description(newDescription != null ? newDescription : event.getDescription())
        .location(newLocation != null ? newLocation : event.getLocation())
        .status(newStatus != null ? newStatus : event.getStatus())
        .build();

    if (isDuplicate(modified)) {
      eventsByDate.computeIfAbsent(oldDate, k -> new ArrayList<>()).add(event);
      if (originalSeriesId != null) {
        eventsBySeries.computeIfAbsent(originalSeriesId, k -> new ArrayList<>()).add(event);
      }
      throw new DuplicateEventException("Edit would create duplicate event");
    }

    LocalDate newDate = modified.getStart().toLocalDate();
    eventsByDate.computeIfAbsent(newDate, k -> new ArrayList<>()).add(modified);
  }

  void editSeriesFromDate(String originalSubject, LocalDateTime startFrom,
                          String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                          String newDescription, String newLocation,
                          EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {

    CalendarEvent firstEvent = findEvent(originalSubject, startFrom);
    if (firstEvent == null || !firstEvent.isRecurring()) {
      throw new EventNotFoundException("Event or series not found");
    }

    String seriesId = firstEvent.getSeriesId();
    List<CalendarEvent> seriesEvents = eventsBySeries.get(seriesId);
    if (seriesEvents == null) {
      throw new EventNotFoundException("Series not found");
    }

    List<CalendarEvent> toEdit = seriesEvents.stream()
        .filter(e -> !e.getStart().isBefore(startFrom))
        .collect(Collectors.toList());

    for (CalendarEvent event : toEdit) {
      LocalDate oldDate = event.getStart().toLocalDate();
      List<CalendarEvent> dateEvents = eventsByDate.get(oldDate);
      if (dateEvents != null) {
        dateEvents.remove(event);
        if (dateEvents.isEmpty()) {
          eventsByDate.remove(oldDate);
        }
      }

      CalendarEvent modified = createModifiedSeriesEvent(event, newSubject, newStart, newEnd,
          newDescription, newLocation, newStatus);

      if (isDuplicate(modified)) {
        throw new DuplicateEventException("Edit would create duplicate in series");
      }

      LocalDate newDate = modified.getStart().toLocalDate();
      eventsByDate.computeIfAbsent(newDate, k -> new ArrayList<>()).add(modified);

      int index = seriesEvents.indexOf(event);
      if (index >= 0) {
        seriesEvents.set(index, modified);
      }
    }
  }

  void editEntireSeries(String seriesId,
                        String newSubject, LocalDateTime newStart, LocalDateTime newEnd,
                        String newDescription, String newLocation, EventStatus newStatus)
      throws EventNotFoundException, DuplicateEventException {

    List<CalendarEvent> seriesEvents = eventsBySeries.get(seriesId);
    if (seriesEvents == null || seriesEvents.isEmpty()) {
      throw new EventNotFoundException("Series not found: " + seriesId);
    }

    List<CalendarEvent> eventsToEdit = new ArrayList<>(seriesEvents);

    for (CalendarEvent event : eventsToEdit) {
      LocalDate oldDate = event.getStart().toLocalDate();
      List<CalendarEvent> dateEvents = eventsByDate.get(oldDate);
      if (dateEvents != null) {
        dateEvents.remove(event);
        if (dateEvents.isEmpty()) {
          eventsByDate.remove(oldDate);
        }
      }

      CalendarEvent modified = createModifiedSeriesEvent(event, newSubject, newStart, newEnd,
          newDescription, newLocation, newStatus);

      if (isDuplicate(modified)) {
        throw new DuplicateEventException("Edit would create duplicate in series");
      }

      LocalDate newDate = modified.getStart().toLocalDate();
      eventsByDate.computeIfAbsent(newDate, k -> new ArrayList<>()).add(modified);

      int index = seriesEvents.indexOf(event);
      if (index >= 0) {
        seriesEvents.set(index, modified);
      }
    }
  }

  List<CalendarEvent> getEventsOnDate(LocalDate date) {
    List<CalendarEvent> events = eventsByDate.get(date);
    return events == null ? new ArrayList<>() : new ArrayList<>(events);
  }

  List<CalendarEvent> getEventsInRange(LocalDate startDate, LocalDate endDate) {
    List<CalendarEvent> result = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      result.addAll(getEventsOnDate(date));
    }

    return result;
  }

  List<CalendarEvent> getAllEvents() {
    return eventsByDate.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  boolean isBusyAt(LocalDateTime dateTime) {
    LocalDate date = dateTime.toLocalDate();
    List<CalendarEvent> events = eventsByDate.get(date);

    if (events == null) {
      return false;
    }

    return events.stream()
        .anyMatch(e -> !dateTime.isBefore(e.getStart()) && dateTime.isBefore(e.getEnd()));
  }

  String exportToCsv() {
    StringBuilder csv = new StringBuilder();
    csv.append("Subject,Start,End,Description,Location,Status\n");

    List<CalendarEvent> allEvents = getAllEvents();
    allEvents.sort((e1, e2) -> e1.getStart().compareTo(e2.getStart()));

    for (CalendarEvent event : allEvents) {
      csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
          escapeCsv(event.getSubject()),
          event.getStart(),
          event.getEnd(),
          escapeCsv(event.getDescription()),
          escapeCsv(event.getLocation()),
          event.getStatus()));
    }

    return csv.toString();
  }

  CalendarEvent findEvent(String subject, LocalDateTime start) {
    LocalDate date = start.toLocalDate();
    List<CalendarEvent> events = eventsByDate.get(date);

    if (events == null) {
      return null;
    }

    return events.stream()
        .filter(e -> e.getSubject().equals(subject) && e.getStart().equals(start))
        .findFirst()
        .orElse(null);
  }

  List<CalendarEvent> getSeriesEvents(String seriesId) {
    List<CalendarEvent> events = eventsBySeries.get(seriesId);
    return events == null ? new ArrayList<>() : new ArrayList<>(events);
  }

  private void addSingleEvent(CalendarEvent event) {
    LocalDate date = event.getStart().toLocalDate();
    eventsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
    if (event.getSeriesId() != null) {
      eventsBySeries.computeIfAbsent(event.getSeriesId(), k -> new ArrayList<>()).add(event);
    }
  }

  private void addRecurringEvent(CalendarEvent event) throws DuplicateEventException {
    if (!event.getStart().toLocalDate().equals(event.getEnd().toLocalDate())) {
      throw new IllegalArgumentException(
          "Recurring event template must start and end on the same day");
    }

    List<CalendarEvent> occurrences = generateOccurrences(event);

    for (CalendarEvent occurrence : occurrences) {
      if (isDuplicate(occurrence)) {
        throw new DuplicateEventException(
            String.format("Recurring event would create duplicate: %s at %s",
                occurrence.getSubject(), occurrence.getStart()));
      }
    }

    for (CalendarEvent occurrence : occurrences) {
      LocalDate date = occurrence.getStart().toLocalDate();
      eventsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(occurrence);
    }

    if (event.getSeriesId() != null) {
      eventsBySeries.put(event.getSeriesId(), new ArrayList<>(occurrences));
    }
  }

  private List<CalendarEvent> generateOccurrences(CalendarEvent template) {
    List<CalendarEvent> occurrences = new ArrayList<>();
    LocalDate currentDate = template.getStart().toLocalDate();
    int count = 0;

    Integer maxCount = template.getRepeatCount();
    LocalDateTime endDate = template.getRepeatUntil();

    int maxIterations = 10000;
    int iterations = 0;

    while (shouldContinueGenerating(currentDate, count, maxCount, endDate)) {
      iterations++;
      if (iterations > maxIterations) {
        throw new IllegalStateException("Too many recurrence iterations (safety limit)");
      }

      if (matchesRecurrencePattern(currentDate, template.getRecurrenceDays())) {
        CalendarEvent occurrence = createOccurrence(template, currentDate);
        occurrences.add(occurrence);
        count++;

        if (maxCount != null && count >= maxCount) {
          break;
        }
      }
      currentDate = currentDate.plusDays(1);
    }

    return occurrences;
  }

  private boolean shouldContinueGenerating(LocalDate current, int count,
                                           Integer maxCount, LocalDateTime endDate) {
    if (maxCount != null) {
      return count < maxCount;
    }
    if (endDate != null) {
      return !current.isAfter(endDate.toLocalDate());
    }
    return false;
  }

  private boolean matchesRecurrencePattern(LocalDate date, Weekday[] recurrenceDays) {
    if (recurrenceDays == null || recurrenceDays.length == 0) {
      return false;
    }

    DayOfWeek dayOfWeek = date.getDayOfWeek();
    for (Weekday weekday : recurrenceDays) {
      if (weekday.getDayOfWeek() == dayOfWeek) {
        return true;
      }
    }
    return false;
  }

  private CalendarEvent createOccurrence(CalendarEvent template, LocalDate date) {
    LocalDateTime newStart = date.atTime(template.getStart().toLocalTime());
    LocalDateTime newEnd = date.atTime(template.getEnd().toLocalTime());

    CalendarEvent.Builder builder = new CalendarEvent.Builder(
        template.getSubject(),
        newStart,
        newEnd
    );

    builder.description(template.getDescription())
        .location(template.getLocation())
        .status(template.getStatus())
        .seriesId(template.getSeriesId());

    return builder.build();
  }

  private boolean isDuplicate(CalendarEvent event) {
    LocalDate date = event.getStart().toLocalDate();
    List<CalendarEvent> existingEvents = eventsByDate.get(date);

    if (existingEvents == null) {
      return false;
    }

    return existingEvents.stream()
        .anyMatch(e -> e.equals(event));
  }

  private CalendarEvent createModifiedSeriesEvent(CalendarEvent original,
                                                  String newSubject, LocalDateTime newStart,
                                                  LocalDateTime newEnd, String newDescription,
                                                  String newLocation, EventStatus newStatus) {
    CalendarEvent.Builder builder = CalendarEvent.builder(
        newSubject != null ? newSubject : original.getSubject(),
        newStart != null ? newStart : original.getStart(),
        newEnd != null ? newEnd : original.getEnd()
    );

    builder.description(newDescription != null ? newDescription : original.getDescription())
        .location(newLocation != null ? newLocation : original.getLocation())
        .status(newStatus != null ? newStatus : original.getStatus());

    if (original.getSeriesId() != null) {
      builder.seriesId(original.getSeriesId());
    }

    return builder.build();
  }

  private String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}


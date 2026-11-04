package calendar.controller;

import calendar.controller.command.CommandContext;
import calendar.controller.command.CommandRegistry;
import calendar.controller.parser.CommandType;
import calendar.controller.parser.CopyEventParams;
import calendar.controller.parser.CopyEventsBetweenParams;
import calendar.controller.parser.CopyEventsOnParams;
import calendar.controller.parser.CreateCalendarParams;
import calendar.controller.parser.CreateEventParams;
import calendar.controller.parser.EditCalendarParams;
import calendar.controller.parser.EditEventParams;
import calendar.controller.parser.ExportParams;
import calendar.controller.parser.ParsedCommand;
import calendar.controller.parser.PrintDateParams;
import calendar.controller.parser.PrintRangeParams;
import calendar.controller.parser.StatusParams;
import calendar.controller.parser.UseCalendarParams;
import calendar.controller.parser.ViewDateParams;
import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import calendar.model.CalendarEvent;
import calendar.model.CalendarModel;
import calendar.model.EventStatus;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Executes parsed commands by coordinating between model and view.
 *
 * @version 1.1
 */
public class CommandHandler {

  private final CalendarModel model;
  private final CalendarView view;
  private final CommandRegistry commandRegistry;
  private CommandContext activeContext = CommandContext.empty();

  /**
   * Creates a command handler.
   *
   * @param model the calendar model
   * @param view  the calendar view
   */
  public CommandHandler(CalendarModel model, CalendarView view) {
    this.model = model;
    this.view = view;
    this.commandRegistry = new CommandRegistry(this);
  }

  /**
   * Executes a parsed command by consulting the command registry.
   *
   * @param command the parsed command
   */
  public void execute(ParsedCommand command) {
    CommandType type = command.getType();
    calendar.controller.command.Command executor = commandRegistry.resolve(type);

    if (executor == null) {
      view.displayError("Command execution not implemented: " + command.getType());
      return;
    }

    withContext(command.getContext(),
        () -> executor.execute(this, command.getParams(), command.getContext()));
  }

  private void withContext(CommandContext context, Runnable action) {
    CommandContext previousContext = activeContext;
    CommandContext effectiveContext =
        context == null ? CommandContext.empty() : context;
    activeContext = effectiveContext;

    String previousCalendar = null;
    boolean switched = false;
    if (effectiveContext.getCalendarName().isPresent()) {
      String target = effectiveContext.getCalendarName().get();
      String current = model.hasActiveCalendar() ? model.getActiveCalendarName() : null;
      if (current == null || !current.equals(target)) {
        previousCalendar = current;
        try {
          model.useCalendar(target);
          switched = true;
        } catch (CalendarNotFoundException e) {
          view.displayError(e.getMessage());
          activeContext = previousContext;
          return;
        }
      }
    }

    try {
      action.run();
    } finally {
      if (switched && previousCalendar != null) {
        try {
          model.useCalendar(previousCalendar);
        } catch (CalendarNotFoundException e) {
          // Ignore if previous calendar no longer exists.
        }
      }
      activeContext = previousContext;
    }
  }

  private boolean ensureCalendarSelected() {
    if (!model.hasActiveCalendar()) {
      view.displayError("No calendar in use. Run 'use calendar --name <name>' first.");
      return false;
    }
    return true;
  }

  /**
   * Handles the {@code create calendar} command.
   *
   * @param params parsed parameters
   */
  public void handleCreateCalendar(CreateCalendarParams params) {
    try {
      model.createCalendar(params.getName(), params.getZoneId());
      view.displayMessage(String.format("Calendar '%s' created (%s)",
          params.getName(), params.getZoneId().getId()));
    } catch (DuplicateCalendarException e) {
      view.displayError(e.getMessage());
    } catch (IllegalArgumentException e) {
      view.displayError("Failed to create calendar: " + e.getMessage());
    }
  }

  /**
   * Handles {@code edit calendar} command.
   *
   * @param params parsed parameters
   */
  public void handleEditCalendar(EditCalendarParams params) {
    String property = params.getProperty().toLowerCase();
    try {
      switch (property) {
        case "name":
          model.renameCalendar(params.getCalendarName(), params.getNewValue());
          view.displayMessage(String.format("Calendar '%s' renamed to '%s'",
              params.getCalendarName(), params.getNewValue()));
          break;
        case "timezone":
          ZoneId zone = ZoneId.of(params.getNewValue());
          model.updateCalendarTimezone(params.getCalendarName(), zone);
          view.displayMessage(String.format("Calendar '%s' timezone set to %s",
              params.getCalendarName(), zone.getId()));
          break;
        default:
          view.displayError("Unsupported calendar property: " + property);
      }
    } catch (CalendarNotFoundException | DuplicateCalendarException e) {
      view.displayError(e.getMessage());
    } catch (DuplicateEventException e) {
      view.displayError("Timezone update failed due to conflicting events: " + e.getMessage());
    } catch (Exception e) {
      view.displayError("Failed to edit calendar: " + e.getMessage());
    }
  }

  /**
   * Handles {@code use calendar} command.
   *
   * @param params parsed parameters
   */
  public void handleUseCalendar(UseCalendarParams params) {
    try {
      model.useCalendar(params.getCalendarName());
      view.displayMessage(String.format("Now using calendar '%s'", params.getCalendarName()));
    } catch (CalendarNotFoundException e) {
      view.displayError(e.getMessage());
    }
  }

  /**
   * Handles {@code create} event command.
   */
  public void handleCreateEvent(CreateEventParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      CalendarEvent event;

      if (params.isRecurring()) {
        // Use recurring builder for recurring events
        CalendarEvent.Builder builder = CalendarEvent.recurringBuilder(
            params.getSubject(),
            params.getStart(),
            params.getEnd(),
            params.getRecurrenceDays());

        // IMPORTANT: Only set ONE of repeatCount or repeatUntil, not both!
        // Each method clears the other, so calling both results in both being null
        if (params.getRepeatCount() != null) {
          builder.repeatCount(params.getRepeatCount());
        } else if (params.getRepeatUntil() != null) {
          builder.repeatUntil(params.getRepeatUntil());
        }

        event = builder.build();
      } else {
        // Use regular builder for single events
        event = CalendarEvent.builder(
                params.getSubject(),
                params.getStart(),
                params.getEnd())
            .build();
      }

      model.addEvent(event);
      view.displayMessage(String.format("Event created: %s", params.getSubject()));

    } catch (DuplicateEventException e) {
      view.displayError("Duplicate event: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      view.displayError("Invalid event: " + e.getMessage());
    }
  }

  /**
   * Handles {@code edit event} command.
   */
  public void handleEditEvent(EditEventParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      // Prepare the new values based on which property is being edited
      String newSubject = null;
      LocalDateTime newStart = null;
      LocalDateTime newEnd = null;
      String newDescription = null;
      String newLocation = null;
      EventStatus newStatus = null;

      String property = params.getProperty().toLowerCase();
      String newValue = params.getNewValue();

      switch (property) {
        case "subject":
          newSubject = newValue;
          break;
        case "start":
          newStart = parseDateTime(newValue);
          break;
        case "end":
          newEnd = parseDateTime(newValue);
          break;
        case "description":
          newDescription = newValue;
          break;
        case "location":
          newLocation = newValue;
          break;
        case "status":
          newStatus = EventStatus.fromString(newValue);
          break;
        default:
          throw new IllegalArgumentException("Unknown property: " + params.getProperty());
      }

      model.editEvent(
          params.getSubject(),
          params.getDateTime(),
          newSubject,
          newStart,
          newEnd,
          newDescription,
          newLocation,
          newStatus
      );

      view.displayMessage(String.format("Event '%s' updated successfully", params.getSubject()));
    } catch (EventNotFoundException e) {
      view.displayError("Event not found: " + e.getMessage());
    } catch (DuplicateEventException e) {
      view.displayError("Edit would create duplicate: " + e.getMessage());
    } catch (Exception e) {
      view.displayError("Failed to edit event: " + e.getMessage());
    }
  }

  /**
   * Handles {@code edit events} (series from date) command.
   */
  public void handleEditSeries(EditEventParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      // Prepare the new values based on which property is being edited
      String newSubject = null;
      LocalDateTime newStart = null;
      LocalDateTime newEnd = null;
      String newDescription = null;
      String newLocation = null;
      EventStatus newStatus = null;

      String property = params.getProperty().toLowerCase();
      String newValue = params.getNewValue();

      switch (property) {
        case "subject":
          newSubject = newValue;
          break;
        case "start":
          newStart = parseDateTime(newValue);
          break;
        case "end":
          newEnd = parseDateTime(newValue);
          break;
        case "description":
          newDescription = newValue;
          break;
        case "location":
          newLocation = newValue;
          break;
        case "status":
          newStatus = EventStatus.fromString(newValue);
          break;
        default:
          throw new IllegalArgumentException("Unknown property: " + params.getProperty());
      }

      model.editSeriesFromDate(
          params.getSubject(),
          params.getDateTime(),
          newSubject,
          newStart,
          newEnd,
          newDescription,
          newLocation,
          newStatus
      );

      view.displayMessage(String.format("Series '%s' updated from %s onwards",
          params.getSubject(), params.getDateTime().toLocalDate()));
    } catch (EventNotFoundException e) {
      view.displayError("Event series not found: " + e.getMessage());
    } catch (DuplicateEventException e) {
      view.displayError("Edit would create duplicate: " + e.getMessage());
    } catch (Exception e) {
      view.displayError("Failed to edit series: " + e.getMessage());
    }
  }

  /**
   * Handles {@code edit series} command.
   */
  public void handleEditAllSeries(EditEventParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      // First find an event in the series to get the series ID
      List<CalendarEvent> events = model.getEventsOnDate(params.getDateTime().toLocalDate());
      CalendarEvent targetEvent = null;

      for (CalendarEvent event : events) {
        if (event.getSubject().equals(params.getSubject())
            && event.getStart().equals(params.getDateTime())) {
          targetEvent = event;
          break;
        }
      }

      if (targetEvent == null) {
        throw new EventNotFoundException("Event not found in series");
      }

      if (targetEvent.getSeriesId() == null) {
        throw new IllegalArgumentException("Event is not part of a series");
      }

      // Prepare the new values based on which property is being edited
      String newSubject = null;
      LocalDateTime newStart = null;
      LocalDateTime newEnd = null;
      String newDescription = null;
      String newLocation = null;
      EventStatus newStatus = null;

      String property = params.getProperty().toLowerCase();
      String newValue = params.getNewValue();

      switch (property) {
        case "subject":
          newSubject = newValue;
          break;
        case "start":
          newStart = parseDateTime(newValue);
          break;
        case "end":
          newEnd = parseDateTime(newValue);
          break;
        case "description":
          newDescription = newValue;
          break;
        case "location":
          newLocation = newValue;
          break;
        case "status":
          newStatus = EventStatus.fromString(newValue);
          break;
        default:
          throw new IllegalArgumentException("Unknown property: " + params.getProperty());
      }

      model.editEntireSeries(
          targetEvent.getSeriesId(),
          newSubject,
          newStart,
          newEnd,
          newDescription,
          newLocation,
          newStatus
      );

      view.displayMessage(String.format("Entire series '%s' updated", params.getSubject()));
    } catch (EventNotFoundException e) {
      view.displayError("Event series not found: " + e.getMessage());
    } catch (DuplicateEventException e) {
      view.displayError("Edit would create duplicate: " + e.getMessage());
    } catch (Exception e) {
      view.displayError("Failed to edit series: " + e.getMessage());
    }
  }

  /**
   * Helper method to parse date-time strings.
   */
  private LocalDateTime parseDateTime(String dateTimeStr) {
    // Handle both formats: "2025-01-15T10:00" and "2025-01-15 10:00"
    String normalized = dateTimeStr.replace(" ", "T");
    return LocalDateTime.parse(normalized);
  }

  /**
   * Handles print date command.
   */
  public void handlePrintDate(PrintDateParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDate queryDate = params.getDate();
    List<CalendarEvent> events =
        transformEventsForDisplay(model.getEventsOnDate(queryDate));
    LocalDate displayDate = adjustDateForDisplay(queryDate);
    String zoneSuffix = formatTimezoneSuffix();

    if (events.isEmpty()) {
      view.displayMessage("No events on " + displayDate + zoneSuffix);
      return;
    }

    java.time.format.DateTimeFormatter timeFmt =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    view.displayMessage("Events on " + displayDate + zoneSuffix + ":");
    for (CalendarEvent e : events) {
      LocalDateTime start = e.getStart();
      LocalDateTime end = e.getEnd();
      StringBuilder line = new StringBuilder();
      line.append("  - ")
          .append(e.getSubject())
          .append(" from ")
          .append(start.toLocalTime().format(timeFmt))
          .append(" to ")
          .append(end.toLocalTime().format(timeFmt));
      if (e.getLocation() != null && !e.getLocation().isEmpty()) {
        line.append(" at ").append(e.getLocation());
      }
      view.displayMessage(line.toString());
    }
  }

  /**
   * Handles print range command.
   */
  public void handlePrintRange(PrintRangeParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDate startDate = params.getStartDate();
    LocalDate endDate = params.getEndDate();

    List<CalendarEvent> events =
        transformEventsForDisplay(model.getEventsInRange(startDate, endDate));

    LocalDate displayStart = adjustDateForDisplay(startDate);
    LocalDate displayEnd = adjustDateForDisplay(endDate);
    String zoneSuffix = formatTimezoneSuffix();

    if (events.isEmpty()) {
      view.displayMessage(
          "No events from " + displayStart + " to " + displayEnd + zoneSuffix);
      return;
    }

    java.time.format.DateTimeFormatter dateFmt =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    java.time.format.DateTimeFormatter timeFmt =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    view.displayMessage(
        "Events from " + displayStart + " to " + displayEnd + zoneSuffix + ":");
    for (CalendarEvent e : events) {
      LocalDateTime start = e.getStart();
      LocalDateTime end = e.getEnd();
      StringBuilder line = new StringBuilder();
      line.append(e.getSubject())
          .append(" starting on ")
          .append(start.toLocalDate().format(dateFmt))
          .append(" at ")
          .append(start.toLocalTime().format(timeFmt))
          .append(", ending on ")
          .append(end.toLocalDate().format(dateFmt))
          .append(" at ")
          .append(end.toLocalTime().format(timeFmt));
      if (e.getLocation() != null && !e.getLocation().isEmpty()) {
        line.append(" at ").append(e.getLocation());
      }
      view.displayMessage(line.toString());
    }
  }

  /**
   * Handles status command.
   */
  public void handleStatus(StatusParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDateTime dateTime = params.getDateTime();
    boolean isBusy = model.isBusyAt(dateTime);
    LocalDateTime displayTime = convertToContextZone(dateTime);
    String status = isBusy ? "busy" : "available";
    view.displayMessage(
        "Status at " + displayTime + formatTimezoneSuffix() + ": " + status);
  }

  /**
   * Handles export command.
   */
  public void handleExport(ExportParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      String target = params.getFilename();
      Path path = Paths.get(target).toAbsolutePath().normalize();
      // Ensure parent directory exists if any
      Path parent = path.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      String lower = target.toLowerCase();
      String payload;
      if (lower.endsWith(".csv")) {
        payload = model.exportToCsv();
      } else if (lower.endsWith(".ical") || lower.endsWith(".ics")) {
        payload = model.exportToIcal();
      } else {
        view.displayError("Unsupported export format. Use .csv or .ical/.ics");
        return;
      }
      Files.write(path, payload.getBytes(StandardCharsets.UTF_8));
      view.displayMessage("Calendar exported to: " + params.getFilename());
      view.displayMessage("Absolute path: " + path);
    } catch (Exception e) {
      view.displayError("Export failed: " + e.getMessage());
    }
  }

  /**
   * Handles {@code copy event} command.
   *
   * @param params parsed parameters
   */
  public void handleCopyEvent(CopyEventParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      model.copyEvent(
          params.getSubject(),
          params.getSourceStart(),
          params.getTargetCalendar(),
          params.getTargetStart());
      view.displayMessage(String.format("Event '%s' copied to calendar '%s'",
          params.getSubject(), params.getTargetCalendar()));
    } catch (CalendarNotFoundException | EventNotFoundException | DuplicateEventException e) {
      view.displayError(e.getMessage());
    } catch (IllegalArgumentException e) {
      view.displayError("Failed to copy event: " + e.getMessage());
    }
  }

  /**
   * Handles {@code copy events on} command.
   *
   * @param params parsed parameters
   */
  public void handleCopyEventsOn(CopyEventsOnParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      model.copyEventsOnDate(
          params.getSourceDate(),
          params.getTargetCalendar(),
          params.getTargetDate());
      view.displayMessage(String.format("Events on %s copied to %s (calendar '%s')",
          params.getSourceDate(), params.getTargetDate(), params.getTargetCalendar()));
    } catch (CalendarNotFoundException | DuplicateEventException e) {
      view.displayError(e.getMessage());
    } catch (IllegalArgumentException e) {
      view.displayError("Failed to copy events: " + e.getMessage());
    }
  }

  /**
   * Handles {@code copy events between} command.
   *
   * @param params parsed parameters
   */
  public void handleCopyEventsBetween(CopyEventsBetweenParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    try {
      model.copyEventsBetween(
          params.getStartDate(),
          params.getEndDate(),
          params.getTargetCalendar(),
          params.getTargetStartDate());
      view.displayMessage(String.format("Events between %s and %s copied to %s (calendar '%s')",
          params.getStartDate(), params.getEndDate(),
          params.getTargetStartDate(), params.getTargetCalendar()));
    } catch (CalendarNotFoundException | DuplicateEventException e) {
      view.displayError(e.getMessage());
    } catch (IllegalArgumentException e) {
      view.displayError("Failed to copy events: " + e.getMessage());
    }
  }

  /**
   * Handles view day command - displays formatted day view.
   */
  public void handleViewDay(ViewDateParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDate queryDate = params.getDate();
    List<CalendarEvent> events =
        transformEventsForDisplay(model.getEventsOnDate(queryDate));
    LocalDate displayDate = adjustDateForDisplay(queryDate);

    if (view instanceof ConsoleView) {
      ConsoleView consoleView = (ConsoleView) view;
      emitTimezoneNotice();
      String formattedDay =
          consoleView.getDayView().formatDay(displayDate, new ArrayList<>(events));
      view.displayMessage(formattedDay);
    } else {
      view.displayMessage(
          "Events on " + displayDate + formatTimezoneSuffix() + ":");
      view.displayEvents(new ArrayList<>(events));
    }
  }

  /**
   * Handles view week command - displays formatted week view.
   */
  public void handleViewWeek(ViewDateParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDate weekStart = params.getDate();

    Map<LocalDate, List<CalendarEvent>> eventsByDay = new HashMap<>();
    for (int i = 0; i < 7; i++) {
      LocalDate date = weekStart.plusDays(i);
      List<CalendarEvent> events =
          transformEventsForDisplay(model.getEventsOnDate(date));
      eventsByDay.put(date, new ArrayList<>(events));
    }

    if (view instanceof ConsoleView) {
      ConsoleView consoleView = (ConsoleView) view;
      emitTimezoneNotice();
      String formattedWeek = consoleView.getWeekView().formatWeek(weekStart, eventsByDay);
      view.displayMessage(formattedWeek);
    } else {
      view.displayMessage("Week starting " + weekStart + formatTimezoneSuffix());
      for (int i = 0; i < 7; i++) {
        LocalDate date = weekStart.plusDays(i);
        view.displayMessage("\n" + date + ":");
        view.displayEvents(new ArrayList<>(eventsByDay.get(date)));
      }
    }
  }

  /**
   * Handles view month command - displays formatted month view.
   */
  public void handleViewMonth(ViewDateParams params) {
    if (!ensureCalendarSelected()) {
      return;
    }
    LocalDate date = params.getDate();
    YearMonth yearMonth = YearMonth.from(date);

    LocalDate firstDay = yearMonth.atDay(1);
    LocalDate lastDay = yearMonth.atEndOfMonth();
    List<CalendarEvent> monthEvents =
        transformEventsForDisplay(model.getEventsInRange(firstDay, lastDay));

    Set<LocalDate> daysWithEvents = monthEvents.stream()
        .map(event -> event.getStart().toLocalDate())
        .collect(Collectors.toSet());

    if (view instanceof ConsoleView) {
      ConsoleView consoleView = (ConsoleView) view;
      emitTimezoneNotice();
      String formattedMonth = consoleView.getMonthView().formatMonth(yearMonth, daysWithEvents);
      view.displayMessage(formattedMonth);
    } else {
      view.displayMessage("Month: " + yearMonth + formatTimezoneSuffix());
      view.displayMessage("Days with events: " + daysWithEvents.size());
    }
  }

  /**
   * Handles exit command.
   */
  public void handleExit() {
    view.displayMessage("Goodbye!");
  }

  private String formatTimezoneSuffix() {
    return activeContext.getZoneId()
        .map(zone -> " [" + zone.getId() + "]")
        .orElse("");
  }

  private void emitTimezoneNotice() {
    activeContext.getZoneId()
        .ifPresent(zone -> view.displayMessage(
            "(displaying times in " + zone.getId() + ")"));
  }

  private List<CalendarEvent> transformEventsForDisplay(List<CalendarEvent> events) {
    ZoneId sourceZone = getActiveCalendarZoneOrNull();
    if (!activeContext.getZoneId().isPresent() || sourceZone == null) {
      return new ArrayList<>(events);
    }

    ZoneId targetZone = activeContext.getZoneId().get();
    List<CalendarEvent> converted = new ArrayList<>(events.size());
    for (CalendarEvent event : events) {
      converted.add(transformEventForDisplay(event, sourceZone, targetZone));
    }
    return converted;
  }

  private CalendarEvent transformEventForDisplay(CalendarEvent event,
                                                 ZoneId sourceZone,
                                                 ZoneId targetZone) {
    LocalDateTime newStart = convertDateTime(event.getStart(), sourceZone, targetZone);
    LocalDateTime newEnd = convertDateTime(event.getEnd(), sourceZone, targetZone);

    CalendarEvent.Builder builder =
        CalendarEvent.builder(event.getSubject(), newStart, newEnd)
            .description(event.getDescription())
            .location(event.getLocation())
            .status(event.getStatus());
    if (event.getSeriesId() != null) {
      builder.seriesId(event.getSeriesId());
    }
    return builder.build();
  }

  private LocalDate adjustDateForDisplay(LocalDate date) {
    ZoneId sourceZone = getActiveCalendarZoneOrNull();
    if (!activeContext.getZoneId().isPresent() || sourceZone == null) {
      return date;
    }
    ZoneId targetZone = activeContext.getZoneId().get();
    LocalDateTime startOfDay = date.atStartOfDay();
    return convertDateTime(startOfDay, sourceZone, targetZone).toLocalDate();
  }

  private LocalDateTime convertToContextZone(LocalDateTime dateTime) {
    ZoneId sourceZone = getActiveCalendarZoneOrNull();
    if (!activeContext.getZoneId().isPresent() || sourceZone == null) {
      return dateTime;
    }
    ZoneId targetZone = activeContext.getZoneId().get();
    return convertDateTime(dateTime, sourceZone, targetZone);
  }

  private LocalDateTime convertDateTime(LocalDateTime dateTime,
                                        ZoneId sourceZone,
                                        ZoneId targetZone) {
    if (dateTime == null || sourceZone == null || targetZone == null) {
      return dateTime;
    }
    return dateTime.atZone(sourceZone)
        .withZoneSameInstant(targetZone)
        .toLocalDateTime();
  }

  private ZoneId getActiveCalendarZoneOrNull() {
    if (!model.hasActiveCalendar()) {
      return null;
    }
    return model.getActiveCalendarZone();
  }
}
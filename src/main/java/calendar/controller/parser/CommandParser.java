package calendar.controller.parser;

import calendar.controller.command.CommandContext;
import calendar.controller.util.DateTimeParser;
import calendar.exceptions.InvalidCommandException;
import calendar.model.Weekday;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses command strings into structured command data.
 *
 * <p>Supported commands with fixed recurrence and print range parsing.
 *
 * @version 1.2
 */
public class CommandParser {

  private static final Pattern AT_TZ_PATTERN =
      Pattern.compile("(?i)\\s+at\\s+tz\\s+([^\\s]+)\\s*$");
  private static final Pattern ON_PATTERN =
      Pattern.compile("(?i)\\s+on\\s+(\"([^\"]+)\"|[^\\s]+)\\s*$");

  /**
   * Parses a command string into a ParsedCommand object.
   *
   * @param commandStr the command string
   * @return the parsed command
   * @throws InvalidCommandException if the command is invalid
   */
  public ParsedCommand parse(String commandStr) throws InvalidCommandException {
    if (commandStr == null || commandStr.trim().isEmpty()) {
      throw new InvalidCommandException("Command cannot be empty");
    }

    String trimmed = commandStr.trim();
    String lowerCommand = trimmed.toLowerCase();

    if (lowerCommand.equals("exit")) {
      return new ParsedCommand(CommandType.EXIT, null);
    }

    if (lowerCommand.startsWith("create calendar")) {
      return parseCreateCalendar(trimmed.substring("create calendar".length()).trim());
    }

    if (lowerCommand.startsWith("edit calendar")) {
      return parseEditCalendar(trimmed.substring("edit calendar".length()).trim());
    }

    if (lowerCommand.startsWith("use calendar")) {
      return parseUseCalendar(trimmed.substring("use calendar".length()).trim());
    }

    if (lowerCommand.startsWith("copy event ")) {
      return parseCopyEventCommand(trimmed.substring(11));
    }

    if (lowerCommand.startsWith("copy events on ")) {
      return parseCopyEventsOn(trimmed.substring(15));
    }

    if (lowerCommand.startsWith("copy events between ")) {
      return parseCopyEventsBetween(
          trimmed.substring("copy events between ".length()));
    }

    if (lowerCommand.startsWith("create event ")) {
      return parseCreateCommand(trimmed.substring(13));
    }

    if (lowerCommand.startsWith("create ")) {
      return parseCreateCommand(trimmed.substring(7));
    }

    if (lowerCommand.startsWith("edit ")) {
      return parseEditCommand(trimmed.substring(5));
    }

    // Handle different print command formats
    if (lowerCommand.startsWith("print events on ")) {
      return parsePrintOnCommand(trimmed.substring(16));
    }

    if (lowerCommand.startsWith("print events from ")) {
      return parsePrintRangeCommand(trimmed.substring(18));
    }

    if (lowerCommand.startsWith("print from ")) {
      return parsePrintRangeCommand(trimmed.substring(11));
    }

    if (lowerCommand.startsWith("print ")) {
      return parsePrintCommand(trimmed.substring(6));
    }

    if (lowerCommand.startsWith("view day ")) {
      return parseViewDayCommand(trimmed.substring(9));
    }

    if (lowerCommand.startsWith("view week ")) {
      return parseViewWeekCommand(trimmed.substring(10));
    }

    if (lowerCommand.startsWith("view month ")) {
      return parseViewMonthCommand(trimmed.substring(11));
    }

    if (lowerCommand.startsWith("show status on ")) {
      return parseStatusCommand(trimmed.substring(15));
    }

    if (lowerCommand.startsWith("status ")) {
      return parseStatusCommand(trimmed.substring(7));
    }

    if (lowerCommand.startsWith("export cal ")) {
      return parseExportCommand(trimmed.substring(11));
    }

    if (lowerCommand.startsWith("export ")) {
      return parseExportCommand(trimmed.substring(7));
    }

    throw new InvalidCommandException("Unknown command: " + commandStr);
  }

  private ParsedCommand parseCreateCalendar(String args) throws InvalidCommandException {
    String name = extractFlagValue(args, "--name");
    String timezone = extractFlagValue(args, "--timezone");

    if (name == null || name.isEmpty()) {
      throw new InvalidCommandException("create calendar requires --name");
    }
    if (timezone == null || timezone.isEmpty()) {
      throw new InvalidCommandException("create calendar requires --timezone");
    }

    try {
      CreateCalendarParams params =
          new CreateCalendarParams(name, ZoneId.of(timezone));
      return new ParsedCommand(CommandType.CREATE_CALENDAR, params);
    } catch (Exception e) {
      throw new InvalidCommandException("Unsupported timezone: " + timezone, e);

    }
  }

  private ParsedCommand parseEditCalendar(String args) throws InvalidCommandException {
    String name = extractFlagValue(args, "--name");
    if (name == null || name.isEmpty()) {
      throw new InvalidCommandException("edit calendar requires --name");
    }

    String lower = args.toLowerCase();
    int propertyIndex = lower.indexOf("--property");
    if (propertyIndex == -1) {
      throw new InvalidCommandException("edit calendar requires --property <property> <value>");
    }

    int cursor = skipWhitespace(args, propertyIndex + "--property".length());
    if (cursor >= args.length()) {
      throw new InvalidCommandException("Missing property name after --property");
    }

    int end = cursor;
    while (end < args.length() && !Character.isWhitespace(args.charAt(end))) {
      end++;
    }

    String property = args.substring(cursor, end).trim().toLowerCase();
    if (property.isEmpty()) {
      throw new InvalidCommandException("Property name cannot be empty");
    }

    cursor = skipWhitespace(args, end);
    if (cursor >= args.length()) {
      throw new InvalidCommandException("Missing new value for property " + property);
    }

    String newValue;
    if (args.charAt(cursor) == '"') {
      int closing = args.indexOf('"', cursor + 1);
      if (closing == -1) {
        throw new InvalidCommandException("Unclosed quote in property value");
      }
      newValue = args.substring(cursor + 1, closing);
    } else {
      newValue = args.substring(cursor).trim();
    }

    if (newValue.isEmpty()) {
      throw new InvalidCommandException("Property value cannot be empty");
    }

    EditCalendarParams params =
        new EditCalendarParams(name, property, newValue);
    return new ParsedCommand(CommandType.EDIT_CALENDAR, params);
  }

  private ParsedCommand parseUseCalendar(String args) throws InvalidCommandException {
    String name = extractFlagValue(args, "--name");
    if (name == null || name.isEmpty()) {
      // Support shorthand: "use calendar <name>"
      name = stripQuotes(args.trim());
    }
    if (name == null || name.isEmpty()) {
      throw new InvalidCommandException("use calendar requires --name <calendar>");
    }
    UseCalendarParams params = new UseCalendarParams(name);
    return new ParsedCommand(CommandType.USE_CALENDAR, params);
  }

  private ParsedCommand parseCopyEventCommand(String args) throws InvalidCommandException {
    String trimmed = args.trim();
    String lower = trimmed.toLowerCase();

    int onIndex = lower.indexOf(" on ");
    if (onIndex == -1) {
      throw new InvalidCommandException("copy event requires 'on <datetime>' clause");
    }

    String subjectPart = trimmed.substring(0, onIndex).trim();
    String subject = stripQuotes(subjectPart);
    if (subject.isEmpty()) {
      throw new InvalidCommandException("Event name cannot be empty");
    }

    String afterOn = trimmed.substring(onIndex + 4).trim();
    String lowerAfterOn = afterOn.toLowerCase();
    int targetIndex = lowerAfterOn.indexOf("--target");
    if (targetIndex == -1) {
      throw new InvalidCommandException("copy event requires --target <calendar>");
    }

    String startStr = afterOn.substring(0, targetIndex).trim();
    LocalDateTime sourceStart;
    try {
      sourceStart = DateTimeParser.parseDateTime(startStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid source date/time: " + startStr, e);
    }

    String afterTarget = afterOn.substring(targetIndex + "--target".length()).trim();
    String lowerAfterTarget = afterTarget.toLowerCase();
    int toIndex = lowerAfterTarget.indexOf(" to ");
    if (toIndex == -1) {
      throw new InvalidCommandException("copy event requires 'to <datetime>' clause");
    }

    String targetCalendarPart = afterTarget.substring(0, toIndex).trim();
    String targetCalendar = stripQuotes(targetCalendarPart);
    String targetStartStr = afterTarget.substring(toIndex + 4).trim();

    LocalDateTime targetStart;
    try {
      targetStart = DateTimeParser.parseDateTime(targetStartStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid target date/time: " + targetStartStr, e);
    }

    CopyEventParams params = new CopyEventParams(
        subject, sourceStart, targetCalendar, targetStart);
    return new ParsedCommand(CommandType.COPY_EVENT, params);
  }

  private ParsedCommand parseCopyEventsOn(String args) throws InvalidCommandException {
    String trimmed = args.trim();
    String lower = trimmed.toLowerCase();
    int targetIndex = lower.indexOf("--target");
    if (targetIndex == -1) {
      throw new InvalidCommandException("copy events on requires --target <calendar>");
    }

    String sourceDateStr = trimmed.substring(0, targetIndex).trim();
    LocalDate sourceDate;
    try {
      sourceDate = DateTimeParser.parseDate(sourceDateStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid source date: " + sourceDateStr, e);
    }

    String afterTarget = trimmed.substring(targetIndex + "--target".length()).trim();
    String lowerAfterTarget = afterTarget.toLowerCase();
    int toIndex = lowerAfterTarget.indexOf(" to ");
    if (toIndex == -1) {
      throw new InvalidCommandException("copy events on requires 'to <date>' clause");
    }

    String targetCalendarPart = afterTarget.substring(0, toIndex).trim();
    String targetCalendar = stripQuotes(targetCalendarPart);
    String targetDateStr = afterTarget.substring(toIndex + 4).trim();

    LocalDate targetDate;
    try {
      targetDate = DateTimeParser.parseDate(targetDateStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid target date: " + targetDateStr, e);
    }

    CopyEventsOnParams params =
        new CopyEventsOnParams(sourceDate, targetCalendar, targetDate);
    return new ParsedCommand(CommandType.COPY_EVENTS_ON, params);
  }

  private ParsedCommand parseCopyEventsBetween(String args) throws InvalidCommandException {
    String trimmed = args.trim();
    String lower = trimmed.toLowerCase();

    int andIndex = lower.indexOf(" and ");
    if (andIndex == -1) {
      throw new InvalidCommandException("copy events between requires 'and <date>' clause");
    }

    String startStr = trimmed.substring(0, andIndex).trim();
    LocalDate startDate;
    try {
      startDate = DateTimeParser.parseDate(startStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid start date: " + startStr, e);
    }

    String afterAnd = trimmed.substring(andIndex + 5).trim();
    String lowerAfterAnd = afterAnd.toLowerCase();
    int targetIndex = lowerAfterAnd.indexOf("--target");
    if (targetIndex == -1) {
      throw new InvalidCommandException("copy events between requires --target <calendar>");
    }

    String endStr = afterAnd.substring(0, targetIndex).trim();
    LocalDate endDate;
    try {
      endDate = DateTimeParser.parseDate(endStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid end date: " + endStr, e);
    }

    String afterTarget = afterAnd.substring(targetIndex + "--target".length()).trim();
    String lowerAfterTarget = afterTarget.toLowerCase();
    int toIndex = lowerAfterTarget.indexOf(" to ");
    if (toIndex == -1) {
      throw new InvalidCommandException("copy events between requires 'to <date>' clause");
    }

    String targetCalendarPart = afterTarget.substring(0, toIndex).trim();
    String targetCalendar = stripQuotes(targetCalendarPart);
    String targetStartStr = afterTarget.substring(toIndex + 4).trim();

    LocalDate targetStartDate;
    try {
      targetStartDate = DateTimeParser.parseDate(targetStartStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid target date: " + targetStartStr, e);
    }

    CopyEventsBetweenParams params = CopyEventsBetweenParams.builder()
        .startDate(startDate)
        .endDate(endDate)
        .targetCalendar(targetCalendar)
        .targetStartDate(targetStartDate)
        .build();
    return new ParsedCommand(CommandType.COPY_EVENTS_BETWEEN, params);
  }

  private String extractFlagValue(String input, String flag) throws InvalidCommandException {
    if (input == null) {
      return null;
    }
    String lower = input.toLowerCase();
    int index = lower.indexOf(flag);
    if (index == -1) {
      return null;
    }
    int cursor = skipWhitespace(input, index + flag.length());
    if (cursor >= input.length()) {
      throw new InvalidCommandException("Missing value for " + flag);
    }

    if (input.charAt(cursor) == '"') {
      int closing = input.indexOf('"', cursor + 1);
      if (closing == -1) {
        throw new InvalidCommandException("Unclosed quote for " + flag);
      }
      return input.substring(cursor + 1, closing);
    }

    int end = cursor;
    while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
      end++;
    }
    return input.substring(cursor, end);
  }

  private String stripQuotes(String value) throws InvalidCommandException {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    if (trimmed.startsWith("\"")) {
      if (!trimmed.endsWith("\"") || trimmed.length() < 2) {
        throw new InvalidCommandException("Unclosed quote in value: " + value);
      }
      return trimmed.substring(1, trimmed.length() - 1);
    }
    return trimmed;
  }

  private int skipWhitespace(String input, int index) {
    int cursor = index;
    while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
      cursor++;
    }
    return cursor;
  }

  private ParsedCommand parseCreateCommand(String args) throws InvalidCommandException {
    int onIndex = args.indexOf(" on ");
    if (onIndex != -1 && !args.substring(0, onIndex).contains(" from ")) {
      return parseAllDayEvent(args);
    }

    String subject;
    String remainder;

    if (args.startsWith("\"")) {
      int endQuote = args.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new InvalidCommandException("Unclosed quote in subject");
      }
      subject = args.substring(1, endQuote);
      remainder = args.substring(endQuote + 1).trim();
    } else {
      int fromIndex = args.indexOf(" from ");
      if (fromIndex == -1) {
        throw new InvalidCommandException("Create command missing 'from' or 'on' keyword");
      }
      subject = args.substring(0, fromIndex).trim();
      remainder = args.substring(fromIndex).trim();
    }

    if (subject.isEmpty()) {
      throw new InvalidCommandException("Event subject cannot be empty");
    }

    if (!remainder.startsWith("from ")) {
      throw new InvalidCommandException("Expected 'from' after subject");
    }

    remainder = remainder.substring(5).trim();

    int toIndex = remainder.indexOf(" to ");
    if (toIndex == -1) {
      throw new InvalidCommandException("Create command missing 'to' keyword");
    }

    String startStr = remainder.substring(0, toIndex).trim();
    String afterTo = remainder.substring(toIndex + 4).trim();

    LocalDateTime start;
    LocalDateTime end;

    try {
      start = DateTimeParser.parseDateTime(startStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid start time: " + startStr, e);
    }

    int repeatsIndex = afterTo.indexOf(" repeats ");
    String endStr;
    String repeatsClause = null;

    if (repeatsIndex != -1) {
      endStr = afterTo.substring(0, repeatsIndex).trim();
      repeatsClause = afterTo.substring(repeatsIndex + 9).trim();
    } else {
      endStr = afterTo;
    }

    try {
      end = DateTimeParser.parseDateTime(endStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid end time: " + endStr, e);
    }

    if (end.isBefore(start) || end.equals(start)) {
      throw new InvalidCommandException("End time must be after start time");
    }

    if (repeatsClause == null) {
      CreateEventParams params = new CreateEventParams(subject, start, end);
      return new ParsedCommand(CommandType.CREATE_EVENT, params);
    }

    // For recurring events, a single instance must not span multiple days
    if (!end.toLocalDate().equals(start.toLocalDate())) {
      throw new InvalidCommandException("Recurring events must start and end on the same day");
    }

    return parseRecurrence(subject, start, end, repeatsClause);
  }

  private ParsedCommand parseAllDayEvent(String args) throws InvalidCommandException {
    String subject;
    String remainder;

    if (args.startsWith("\"")) {
      int endQuote = args.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new InvalidCommandException("Unclosed quote in subject");
      }
      subject = args.substring(1, endQuote);
      remainder = args.substring(endQuote + 1).trim();
    } else {
      int onIndex = args.indexOf(" on ");
      subject = args.substring(0, onIndex).trim();
      remainder = args.substring(onIndex).trim();
    }

    if (subject.isEmpty()) {
      throw new InvalidCommandException("Event subject cannot be empty");
    }

    if (!remainder.startsWith("on ")) {
      throw new InvalidCommandException("Expected 'on' after subject");
    }

    String afterOn = remainder.substring(3).trim();

    int repeatsIndex = afterOn.indexOf(" repeats ");
    String dateStr;
    String repeatsClause = null;

    if (repeatsIndex != -1) {
      dateStr = afterOn.substring(0, repeatsIndex).trim();
      repeatsClause = afterOn.substring(repeatsIndex + 9).trim();
    } else {
      dateStr = afterOn;
    }

    LocalDate date;
    try {
      date = DateTimeParser.parseDate(dateStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid date: " + dateStr, e);
    }

    // README: All Day Event is defined as 8am to 5pm
    LocalDateTime start = date.atTime(8, 0);
    LocalDateTime end = date.atTime(17, 0);

    if (repeatsClause == null) {
      CreateEventParams params = CreateEventParams.builder(subject, start, end).build();
      return new ParsedCommand(CommandType.CREATE_EVENT, params);
    }

    return parseRecurrence(subject, start, end, repeatsClause);
  }

  /**
   * Parses recurrence pattern.
   * Supports formats:
   * - "MWF for 5 times" or "MWF 5 times"
   * - "MWF until 2025-02-01"
   */
  private ParsedCommand parseRecurrence(String subject, LocalDateTime start,
                                        LocalDateTime end, String repeatsClause)
      throws InvalidCommandException {

    // Remove optional "for" keyword to normalize format
    String normalized = repeatsClause.replace(" for ", " ").trim();

    String[] parts = normalized.split("\\s+");
    if (parts.length < 2) {
      throw new InvalidCommandException("Invalid repeats clause: " + repeatsClause);
    }

    // Parse weekdays (first part)
    String daysStr = parts[0];
    Weekday[] days;
    try {
      days = Weekday.parseString(daysStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid weekday format: " + daysStr, e);
    }

    // Parse count or until (remaining parts)
    Integer repeatCount = null;
    LocalDateTime repeatUntil = null;

    if (parts[1].equals("until")) {
      // Format: "MWF until 2025-02-01"
      if (parts.length < 3) {
        throw new InvalidCommandException("Missing date after 'until'");
      }
      try {
        LocalDate untilDate = DateTimeParser.parseDate(parts[2]);
        repeatUntil = untilDate.atTime(23, 59);
      } catch (IllegalArgumentException e) {
        throw new InvalidCommandException("Invalid until date: " + parts[2], e);
      }
    } else {
      // Format: "MWF 5 times" (after "for" has been removed)
      try {
        repeatCount = Integer.parseInt(parts[1]);
        if (repeatCount <= 0) {
          throw new InvalidCommandException("Repeat count must be positive");
        }
      } catch (NumberFormatException e) {
        throw new InvalidCommandException("Invalid repeat count: " + parts[1], e);
      }
    }

    CreateEventParams.Builder builder = CreateEventParams.builder(subject, start, end)
        .withRecurrence(days);
    
    if (repeatCount != null) {
      builder.repeatCount(repeatCount);
    } else if (repeatUntil != null) {
      builder.repeatUntil(repeatUntil);
    }
    
    CreateEventParams params = builder.build();
    return new ParsedCommand(CommandType.CREATE_EVENT, params);
  }

  private ParsedCommand parseEditCommand(String args) throws InvalidCommandException {
    // Parse edit command format:
    // edit event <property> <subject> from <datetime> with <newValue>
    // edit events <property> <subject> from <datetime> with <newValue> (for series from date)
    // edit series <property> <subject> from <datetime> with <newValue> (for entire series)

    String trimmed = args.trim();

    // Determine edit type
    CommandType editType;
    if (trimmed.startsWith("event ")) {
      editType = CommandType.EDIT_EVENT;
      trimmed = trimmed.substring(6).trim();
    } else if (trimmed.startsWith("events ")) {
      editType = CommandType.EDIT_SERIES;
      trimmed = trimmed.substring(7).trim();
    } else if (trimmed.startsWith("series ")) {
      editType = CommandType.EDIT_ALL_SERIES;
      trimmed = trimmed.substring(7).trim();
    } else {
      throw new InvalidCommandException("Edit command must specify 'event', 'events', or 'series'");
    }

    // Extract property (first word)
    int firstSpace = trimmed.indexOf(' ');
    if (firstSpace == -1) {
      throw new InvalidCommandException("Edit command missing property");
    }

    String property = trimmed.substring(0, firstSpace).trim();
    // Extract property BEFORE modifying trimmed
    trimmed = trimmed.substring(firstSpace + 1).trim();

    // Validate property
    if (!property.matches("subject|start|end|description|location|status")) {
      throw new InvalidCommandException("Invalid property: "
          + property + ". Must be one of: subject, start, end, description, location, status");
    }

    // Extract subject (may be quoted)
    String subject;
    String remainder;

    if (trimmed.startsWith("\"")) {
      int endQuote = trimmed.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new InvalidCommandException("Unclosed quote in subject");
      }
      subject = trimmed.substring(1, endQuote);
      remainder = trimmed.substring(endQuote + 1).trim();
    } else {
      int fromIndex = trimmed.indexOf(" from ");
      if (fromIndex == -1) {
        throw new InvalidCommandException("Edit command missing 'from' keyword");
      }
      subject = trimmed.substring(0, fromIndex).trim();
      remainder = trimmed.substring(fromIndex).trim();
    }

    // Extract datetime
    if (!remainder.startsWith("from ")) {
      throw new InvalidCommandException("Expected 'from' after subject");
    }
    remainder = remainder.substring(5).trim();

    int withIndex = remainder.indexOf(" with ");
    if (withIndex == -1) {
      throw new InvalidCommandException("Edit command missing 'with' keyword");
    }

    String datetimeStr = remainder.substring(0, withIndex).trim();
    String newValue = remainder.substring(withIndex + 6).trim();

    // Parse the datetime
    LocalDateTime datetime;
    try {
      datetime = DateTimeParser.parseDateTime(datetimeStr);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid datetime: " + datetimeStr, e);
    }

    // Remove quotes from new value if present
    if (newValue.startsWith("\"") && newValue.endsWith("\"") && newValue.length() > 1) {
      newValue = newValue.substring(1, newValue.length() - 1);
    }

    // Create appropriate params based on edit type
    EditEventParams params = EditEventParams.builder()
        .subject(subject)
        .dateTime(datetime)
        .property(property)
        .newValue(newValue)
        .build();
    return new ParsedCommand(editType, params);
  }

  /**
   * Parses print commands that could be either date or range format.
   * This handles the general "print" prefix.
   */
  private ParsedCommand parsePrintCommand(String args) throws InvalidCommandException {
    String trimmed = args.trim();

    if (trimmed.startsWith("from ")) {
      return parsePrintRangeCommand(trimmed.substring(5));
    } else {
      return parsePrintOnCommand(trimmed);
    }
  }

  /**
   * Parses print commands for a specific date.
   * Input should be just the date string.
   */
  private ParsedCommand parsePrintOnCommand(String dateStr) throws InvalidCommandException {
    try {
      LocalDate date = DateTimeParser.parseDate(dateStr.trim());
      PrintDateParams params = new PrintDateParams(date);
      return new ParsedCommand(CommandType.PRINT_DATE, params);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid date format: " + dateStr, e);
    }
  }

  /**
   * Parses print range commands.
   * Input should be the range string starting after "from" keyword.
   */
  private ParsedCommand parsePrintRangeCommand(String rangeStr) throws InvalidCommandException {
    int toIndex = rangeStr.indexOf(" to ");
    if (toIndex == -1) {
      throw new InvalidCommandException("Print range missing 'to' keyword");
    }

    String startStr = rangeStr.substring(0, toIndex).trim();
    String endStr = rangeStr.substring(toIndex + 4).trim();

    try {
      // First try to parse as DateTime
      LocalDateTime startDateTime = null;
      LocalDateTime endDateTime = null;

      try {
        startDateTime = DateTimeParser.parseDateTime(startStr);
        endDateTime = DateTimeParser.parseDateTime(endStr);
      } catch (IllegalArgumentException e) {
        // If DateTime parsing fails, fall back to Date
      }

      if (startDateTime != null && endDateTime != null) {
        // Convert DateTime to Date for the range (take just the date part)
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        PrintRangeParams params = new PrintRangeParams(startDate, endDate);
        return new ParsedCommand(CommandType.PRINT_RANGE, params);
      } else {
        // Parse as dates
        LocalDate startDate = DateTimeParser.parseDate(startStr);
        LocalDate endDate = DateTimeParser.parseDate(endStr);
        PrintRangeParams params = new PrintRangeParams(startDate, endDate);
        return new ParsedCommand(CommandType.PRINT_RANGE, params);
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid date format in print range", e);
    }
  }

  private ParsedCommand parseViewDayCommand(String args) throws InvalidCommandException {
    ContextExtraction extraction = extractContext(args);
    String base = extraction.getRemainder();
    if (base.isEmpty()) {
      throw new InvalidCommandException("view day requires a date");
    }
    try {
      LocalDate date = DateTimeParser.parseDate(base.trim());
      ViewDateParams params = new ViewDateParams(date);
      return new ParsedCommand(CommandType.VIEW_DAY, params, extraction.getContext());
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid date format: " + base, e);
    }
  }

  private ParsedCommand parseViewWeekCommand(String args) throws InvalidCommandException {
    ContextExtraction extraction = extractContext(args);
    String base = extraction.getRemainder();
    if (base.isEmpty()) {
      throw new InvalidCommandException("view week requires a date");
    }
    try {
      LocalDate startDate = DateTimeParser.parseDate(base.trim());
      ViewDateParams params = new ViewDateParams(startDate);
      return new ParsedCommand(CommandType.VIEW_WEEK, params, extraction.getContext());
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid date format: " + base, e);
    }
  }

  private ParsedCommand parseViewMonthCommand(String args) throws InvalidCommandException {
    ContextExtraction extraction = extractContext(args);
    String base = extraction.getRemainder();
    if (base.isEmpty()) {
      throw new InvalidCommandException("view month requires a date");
    }
    try {
      String trimmed = base.trim();
      if (trimmed.matches("\\d{4}-\\d{2}")) {
        trimmed = trimmed + "-01";
      }
      LocalDate date = DateTimeParser.parseDate(trimmed);
      ViewDateParams params = new ViewDateParams(date);
      return new ParsedCommand(CommandType.VIEW_MONTH, params, extraction.getContext());
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid month format. Use YYYY-MM: " + base, e);
    }
  }

  private ParsedCommand parseStatusCommand(String args) throws InvalidCommandException {
    try {
      LocalDateTime dateTime = DateTimeParser.parseDateTime(args.trim());
      StatusParams params = new StatusParams(dateTime);
      return new ParsedCommand(CommandType.STATUS, params);
    } catch (IllegalArgumentException e) {
      throw new InvalidCommandException("Invalid datetime format: " + args, e);
    }
  }

  private ParsedCommand parseExportCommand(String args) throws InvalidCommandException {
    ContextExtraction extraction = extractContext(args);
    String working = extraction.getRemainder();
    String trimmed = working.trim();
    if (trimmed.isEmpty()) {
      throw new InvalidCommandException("Export requires a filename");
    }

    String calendarName = null;
    String filename;

    if (trimmed.startsWith("\"")) {
      int endQuote = trimmed.indexOf("\"", 1);
      if (endQuote == -1) {
        throw new InvalidCommandException("Unclosed quote in calendar name");
      }
      calendarName = trimmed.substring(1, endQuote);
      filename = trimmed.substring(endQuote + 1).trim();

      if (filename.isEmpty()) {
        throw new InvalidCommandException("Export filename cannot be empty");
      }
    } else {
      String[] parts = trimmed.split("\\s+", 2);
      if (parts.length == 1) {
        filename = parts[0];
      } else {
        calendarName = parts[0];
        filename = parts[1].trim();
      }
    }

    if (filename.isEmpty()) {
      throw new InvalidCommandException("Export filename cannot be empty");
    }

    ExportParams params = new ExportParams(filename);
    return new ParsedCommand(CommandType.EXPORT, params,
        calendarName == null || calendarName.isEmpty()
            ? extraction.getContext()
            : CommandContext.of(calendarName, extraction.getContext().getZoneId().orElse(null)));
  }

  /**
   * Extracts trailing context clauses (ON &lt;calendar&gt;, AT TZ &lt;zoneId&gt;).
   */
  private ContextExtraction extractContext(String input) throws InvalidCommandException {
    if (input == null) {
      return new ContextExtraction("", CommandContext.empty());
    }
    String working = input.trim();
    String calendar = null;
    ZoneId zoneId = null;
    boolean changed = true;
    while (changed) {
      changed = false;

      Matcher tzMatcher = AT_TZ_PATTERN.matcher(working);
      if (tzMatcher.find()) {
        String zoneToken = tzMatcher.group(1).trim();
        String zoneValue = stripQuotes(zoneToken);
        try {
          zoneId = ZoneId.of(zoneValue);
        } catch (Exception e) {
          throw new InvalidCommandException("Unsupported timezone: " + zoneValue, e);
        }
        working = working.substring(0, tzMatcher.start()).trim();
        changed = true;
        continue;
      }

      Matcher onMatcher = ON_PATTERN.matcher(working);
      if (onMatcher.find()) {
        String calendarToken = onMatcher.group(1).trim();
        String calendarValue = stripQuotes(calendarToken);
        if (calendarValue.isEmpty()) {
          throw new InvalidCommandException("Calendar name after ON cannot be empty");
        }
        calendar = calendarValue;
        working = working.substring(0, onMatcher.start()).trim();
        changed = true;
      }
    }
    return new ContextExtraction(working.trim(), CommandContext.of(calendar, zoneId));
  }

  private static final class ContextExtraction {
    private final String remainder;
    private final CommandContext context;

    private ContextExtraction(String remainder, CommandContext context) {
      this.remainder = remainder == null ? "" : remainder;
      this.context = context == null ? CommandContext.empty() : context;
    }

    private String getRemainder() {
      return remainder;
    }

    private CommandContext getContext() {
      return context;
    }
  }
}
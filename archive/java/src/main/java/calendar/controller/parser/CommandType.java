package calendar.controller.parser;

/**
 * Enum representing command types.
 */
public enum CommandType {
  CREATE_CALENDAR,
  EDIT_CALENDAR,
  USE_CALENDAR,
  CREATE_EVENT,
  EDIT_EVENT,
  EDIT_SERIES,
  EDIT_ALL_SERIES,
  PRINT_DATE,
  PRINT_RANGE,
  VIEW_DAY,
  VIEW_WEEK,
  VIEW_MONTH,
  STATUS,
  COPY_EVENT,
  COPY_EVENTS_ON,
  COPY_EVENTS_BETWEEN,
  EXPORT,
  EXIT
}
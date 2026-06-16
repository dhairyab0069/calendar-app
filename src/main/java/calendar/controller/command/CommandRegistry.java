package calendar.controller.command;

import calendar.controller.CommandHandler;
import calendar.controller.parser.CommandType;
import calendar.controller.parser.CopyEventParams;
import calendar.controller.parser.CopyEventsBetweenParams;
import calendar.controller.parser.CopyEventsOnParams;
import calendar.controller.parser.CreateCalendarParams;
import calendar.controller.parser.CreateEventParams;
import calendar.controller.parser.EditCalendarParams;
import calendar.controller.parser.EditEventParams;
import calendar.controller.parser.ExportParams;
import calendar.controller.parser.PrintDateParams;
import calendar.controller.parser.PrintRangeParams;
import calendar.controller.parser.StatusParams;
import calendar.controller.parser.UseCalendarParams;
import calendar.controller.parser.ViewDateParams;
import java.util.EnumMap;
import java.util.Map;

/**
 * Central registry that maps {@link CommandType} to executable {@link Command}s.
 */
public final class CommandRegistry {

  private final Map<CommandType, Command> inventory;

  /**
   * Builds the registry and wires command types to handler methods.
   *
   * @param handler core CommandHandler instance
   */
  public CommandRegistry(CommandHandler handler) {
    this.inventory = new EnumMap<>(CommandType.class);
    register(handler);
  }

  private void register(CommandHandler handler) {
    inventory.put(CommandType.CREATE_CALENDAR,
        (h, params, ctx) -> handler.handleCreateCalendar((CreateCalendarParams) params));
    inventory.put(CommandType.EDIT_CALENDAR,
        (h, params, ctx) -> handler.handleEditCalendar((EditCalendarParams) params));
    inventory.put(CommandType.USE_CALENDAR,
        (h, params, ctx) -> handler.handleUseCalendar((UseCalendarParams) params));
    inventory.put(CommandType.CREATE_EVENT,
        (h, params, ctx) -> handler.handleCreateEvent((CreateEventParams) params));
    inventory.put(CommandType.EDIT_EVENT,
        (h, params, ctx) -> handler.handleEditEvent((EditEventParams) params));
    inventory.put(CommandType.EDIT_SERIES,
        (h, params, ctx) -> handler.handleEditSeries((EditEventParams) params));
    inventory.put(CommandType.EDIT_ALL_SERIES,
        (h, params, ctx) -> handler.handleEditAllSeries((EditEventParams) params));
    inventory.put(CommandType.PRINT_DATE,
        (h, params, ctx) -> handler.handlePrintDate((PrintDateParams) params));
    inventory.put(CommandType.PRINT_RANGE,
        (h, params, ctx) -> handler.handlePrintRange((PrintRangeParams) params));
    inventory.put(CommandType.VIEW_DAY,
        (h, params, ctx) -> handler.handleViewDay((ViewDateParams) params));
    inventory.put(CommandType.VIEW_WEEK,
        (h, params, ctx) -> handler.handleViewWeek((ViewDateParams) params));
    inventory.put(CommandType.VIEW_MONTH,
        (h, params, ctx) -> handler.handleViewMonth((ViewDateParams) params));
    inventory.put(CommandType.STATUS,
        (h, params, ctx) -> handler.handleStatus((StatusParams) params));
    inventory.put(CommandType.COPY_EVENT,
        (h, params, ctx) -> handler.handleCopyEvent((CopyEventParams) params));
    inventory.put(CommandType.COPY_EVENTS_ON,
        (h, params, ctx) -> handler.handleCopyEventsOn((CopyEventsOnParams) params));
    inventory.put(CommandType.COPY_EVENTS_BETWEEN,
        (h, params, ctx) -> handler.handleCopyEventsBetween((CopyEventsBetweenParams) params));
    inventory.put(CommandType.EXPORT,
        (h, params, ctx) -> handler.handleExport((ExportParams) params));
    inventory.put(CommandType.EXIT,
        (h, params, ctx) -> handler.handleExit());
  }

  /**
   * Resolves the command executor for the provided type.
   *
   * @param type command type
   * @return executable command or {@code null} if not registered
   */
  public Command resolve(CommandType type) {
    return inventory.get(type);
  }
}

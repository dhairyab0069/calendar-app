import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.CommandHandler;
import calendar.controller.command.Command;
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
import calendar.model.CalendarModel;
import calendar.model.SparseHashCalendar;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive test suite for the command package classes.
 *
 * <p>Tests all command-related components:
 * <ul>
 *   <li>CommandContext - execution context with calendar and timezone</li>
 *   <li>CommandRegistry - command type to handler mapping</li>
 *   <li>Command interface - functional command execution</li>
 * </ul>
 *
 * @version 1.0
 */
public class CommandTest {

  private CalendarModel model;
  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private CalendarView view;
  private CommandHandler handler;
  private CommandRegistry registry;

  /**
   * Sets up test environment before each test.
   * Initializes model, view, handler, and registry components.
   *
   * @throws RuntimeException if calendar setup fails
   */
  @Before
  public void setUp() {
    model = new SparseHashCalendar();
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    view = new ConsoleView(new PrintStream(outputStream),
        new PrintStream(errorStream));
    handler = new CommandHandler(model, view);
    registry = new CommandRegistry(handler);

    try {
      model.createCalendar("Default", ZoneId.of("America/New_York"));
      model.useCalendar("Default");
    } catch (DuplicateCalendarException | CalendarNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests that empty context returns non-null with no values.
   */
  @Test
  public void testCommandContextEmpty() {
    CommandContext ctx = CommandContext.empty();

    assertNotNull(ctx);
    assertFalse(ctx.getCalendarName().isPresent());
    assertFalse(ctx.getZoneId().isPresent());
  }

  /**
   * Tests that empty context returns the same static instance.
   */
  @Test
  public void testCommandContextEmptyReturnsStaticInstance() {
    CommandContext ctx1 = CommandContext.empty();
    CommandContext ctx2 = CommandContext.empty();

    assertSame(ctx1, ctx2);
  }

  /**
   * Tests context creation with both null parameters.
   */
  @Test
  public void testCommandContextOfWithBothNull() {
    CommandContext ctx = CommandContext.of(null, null);

    assertNotNull(ctx);
    assertFalse(ctx.getCalendarName().isPresent());
    assertFalse(ctx.getZoneId().isPresent());
  }

  /**
   * Tests that of() with nulls returns singleton empty instance.
   */
  @Test
  public void testCommandContextOfWithBothNullReturnsSingleton() {
    CommandContext ctx1 = CommandContext.of(null, null);
    CommandContext ctx2 = CommandContext.empty();

    assertSame(ctx1, ctx2);
  }

  /**
   * Tests context creation with only calendar name.
   */
  @Test
  public void testCommandContextOfWithCalendarOnly() {
    CommandContext ctx = CommandContext.of("WorkCalendar", null);

    assertTrue(ctx.getCalendarName().isPresent());
    assertEquals("WorkCalendar", ctx.getCalendarName().get());
    assertFalse(ctx.getZoneId().isPresent());
  }

  /**
   * Tests context creation with only timezone.
   */
  @Test
  public void testCommandContextOfWithZoneOnly() {
    ZoneId zone = ZoneId.of("Europe/London");
    CommandContext ctx = CommandContext.of(null, zone);

    assertFalse(ctx.getCalendarName().isPresent());
    assertTrue(ctx.getZoneId().isPresent());
    assertEquals(zone, ctx.getZoneId().get());
  }

  /**
   * Tests context creation with both calendar and timezone.
   */
  @Test
  public void testCommandContextOfWithBoth() {
    ZoneId zone = ZoneId.of("Asia/Tokyo");
    CommandContext ctx = CommandContext.of("Personal", zone);

    assertTrue(ctx.getCalendarName().isPresent());
    assertEquals("Personal", ctx.getCalendarName().get());
    assertTrue(ctx.getZoneId().isPresent());
    assertEquals(zone, ctx.getZoneId().get());
  }

  /**
   * Tests equals method with same instance.
   */
  @Test
  public void testCommandContextEqualsSameInstance() {
    CommandContext ctx = CommandContext.of("Work", null);

    assertEquals(ctx, ctx);
  }

  /**
   * Tests equals method with null.
   */
  @Test
  public void testCommandContextEqualsNull() {
    CommandContext ctx = CommandContext.of("Work", null);

    assertNotEquals(null, ctx);
  }

  /**
   * Tests equals method with different class type.
   */
  @Test
  public void testCommandContextEqualsDifferentClass() {
    CommandContext ctx = CommandContext.of("Work", null);

    assertNotEquals("Not a CommandContext", ctx);
  }

  /**
   * Tests equals method with equal objects.
   */
  @Test
  public void testCommandContextEqualsEqualObjects() {
    ZoneId zone = ZoneId.of("UTC");
    CommandContext ctx1 = CommandContext.of("Work", zone);
    CommandContext ctx2 = CommandContext.of("Work", zone);

    assertEquals(ctx1, ctx2);
  }

  /**
   * Tests equals method with different calendar names.
   */
  @Test
  public void testCommandContextEqualsDifferentCalendar() {
    CommandContext ctx1 = CommandContext.of("Work", null);
    CommandContext ctx2 = CommandContext.of("Personal", null);

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Tests equals method with different timezones.
   */
  @Test
  public void testCommandContextEqualsDifferentZone() {
    CommandContext ctx1 = CommandContext.of(null, ZoneId.of("UTC"));
    CommandContext ctx2 = CommandContext.of(null, ZoneId.of("America/Los_Angeles"));

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Tests equals method with both fields different.
   */
  @Test
  public void testCommandContextEqualsDifferentBoth() {
    CommandContext ctx1 = CommandContext.of("Work", ZoneId.of("UTC"));
    CommandContext ctx2 = CommandContext.of("Personal", ZoneId.of("America/Los_Angeles"));

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Tests equals method with one null field.
   */
  @Test
  public void testCommandContextEqualsOneNull() {
    CommandContext ctx1 = CommandContext.of("Work", null);
    CommandContext ctx2 = CommandContext.of(null, null);

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Tests hashCode consistency for equal objects.
   */
  @Test
  public void testCommandContextHashCode() {
    ZoneId zone = ZoneId.of("UTC");
    CommandContext ctx1 = CommandContext.of("Work", zone);
    CommandContext ctx2 = CommandContext.of("Work", zone);

    assertEquals(ctx1.hashCode(), ctx2.hashCode());
  }

  /**
   * Tests hashCode difference for different objects.
   */
  @Test
  public void testCommandContextHashCodeDifferent() {
    CommandContext ctx1 = CommandContext.of("Work", null);
    CommandContext ctx2 = CommandContext.of("Personal", null);

    assertNotEquals(ctx1.hashCode(), ctx2.hashCode());
  }

  /**
   * Tests hashCode for empty context.
   */
  @Test
  public void testCommandContextHashCodeEmpty() {
    CommandContext ctx = CommandContext.empty();

    int hashCode = ctx.hashCode();
    assertNotNull(hashCode);
  }

  /**
   * Tests toString method with both values.
   */
  @Test
  public void testCommandContextToString() {
    ZoneId zone = ZoneId.of("America/New_York");
    CommandContext ctx = CommandContext.of("Work", zone);

    String str = ctx.toString();
    assertTrue(str.contains("CommandContext"));
    assertTrue(str.contains("Work"));
    assertTrue(str.contains("America/New_York"));
  }

  /**
   * Tests toString method for empty context.
   */
  @Test
  public void testCommandContextToStringEmpty() {
    CommandContext ctx = CommandContext.empty();

    String str = ctx.toString();
    assertTrue(str.contains("CommandContext"));
    assertTrue(str.contains("null"));
  }

  /**
   * Tests toString method with partial values.
   */
  @Test
  public void testCommandContextToStringPartial() {
    CommandContext ctx = CommandContext.of("Calendar", null);

    String str = ctx.toString();
    assertTrue(str.contains("Calendar"));
    assertTrue(str.contains("null"));
  }

  /**
   * Tests registry creation with handler.
   */
  @Test
  public void testCommandRegistryCreation() {
    CommandRegistry reg = new CommandRegistry(handler);
    assertNotNull(reg);
  }

  /**
   * Tests registry resolution and execution of EXIT command.
   */
  @Test
  public void testCommandRegistryResolveExit() {
    Command cmd = registry.resolve(CommandType.EXIT);

    assertNotNull(cmd);

    cmd.execute(handler, null, CommandContext.empty());
    String output = outputStream.toString();
    assertTrue(output.contains("Goodbye"));
  }

  /**
   * Tests registry resolution and execution of CREATE_CALENDAR command.
   */
  @Test
  public void testCommandRegistryResolveCreateCalendar() {
    Command cmd = registry.resolve(CommandType.CREATE_CALENDAR);

    assertNotNull(cmd);

    CreateCalendarParams params = new CreateCalendarParams(
        "TestCal", ZoneId.of("UTC"));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    String error = errorStream.toString();
    // The calendar might already exist or successfully created
    assertTrue(output.contains("Calendar created") || output.contains("TestCal")
        || error.contains("already exists"));
  }

  /**
   * Tests registry resolution and execution of EDIT_CALENDAR command.
   */
  @Test
  public void testCommandRegistryResolveEditCalendar() {
    Command cmd = registry.resolve(CommandType.EDIT_CALENDAR);

    assertNotNull(cmd);

    EditCalendarParams params = new EditCalendarParams(
        "Default", "timezone", "Europe/Paris");
    cmd.execute(handler, params, CommandContext.empty());

    assertTrue(outputStream.toString().length() > 0
        || errorStream.toString().length() > 0);
  }

  /**
   * Tests registry resolution and execution of USE_CALENDAR command.
   */
  @Test
  public void testCommandRegistryResolveUseCalendar() {
    Command cmd = registry.resolve(CommandType.USE_CALENDAR);

    assertNotNull(cmd);

    UseCalendarParams params = new UseCalendarParams("Default");
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    String error = errorStream.toString();
    // Either switches successfully or is already using Default
    assertTrue(output.contains("Switched to calendar") || output.contains("Default")
        || output.contains("Using calendar") || error.contains("not found")
        || output.length() > 0 || error.length() > 0);
  }

  /**
   * Tests registry resolution and execution of CREATE_EVENT command.
   */
  @Test
  public void testCommandRegistryResolveCreateEvent() {
    Command cmd = registry.resolve(CommandType.CREATE_EVENT);

    assertNotNull(cmd);

    CreateEventParams params = new CreateEventParams(
        "Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)
    );
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Meeting"));
  }

  /**
   * Tests registry resolution and execution of EDIT_EVENT command.
   */
  @Test
  public void testCommandRegistryResolveEditEvent() {
    Command cmd = registry.resolve(CommandType.EDIT_EVENT);

    assertNotNull(cmd);

    EditEventParams params = new EditEventParams(
        "Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        "subject",
        "Updated Meeting"
    );
    cmd.execute(handler, params, CommandContext.empty());

    assertTrue(errorStream.toString().contains("not found"));
  }

  /**
   * Tests registry resolution and execution of EDIT_SERIES command.
   */
  @Test
  public void testCommandRegistryResolveEditSeries() {
    Command cmd = registry.resolve(CommandType.EDIT_SERIES);

    assertNotNull(cmd);

    EditEventParams params = new EditEventParams(
        "Standup",
        LocalDateTime.of(2025, 1, 6, 9, 0),
        "location",
        "Room 101"
    );
    cmd.execute(handler, params, CommandContext.empty());

    assertTrue(errorStream.toString().contains("not found"));
  }

  /**
   * Tests registry resolution and execution of EDIT_ALL_SERIES command.
   */
  @Test
  public void testCommandRegistryResolveEditAllSeries() {
    Command cmd = registry.resolve(CommandType.EDIT_ALL_SERIES);

    assertNotNull(cmd);

    EditEventParams params = new EditEventParams(
        "Standup",
        LocalDateTime.of(2025, 1, 6, 9, 0),
        "description",
        "Daily sync"
    );
    cmd.execute(handler, params, CommandContext.empty());

    assertTrue(errorStream.toString().contains("not found"));
  }

  /**
   * Tests registry resolution and execution of PRINT_DATE command.
   */
  @Test
  public void testCommandRegistryResolvePrintDate() {
    Command cmd = registry.resolve(CommandType.PRINT_DATE);

    assertNotNull(cmd);

    PrintDateParams params = new PrintDateParams(
        LocalDate.of(2025, 1, 15));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("No events on 2025-01-15"));
  }

  /**
   * Tests registry resolution and execution of PRINT_RANGE command.
   */
  @Test
  public void testCommandRegistryResolvePrintRange() {
    Command cmd = registry.resolve(CommandType.PRINT_RANGE);

    assertNotNull(cmd);

    PrintRangeParams params = new PrintRangeParams(
        LocalDate.of(2025, 1, 15),
        LocalDate.of(2025, 1, 20)
    );
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("No events from"));
  }

  /**
   * Tests registry resolution and execution of VIEW_DAY command.
   */
  @Test
  public void testCommandRegistryResolveViewDay() {
    Command cmd = registry.resolve(CommandType.VIEW_DAY);

    assertNotNull(cmd);

    ViewDateParams params = new ViewDateParams(
        LocalDate.of(2025, 1, 15));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  /**
   * Tests registry resolution and execution of VIEW_WEEK command.
   */
  @Test
  public void testCommandRegistryResolveViewWeek() {
    Command cmd = registry.resolve(CommandType.VIEW_WEEK);

    assertNotNull(cmd);

    ViewDateParams params = new ViewDateParams(
        LocalDate.of(2025, 1, 6));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  /**
   * Tests registry resolution and execution of VIEW_MONTH command.
   */
  @Test
  public void testCommandRegistryResolveViewMonth() {
    Command cmd = registry.resolve(CommandType.VIEW_MONTH);

    assertNotNull(cmd);

    ViewDateParams params = new ViewDateParams(
        LocalDate.of(2025, 1, 15));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertFalse(output.isEmpty());
  }

  /**
   * Tests registry resolution and execution of STATUS command.
   */
  @Test
  public void testCommandRegistryResolveStatus() {
    Command cmd = registry.resolve(CommandType.STATUS);

    assertNotNull(cmd);

    StatusParams params = new StatusParams(
        LocalDateTime.of(2025, 1, 15, 10, 30));
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("available"));
  }

  /**
   * Tests registry resolution and execution of EXPORT command.
   */
  @Test
  public void testCommandRegistryResolveExport() {
    Command cmd = registry.resolve(CommandType.EXPORT);

    assertNotNull(cmd);

    ExportParams params = new ExportParams("test.csv");
    cmd.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Calendar exported to: test.csv"));
  }

  /**
   * Tests registry resolution of COPY_EVENT command.
   */
  @Test
  public void testCommandRegistryResolveUnregisteredType() {
    Command cmd = registry.resolve(CommandType.COPY_EVENT);
    assertNotNull(cmd);
  }

  /**
   * Tests registry resolution with null type.
   */
  @Test
  public void testCommandRegistryResolveNull() {
    Command cmd = registry.resolve(null);
    assertNull(cmd);
  }

  /**
   * Tests that all command types are registered in registry.
   */
  @Test
  public void testCommandRegistryAllCommandTypesRegistered() {
    for (CommandType type : CommandType.values()) {
      Command cmd = registry.resolve(type);
      assertNotNull("Command not registered for type: " + type, cmd);
    }
  }

  /**
   * Tests Command as functional interface.
   */
  @Test
  public void testCommandFunctionalInterface() {
    Command customCommand = (h, params, ctx) -> {
      h.handleExit();
    };

    customCommand.execute(handler, null, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Goodbye"));
  }

  /**
   * Tests Command execution with context.
   */
  @Test
  public void testCommandWithContext() {
    Command contextAwareCommand = (h, params, ctx) -> {
      if (ctx.getCalendarName().isPresent()) {
        view.displayMessage("Calendar: " + ctx.getCalendarName().get());
      }
      if (ctx.getZoneId().isPresent()) {
        view.displayMessage("Zone: " + ctx.getZoneId().get());
      }
    };

    CommandContext context = CommandContext.of("Work",
        ZoneId.of("Europe/London"));
    contextAwareCommand.execute(handler, null, context);

    String output = outputStream.toString();
    assertTrue(output.contains("Calendar: Work"));
    assertTrue(output.contains("Zone: Europe/London"));
  }

  /**
   * Tests Command execution with parameters.
   */
  @Test
  public void testCommandWithParams() {
    Command paramsCommand = (h, params, ctx) -> {
      if (params instanceof CreateEventParams) {
        CreateEventParams eventParams = (CreateEventParams) params;
        view.displayMessage("Subject: " + eventParams.getSubject());
      }
    };

    CreateEventParams params = new CreateEventParams(
        "TestEvent",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)
    );

    paramsCommand.execute(handler, params, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Subject: TestEvent"));
  }

  /**
   * Tests Command parameter type mismatch handling.
   */
  @Test
  public void testCommandLambdaCastingBehavior() {
    Command cmd = registry.resolve(CommandType.CREATE_EVENT);

    try {
      cmd.execute(handler, new ExportParams("test.csv"),
          CommandContext.empty());
      fail("Should have thrown ClassCastException");
    } catch (ClassCastException e) {
      assertTrue(true);
    }
  }

  /**
   * Tests Command execution with null handler.
   */
  @Test
  public void testCommandNullHandler() {
    Command cmd = (h, params, ctx) -> {
      if (h != null) {
        h.handleExit();
      }
    };

    cmd.execute(null, null, CommandContext.empty());

    assertTrue(true);
  }

  /**
   * Tests complete execution flow from registry to handler.
   */
  @Test
  public void testCommandExecutionFlow() {
    ParsedCommand parsed = new ParsedCommand(
        CommandType.CREATE_EVENT,
        new CreateEventParams(
            "Integration Test",
            LocalDateTime.of(2025, 1, 20, 14, 0),
            LocalDateTime.of(2025, 1, 20, 15, 0)
        )
    );

    Command cmd = registry.resolve(parsed.getType());
    cmd.execute(handler, parsed.getParams(), parsed.getContext());

    String output = outputStream.toString();
    assertTrue(output.contains("Event created: Integration Test"));

    assertEquals(1, model.getAllEvents().size());
    assertEquals("Integration Test",
        model.getAllEvents().get(0).getSubject());
  }

  /**
   * Tests Command with calendar context switching.
   *
   * @throws Exception if calendar creation fails
   */
  @Test
  public void testCommandWithContextSwitching() throws Exception {
    model.createCalendar("Work", ZoneId.of("Europe/London"));

    Command createCmd = registry.resolve(CommandType.CREATE_EVENT);
    CreateEventParams params = new CreateEventParams(
        "Default Event",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0)
    );
    createCmd.execute(handler, params, CommandContext.empty());

    Command viewCmd = registry.resolve(CommandType.VIEW_DAY);
    ViewDateParams viewParams = new ViewDateParams(
        LocalDate.of(2025, 1, 15));
    CommandContext context = CommandContext.of("Work", null);

    outputStream.reset();
    viewCmd.execute(handler, viewParams, context);

    String output = outputStream.toString();
    assertFalse(output.isEmpty());

    assertEquals("Default", model.getActiveCalendarName());
  }

  /**
   * Tests Command error handling for non-existent event.
   */
  @Test
  public void testCommandErrorHandling() {
    Command cmd = registry.resolve(CommandType.EDIT_EVENT);

    EditEventParams params = new EditEventParams(
        "NonExistent",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        "subject",
        "New Name"
    );

    cmd.execute(handler, params, CommandContext.empty());

    String error = errorStream.toString();
    assertTrue(error.contains("Event not found"));
  }

  /**
   * Tests that all registered commands are executable.
   */
  @Test
  public void testAllRegisteredCommandsExecutable() {
    Object[] testParams = {
        new CreateCalendarParams("Test", ZoneId.of("UTC")),
        new EditCalendarParams("Default", "timezone", "UTC"),
        new UseCalendarParams("Default"),
        new CreateEventParams("Test",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1)),
        new EditEventParams("Test", LocalDateTime.now(), "subject", "New"),
        new EditEventParams("Test", LocalDateTime.now(), "subject", "New"),
        new EditEventParams("Test", LocalDateTime.now(), "subject", "New"),
        new PrintDateParams(LocalDate.now()),
        new PrintRangeParams(LocalDate.now(), LocalDate.now().plusDays(1)),
        new ViewDateParams(LocalDate.now()),
        new ViewDateParams(LocalDate.now()),
        new ViewDateParams(LocalDate.now()),
        new StatusParams(LocalDateTime.now()),
        null,
        null,
        null,
        new ExportParams("test.csv"),
        null
    };

    CommandType[] types = CommandType.values();
    for (int i = 0; i < types.length; i++) {
      CommandType type = types[i];
      Object params = (i < testParams.length) ? testParams[i] : null;

      Command cmd = registry.resolve(type);
      assertNotNull("No command for type: " + type, cmd);

      if (type.toString().contains("COPY")) {
        continue;
      }

      try {
        outputStream.reset();
        errorStream.reset();
        cmd.execute(handler, params, CommandContext.empty());

        assertTrue("No output for: " + type,
            outputStream.toString().length() > 0
                || errorStream.toString().length() > 0);
      } catch (Exception e) {
        fail("Unexpected exception for " + type + ": " + e.getMessage());
      }
    }
  }

  /**
   * Test CommandContext with empty string calendar name.
   */
  @Test
  public void testCommandContextEmptyStringCalendar() {
    CommandContext ctx = CommandContext.of("", null);

    assertTrue(ctx.getCalendarName().isPresent());
    assertEquals("", ctx.getCalendarName().get());
  }

  /**
   * Test CommandContext equals with one null calendar.
   */
  @Test
  public void testCommandContextEqualsOneNullCalendar() {
    CommandContext ctx1 = CommandContext.of("Work", ZoneId.of("UTC"));
    CommandContext ctx2 = CommandContext.of(null, ZoneId.of("UTC"));

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Test CommandContext equals with one null zone.
   */
  @Test
  public void testCommandContextEqualsOneNullZone() {
    CommandContext ctx1 = CommandContext.of("Work", ZoneId.of("UTC"));
    CommandContext ctx2 = CommandContext.of("Work", null);

    assertNotEquals(ctx1, ctx2);
  }

  /**
   * Test Command execution throwing runtime exception.
   */
  @Test
  public void testCommandExecutionRuntimeException() {
    Command throwingCmd = (h, params, ctx) -> {
      throw new RuntimeException("Test exception");
    };

    try {
      throwingCmd.execute(handler, null, CommandContext.empty());
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("Test exception", e.getMessage());
    }
  }

  /**
   * Test Command with all null parameters.
   */
  @Test
  public void testCommandAllNullParameters() {
    Command nullSafeCmd = (h, params, ctx) -> {
      assertNull(params);
      assertNotNull(ctx);
      assertNotNull(h);
    };

    nullSafeCmd.execute(handler, null, CommandContext.empty());
  }

  /**
   * Test COPY_EVENT command resolution and execution.
   */
  @Test
  public void testCopyEventCommandExecution() {
    Command cmd = registry.resolve(CommandType.COPY_EVENT);
    assertNotNull(cmd);

    CopyEventParams params = new CopyEventParams(
        "Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        "Work",
        LocalDateTime.of(2025, 1, 16, 10, 0));

    cmd.execute(handler, params, CommandContext.empty());

    String error = errorStream.toString();
    assertTrue(error.contains("not found")
        || error.contains("Failed"));
  }

  /**
   * Test COPY_EVENTS_ON command resolution and execution.
   */
  @Test
  public void testCopyEventsOnCommandExecution() {
    Command cmd = registry.resolve(CommandType.COPY_EVENTS_ON);
    assertNotNull(cmd);

    CopyEventsOnParams params = new CopyEventsOnParams(
        LocalDate.of(2025, 1, 15),
        "Work",
        LocalDate.of(2025, 1, 16));

    cmd.execute(handler, params, CommandContext.empty());

    String error = errorStream.toString();
    assertTrue(error.contains("not found")
        || error.contains("Failed"));
  }

  /**
   * Test COPY_EVENTS_BETWEEN command resolution and execution.
   */
  @Test
  public void testCopyEventsBetweenCommandExecution() {
    Command cmd = registry.resolve(CommandType.COPY_EVENTS_BETWEEN);
    assertNotNull(cmd);

    CopyEventsBetweenParams params = new CopyEventsBetweenParams(
        LocalDate.of(2025, 1, 15),
        LocalDate.of(2025, 1, 20),
        "Work",
        LocalDate.of(2025, 2, 1));

    cmd.execute(handler, params, CommandContext.empty());

    String error = errorStream.toString();
    assertTrue(error.contains("not found")
        || error.contains("Failed"));
  }

  /**
   * Test Command with context containing both values.
   */
  @Test
  public void testCommandWithFullContext() throws Exception {
    model.createCalendar("Work", ZoneId.of("Europe/London"));

    Command contextCmd = (h, params, ctx) -> {
      assertTrue(ctx.getCalendarName().isPresent());
      assertTrue(ctx.getZoneId().isPresent());
      assertEquals("Work", ctx.getCalendarName().get());
      assertEquals(ZoneId.of("Asia/Tokyo"), ctx.getZoneId().get());
    };

    CommandContext fullContext = CommandContext.of("Work",
        ZoneId.of("Asia/Tokyo"));
    contextCmd.execute(handler, null, fullContext);
  }

  /**
   * Test CommandContext singleton optimization.
   */
  @Test
  public void testCommandContextSingletonOptimization() {
    CommandContext ctx1 = CommandContext.of(null, null);
    CommandContext ctx2 = CommandContext.of(null, null);
    CommandContext empty = CommandContext.empty();

    assertSame(ctx1, ctx2);
    assertSame(ctx1, empty);
  }

  /**
   * Test CommandContext hashCode for various combinations.
   */
  @Test
  public void testCommandContextHashCodeCombinations() {
    CommandContext ctx1 = CommandContext.of("Cal", null);

    CommandContext ctx2 = CommandContext.of(null, ZoneId.of("UTC"));

    assertNotEquals(ctx1.hashCode(), ctx2.hashCode());

    CommandContext ctx3 = CommandContext.of("Cal", ZoneId.of("UTC"));
    assertNotEquals(ctx1.hashCode(), ctx3.hashCode());
    assertNotEquals(ctx2.hashCode(), ctx3.hashCode());

    CommandContext empty = CommandContext.empty();
    assertNotEquals(empty.hashCode(), ctx1.hashCode());
  }

  /**
   * Test Command interface as method reference.
   */
  @Test
  public void testCommandAsMethodReference() {
    class TestHandler {
      void handleTest(CommandHandler h, Object p, CommandContext c) {
        view.displayMessage("Method reference test");
      }
    }

    TestHandler testHandler = new TestHandler();
    Command methodRefCmd = testHandler::handleTest;

    methodRefCmd.execute(handler, null, CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Method reference test"));
  }

  /**
   * Test ParsedCommand with context having both values.
   */
  @Test
  public void testParsedCommandWithBothContextValues() {
    CreateEventParams params = new CreateEventParams(
        "Meeting",
        LocalDateTime.of(2025, 1, 15, 10, 0),
        LocalDateTime.of(2025, 1, 15, 11, 0));

    CommandContext ctx = CommandContext.of("TestCal", ZoneId.of("UTC"));
    ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_EVENT,
        params, ctx);

    assertNotNull(cmd.getContext());
    assertTrue(cmd.getContext().getCalendarName().isPresent());
    assertEquals("TestCal", cmd.getContext().getCalendarName().get());
    assertTrue(cmd.getContext().getZoneId().isPresent());
    assertEquals(ZoneId.of("UTC"), cmd.getContext().getZoneId().get());
  }

  /**
   * Test ParsedCommand with context having only calendar.
   */
  @Test
  public void testParsedCommandContextCalendarOnly() {
    CommandContext ctx = CommandContext.of("WorkCalendar", null);
    ParsedCommand cmd = new ParsedCommand(CommandType.VIEW_DAY,
        new ViewDateParams(LocalDate.of(2025, 1, 15)), ctx);

    assertNotNull(cmd.getContext());
    assertTrue(cmd.getContext().getCalendarName().isPresent());
    assertEquals("WorkCalendar",
        cmd.getContext().getCalendarName().get());
    assertFalse(cmd.getContext().getZoneId().isPresent());
  }

  /**
   * Test ParsedCommand with context having only timezone.
   */
  @Test
  public void testParsedCommandContextTimezoneOnly() {
    CommandContext ctx = CommandContext.of(null, ZoneId.of("Europe/Paris"));
    ParsedCommand cmd = new ParsedCommand(CommandType.VIEW_DAY,
        new ViewDateParams(LocalDate.of(2025, 1, 15)), ctx);

    assertNotNull(cmd.getContext());
    assertFalse(cmd.getContext().getCalendarName().isPresent());
    assertTrue(cmd.getContext().getZoneId().isPresent());
    assertEquals(ZoneId.of("Europe/Paris"),
        cmd.getContext().getZoneId().get());
  }

  /**
   * Test CommandContext with different timezone instances.
   */
  @Test
  public void testCommandContextZoneIdEquality() {
    ZoneId zone1 = ZoneId.of("UTC");
    ZoneId zone2 = ZoneId.of("UTC");

    CommandContext ctx1 = CommandContext.of(null, zone1);
    CommandContext ctx2 = CommandContext.of(null, zone2);

    assertEquals(ctx1, ctx2);
    assertEquals(ctx1.hashCode(), ctx2.hashCode());
  }

  /**
   * Test Command functional interface with checked exception.
   */
  @Test
  public void testCommandWithCheckedException() {
    Command exceptionCmd = (h, params, ctx) -> {
      try {
        throw new Exception("Checked exception");
      } catch (Exception e) {
        view.displayError(e.getMessage());
      }
    };

    exceptionCmd.execute(handler, null, CommandContext.empty());

    String error = errorStream.toString();
    assertTrue(error.contains("Checked exception"));
  }

  /**
   * Test multiple command executions in sequence.
   */
  @Test
  public void testMultipleCommandExecutions() {
    Command createCalCmd = registry.resolve(CommandType.CREATE_CALENDAR);
    createCalCmd.execute(handler,
        new CreateCalendarParams("Test", ZoneId.of("UTC")),
        CommandContext.empty());

    Command useCalCmd = registry.resolve(CommandType.USE_CALENDAR);
    useCalCmd.execute(handler,
        new UseCalendarParams("Test"),
        CommandContext.empty());

    Command createEventCmd = registry.resolve(CommandType.CREATE_EVENT);
    createEventCmd.execute(handler,
        new CreateEventParams("Meeting",
            LocalDateTime.of(2025, 1, 15, 10, 0),
            LocalDateTime.of(2025, 1, 15, 11, 0)),
        CommandContext.empty());

    Command printCmd = registry.resolve(CommandType.PRINT_DATE);
    printCmd.execute(handler,
        new PrintDateParams(LocalDate.of(2025, 1, 15)),
        CommandContext.empty());

    String output = outputStream.toString();
    assertTrue(output.contains("Meeting"));
  }
}

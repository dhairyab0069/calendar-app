package calendar.controller.command;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Carries optional execution context extracted from the command string,
 * such as an explicit calendar target (ON &lt;calendar&gt;) or timezone
 * override (AT TZ &lt;zoneId&gt;).
 */
public final class CommandContext {

  private static final CommandContext EMPTY = new CommandContext(null, null);

  private final String calendarName;
  private final ZoneId zoneId;

  /**
   * Creates a context instance with optional overrides.
   *
   * @param calendarName calendar name to scope execution (nullable)
   * @param zoneId       timezone override (nullable)
   */
  private CommandContext(String calendarName, ZoneId zoneId) {
    this.calendarName = calendarName;
    this.zoneId = zoneId;
  }

  /**
   * Returns an empty command context with no overrides.
   *
   * @return immutable empty context
   */
  public static CommandContext empty() {
    return EMPTY;
  }

  /**
   * Creates a command context with the provided calendar and timezone.
   *
   * @param calendarName calendar name (nullable)
   * @param zoneId       timezone (nullable)
   * @return newly constructed context
   */
  public static CommandContext of(String calendarName, ZoneId zoneId) {
    if (calendarName == null && zoneId == null) {
      return EMPTY;
    }
    return new CommandContext(calendarName, zoneId);
  }

  /**
   * Returns the optional calendar name supplied via an {@code ON} clause.
   *
   * @return optional calendar name
   */
  public Optional<String> getCalendarName() {
    return Optional.ofNullable(calendarName);
  }

  /**
   * Returns the optional timezone override supplied via an {@code AT TZ} clause.
   *
   * @return optional timezone override
   */
  public Optional<ZoneId> getZoneId() {
    return Optional.ofNullable(zoneId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommandContext)) {
      return false;
    }
    CommandContext that = (CommandContext) o;
    return Objects.equals(calendarName, that.calendarName)
        && Objects.equals(zoneId, that.zoneId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calendarName, zoneId);
  }

  @Override
  public String toString() {
    return "CommandContext{"
        + "calendarName='" + calendarName + '\''
        + ", zoneId=" + zoneId
        + '}';
  }
}

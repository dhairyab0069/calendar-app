package calendar.controller.parser;

import java.time.ZoneId;

/**
 * Parameters for the {@code create calendar} command.
 */
public class CreateCalendarParams {
  private final String name;
  private final ZoneId zoneId;

  /**
   * Creates the parameter bundle for {@code create calendar}.
   *
   * @param name   calendar name
   * @param zoneId associated timezone
   */
  public CreateCalendarParams(String name, ZoneId zoneId) {
    this.name = name;
    this.zoneId = zoneId;
  }

  public String getName() {
    return name;
  }

  public ZoneId getZoneId() {
    return zoneId;
  }
}


package calendar.controller.parser;

/**
 * Parameters for the {@code use calendar} command.
 */
public class UseCalendarParams {

  private final String calendarName;

  /**
   * Creates the parameter bundle for the {@code use calendar} command.
   *
   * @param calendarName the target calendar name
   */
  public UseCalendarParams(String calendarName) {
    this.calendarName = calendarName;
  }

  public String getCalendarName() {
    return calendarName;
  }
}


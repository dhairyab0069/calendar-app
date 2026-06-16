package calendar.controller.parser;

/**
 * Parameters for the {@code edit calendar} command.
 */
public class EditCalendarParams {

  private final String calendarName;
  private final String property;
  private final String newValue;

  /**
   * Creates parameters for {@code edit calendar}.
   *
   * @param calendarName calendar to modify
   * @param property     property name (e.g., {@code name}, {@code timezone})
   * @param newValue     replacement value
   */
  public EditCalendarParams(String calendarName, String property, String newValue) {
    this.calendarName = calendarName;
    this.property = property;
    this.newValue = newValue;
  }

  public String getCalendarName() {
    return calendarName;
  }

  public String getProperty() {
    return property;
  }

  public String getNewValue() {
    return newValue;
  }
}


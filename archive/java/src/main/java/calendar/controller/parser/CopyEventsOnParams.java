package calendar.controller.parser;

import java.time.LocalDate;

/**
 * Parameters for {@code copy events on} command.
 */
public class CopyEventsOnParams {

  private final LocalDate sourceDate;
  private final String targetCalendar;
  private final LocalDate targetDate;

  /**
   * Creates parameters for {@code copy events on} command.
   *
   * @param sourceDate     the date to copy from
   * @param targetCalendar destination calendar name
   * @param targetDate     the date to paste to in target calendar
   */
  public CopyEventsOnParams(LocalDate sourceDate, String targetCalendar, LocalDate targetDate) {
    this.sourceDate = sourceDate;
    this.targetCalendar = targetCalendar;
    this.targetDate = targetDate;
  }

  public LocalDate getSourceDate() {
    return sourceDate;
  }

  public String getTargetCalendar() {
    return targetCalendar;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }
}


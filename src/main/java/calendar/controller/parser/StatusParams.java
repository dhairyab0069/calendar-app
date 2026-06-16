package calendar.controller.parser;

import java.time.LocalDateTime;

/**
 * Parameters for status command.
 */
public class StatusParams {
  private final LocalDateTime dateTime;

  /**
   * Creates parameters for status command.
   *
   * @param dateTime the date-time to check status for
   */
  public StatusParams(LocalDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }
}

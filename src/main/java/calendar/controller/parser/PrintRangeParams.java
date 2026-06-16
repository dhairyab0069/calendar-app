package calendar.controller.parser;

import java.time.LocalDate;

/**
 * Parameters for print range command.
 */
public class PrintRangeParams {
  private final LocalDate startDate;
  private final LocalDate endDate;

  /**
   * Creates parameters for print range command.
   *
   * @param startDate the start date of the range
   * @param endDate   the end date of the range
   */
  public PrintRangeParams(LocalDate startDate, LocalDate endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}

package calendar.controller.parser;

import java.time.LocalDate;

/**
 * Parameters for print date command.
 */
public class PrintDateParams {
  private final LocalDate date;

  /**
   * Creates parameters for print date command.
   *
   * @param date the date to print
   */
  public PrintDateParams(LocalDate date) {
    this.date = date;
  }

  public LocalDate getDate() {
    return date;
  }
}

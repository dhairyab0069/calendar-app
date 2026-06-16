package calendar.controller.parser;

import java.time.LocalDate;

/**
 * Parameters for view commands (day, week, month).
 *
 * <p>Contains a date that serves as the reference point for the view:
 * <ul>
 *   <li><b>Day view</b>: The specific date to display</li>
 *   <li><b>Week view</b>: The start date of the week (typically Sunday or Monday)</li>
 *   <li><b>Month view</b>: Any date in the month to display (year-month extracted)</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe.
 *
 * @version 1.0
 */
public class ViewDateParams {

  private final LocalDate date;

  /**
   * Creates parameters for a view command.
   *
   * @param date the reference date for the view
   * @throws IllegalArgumentException if date is null
   */
  public ViewDateParams(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("Date cannot be null");
    }
    this.date = date;
  }

  /**
   * Gets the reference date.
   *
   * @return the date
   */
  public LocalDate getDate() {
    return date;
  }

  @Override
  public String toString() {
    return "ViewDateParams{date=" + date + "}";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ViewDateParams other = (ViewDateParams) obj;
    return date.equals(other.date);
  }

  @Override
  public int hashCode() {
    return date.hashCode();
  }
}
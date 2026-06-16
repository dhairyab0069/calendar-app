package calendar.controller.parser;

import java.time.LocalDate;

/**
 * Parameters for {@code copy events between} command.
 * Uses Builder pattern for validation and consistency.
 */
public class CopyEventsBetweenParams {

  private final LocalDate startDate;
  private final LocalDate endDate;
  private final String targetCalendar;
  private final LocalDate targetStartDate;

  /**
   * Creates parameter bundle for {@code copy events between} command.
   * Kept for backward compatibility.
   *
   * @param startDate       inclusive start of the interval to copy
   * @param endDate         inclusive end of the interval to copy
   * @param targetCalendar  destination calendar name
   * @param targetStartDate start date of the destination interval
   */
  public CopyEventsBetweenParams(LocalDate startDate, LocalDate endDate,
                                 String targetCalendar, LocalDate targetStartDate) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.targetCalendar = targetCalendar;
    this.targetStartDate = targetStartDate;
  }

  /**
   * Private constructor for builder.
   */
  private CopyEventsBetweenParams(Builder builder) {
    this.startDate = builder.startDate;
    this.endDate = builder.endDate;
    this.targetCalendar = builder.targetCalendar;
    this.targetStartDate = builder.targetStartDate;
  }

  /**
   * Creates a builder for CopyEventsBetweenParams.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getTargetCalendar() {
    return targetCalendar;
  }

  public LocalDate getTargetStartDate() {
    return targetStartDate;
  }

  /**
   * Builder for CopyEventsBetweenParams.
   * Provides a fluent API with validation.
   */
  public static class Builder {
    private LocalDate startDate;
    private LocalDate endDate;
    private String targetCalendar;
    private LocalDate targetStartDate;

    /**
     * Sets the start date of the interval to copy.
     *
     * @param startDate the inclusive start date (required)
     * @return this builder
     */
    public Builder startDate(LocalDate startDate) {
      if (startDate == null) {
        throw new IllegalArgumentException("Start date cannot be null");
      }
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the interval to copy.
     *
     * @param endDate the inclusive end date (required, must be >= startDate)
     * @return this builder
     */
    public Builder endDate(LocalDate endDate) {
      if (endDate == null) {
        throw new IllegalArgumentException("End date cannot be null");
      }
      if (startDate != null && endDate.isBefore(startDate)) {
        throw new IllegalArgumentException("End date must be after or equal to start date");
      }
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the target calendar name.
     *
     * @param targetCalendar the destination calendar name (required)
     * @return this builder
     */
    public Builder targetCalendar(String targetCalendar) {
      if (targetCalendar == null || targetCalendar.trim().isEmpty()) {
        throw new IllegalArgumentException("Target calendar cannot be null or empty");
      }
      this.targetCalendar = targetCalendar.trim();
      return this;
    }

    /**
     * Sets the target start date for the copied interval.
     *
     * @param targetStartDate the start date in the target calendar (required)
     * @return this builder
     */
    public Builder targetStartDate(LocalDate targetStartDate) {
      if (targetStartDate == null) {
        throw new IllegalArgumentException("Target start date cannot be null");
      }
      this.targetStartDate = targetStartDate;
      return this;
    }

    /**
     * Builds the CopyEventsBetweenParams instance.
     *
     * @return a new CopyEventsBetweenParams instance
     * @throws IllegalArgumentException if any required field is missing or invalid
     */
    public CopyEventsBetweenParams build() {
      if (startDate == null) {
        throw new IllegalArgumentException("Start date is required");
      }
      if (endDate == null) {
        throw new IllegalArgumentException("End date is required");
      }
      if (endDate.isBefore(startDate)) {
        throw new IllegalArgumentException("End date must be after or equal to start date");
      }
      if (targetCalendar == null) {
        throw new IllegalArgumentException("Target calendar is required");
      }
      if (targetStartDate == null) {
        throw new IllegalArgumentException("Target start date is required");
      }

      return new CopyEventsBetweenParams(this);
    }
  }
}


package calendar.controller.parser;

import calendar.model.Weekday;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Parameters for create event command.
 * Uses Builder pattern for better handling of optional recurrence fields.
 */
public class CreateEventParams {
  private final String subject;
  private final LocalDateTime start;
  private final LocalDateTime end;
  private final Weekday[] recurrenceDays;
  private final Integer repeatCount;
  private final LocalDateTime repeatUntil;

  /**
   * Creates parameters for a single (non-recurring) event.
   * Kept for backward compatibility.
   *
   * @param subject the event subject
   * @param start   the start date-time
   * @param end     the end date-time
   */
  public CreateEventParams(String subject, LocalDateTime start, LocalDateTime end) {
    this(subject, start, end, null, null, null);
  }

  /**
   * Creates parameters for a recurring event.
   * Kept for backward compatibility.
   *
   * @param subject        the event subject
   * @param start          the start date-time
   * @param end            the end date-time
   * @param recurrenceDays days of the week to repeat on
   * @param repeatCount    number of occurrences (null for unlimited with repeatUntil)
   * @param repeatUntil    end date for series (null if using repeatCount)
   */
  public CreateEventParams(String subject, LocalDateTime start, LocalDateTime end,
                           Weekday[] recurrenceDays, Integer repeatCount,
                           LocalDateTime repeatUntil) {
    this.subject = subject;
    this.start = start;
    this.end = end;
    this.recurrenceDays = recurrenceDays;
    this.repeatCount = repeatCount;
    this.repeatUntil = repeatUntil;
  }

  /**
   * Private constructor for builder.
   */
  private CreateEventParams(Builder builder) {
    this.subject = builder.subject;
    this.start = builder.start;
    this.end = builder.end;
    this.recurrenceDays = builder.recurrenceDays;
    this.repeatCount = builder.repeatCount;
    this.repeatUntil = builder.repeatUntil;
  }

  /**
   * Creates a builder for a single (non-recurring) event.
   *
   * @param subject the event subject (required)
   * @param start   the start date-time (required)
   * @param end     the end date-time (required)
   * @return a new Builder instance
   */
  public static Builder builder(String subject, LocalDateTime start, LocalDateTime end) {
    return new Builder(subject, start, end);
  }

  public String getSubject() {
    return subject;
  }

  public LocalDateTime getStart() {
    return start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  public Weekday[] getRecurrenceDays() {
    return recurrenceDays;
  }

  public Integer getRepeatCount() {
    return repeatCount;
  }

  public LocalDateTime getRepeatUntil() {
    return repeatUntil;
  }

  public boolean isRecurring() {
    return recurrenceDays != null;
  }

  /**
   * Builder for CreateEventParams.
   * Provides a fluent API for constructing event parameters with optional recurrence fields.
   */
  public static class Builder {
    private final String subject;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private Weekday[] recurrenceDays;
    private Integer repeatCount;
    private LocalDateTime repeatUntil;

    /**
     * Creates a builder with required fields.
     *
     * @param subject the event subject (required, non-null)
     * @param start   the start date-time (required, non-null)
     * @param end     the end date-time (required, non-null)
     * @throws IllegalArgumentException if any required field is null or invalid
     */
    public Builder(String subject, LocalDateTime start, LocalDateTime end) {
      if (subject == null || subject.trim().isEmpty()) {
        throw new IllegalArgumentException("Subject cannot be null or empty");
      }
      if (start == null) {
        throw new IllegalArgumentException("Start time cannot be null");
      }
      if (end == null) {
        throw new IllegalArgumentException("End time cannot be null");
      }
      if (end.isBefore(start) || end.equals(start)) {
        throw new IllegalArgumentException("End time must be after start time");
      }

      this.subject = subject.trim();
      this.start = start;
      this.end = end;
    }

    /**
     * Sets the recurrence days for a recurring event.
     *
     * @param days the days of the week to repeat on (required for recurring events)
     * @return this builder
     */
    public Builder withRecurrence(Weekday[] days) {
      if (days == null || days.length == 0) {
        throw new IllegalArgumentException("Recurrence days cannot be null or empty");
      }
      this.recurrenceDays = Arrays.copyOf(days, days.length);
      return this;
    }

    /**
     * Sets the repeat count for recurring events.
     * Mutually exclusive with repeatUntil.
     *
     * @param count the number of occurrences (must be positive)
     * @return this builder
     */
    public Builder repeatCount(Integer count) {
      if (count != null && count <= 0) {
        throw new IllegalArgumentException("Repeat count must be positive");
      }
      this.repeatCount = count;
      this.repeatUntil = null; // Mutually exclusive
      return this;
    }

    /**
     * Sets the repeat until date for recurring events.
     * Mutually exclusive with repeatCount.
     *
     * @param until the end date for the series (must be after start date)
     * @return this builder
     */
    public Builder repeatUntil(LocalDateTime until) {
      if (until != null && until.isBefore(start)) {
        throw new IllegalArgumentException("Repeat until date must be after start date");
      }
      this.repeatUntil = until;
      this.repeatCount = null; // Mutually exclusive
      return this;
    }

    /**
     * Builds the CreateEventParams instance.
     *
     * @return a new CreateEventParams instance
     * @throws IllegalArgumentException if validation fails
     */
    public CreateEventParams build() {
      // Validate recurrence configuration if recurring
      if (recurrenceDays != null) {
        if (repeatCount == null && repeatUntil == null) {
          throw new IllegalArgumentException(
              "Recurring events require either repeatCount or repeatUntil");
        }
      } else {
        // Single event should not have recurrence fields
        if (repeatCount != null || repeatUntil != null) {
          throw new IllegalArgumentException(
              "repeatCount and repeatUntil can only be set for recurring events");
        }
      }

      return new CreateEventParams(this);
    }
  }
}

package calendar.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a calendar event with support for both single
 * and recurring occurrences.
 *
 * <p>This class is fully immutable - all fields are final and any mutable data (arrays) are
 * defensively copied. Use the Builder pattern to create instances with optional fields.
 *
 * <p>A unified representation where:
 * <ul>
 *   <li>Single events have seriesId = null</li>
 *   <li>Recurring events have seriesId != null (UUID)</li>
 * </ul>
 *
 * <p>Uniqueness is defined by the tuple (subject, start, end).
 *
 * @version 2.0
 */
public final class CalendarEvent {

  private final String subject;
  private final LocalDateTime start;
  private final LocalDateTime end;
  private final String description;
  private final String location;
  private final EventStatus status;
  private final String seriesId;
  private final Weekday[] recurrenceDays;
  private final Integer repeatCount;
  private final LocalDateTime repeatUntil;

  /**
   * Private constructor - use Builder to create instances.
   */
  private CalendarEvent(Builder builder) {
    this.subject = builder.subject;
    this.start = builder.start;
    this.end = builder.end;
    this.description = builder.description;
    this.location = builder.location;
    this.status = builder.status;
    this.seriesId = builder.seriesId;
    this.recurrenceDays = builder.recurrenceDays == null
        ? null
        : Arrays.copyOf(builder.recurrenceDays, builder.recurrenceDays.length);
    this.repeatCount = builder.repeatCount;
    this.repeatUntil = builder.repeatUntil;
  }

  /**
   * Creates a builder for a single (non-recurring) event.
   *
   * @param subject the event subject (required, non-empty)
   * @param start   the start date-time (required)
   * @param end     the end date-time (required, must be after start)
   * @return a new Builder instance
   * @throws IllegalArgumentException if validation fails
   */
  public static Builder builder(String subject, LocalDateTime start, LocalDateTime end) {
    return new Builder(subject, start, end);
  }

  /**
   * Creates a builder for a recurring event.
   *
   * @param subject        the event subject
   * @param start          the start date-time
   * @param end            the end date-time
   * @param recurrenceDays days of the week to repeat on
   * @return a new Builder instance for recurring events
   * @throws IllegalArgumentException if validation fails
   */
  public static Builder recurringBuilder(String subject, LocalDateTime start,
                                         LocalDateTime end, Weekday[] recurrenceDays) {
    return new Builder(subject, start, end).withRecurrence(recurrenceDays);
  }

  /**
   * Creates a copy builder from an existing event.
   * Useful for creating modified versions of immutable events.
   *
   * @param event the event to copy
   * @return a new Builder with all fields copied from the event
   */
  public static Builder from(CalendarEvent event) {
    Builder builder = new Builder(event.subject, event.start, event.end)
        .description(event.description)
        .location(event.location)
        .status(event.status);

    if (event.isRecurring()) {
      builder.seriesId(event.seriesId)
          .recurrenceDays(event.recurrenceDays);

      // IMPORTANT: Only set one of repeatCount or repeatUntil to avoid
      // clearing the other via builder methods that are mutually exclusive.
      if (event.repeatCount != null) {
        builder.repeatCount(event.repeatCount);
      } else if (event.repeatUntil != null) {
        builder.repeatUntil(event.repeatUntil);
      }
    }

    return builder;
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

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public EventStatus getStatus() {
    return status;
  }

  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Returns a defensive copy of recurrence days array.
   *
   * @return copy of recurrence days array, or null if not recurring
   */
  public Weekday[] getRecurrenceDays() {
    return recurrenceDays == null ? null : Arrays.copyOf(recurrenceDays, recurrenceDays.length);
  }

  public Integer getRepeatCount() {
    return repeatCount;
  }

  public LocalDateTime getRepeatUntil() {
    return repeatUntil;
  }

  /**
   * Checks if this is a recurring event.
   *
   * @return true if part of a series, false if single event
   */
  public boolean isRecurring() {
    return seriesId != null;
  }

  /**
   * Creates a new CalendarEvent with updated description.
   *
   * @param newDescription the new description
   * @return a new CalendarEvent instance with the updated description
   */
  public CalendarEvent withDescription(String newDescription) {
    return CalendarEvent.from(this).description(newDescription).build();
  }

  /**
   * Creates a new CalendarEvent with updated location.
   *
   * @param newLocation the new location
   * @return a new CalendarEvent instance with the updated location
   */
  public CalendarEvent withLocation(String newLocation) {
    return CalendarEvent.from(this).location(newLocation).build();
  }

  /**
   * Creates a new CalendarEvent with updated status.
   *
   * @param newStatus the new status
   * @return a new CalendarEvent instance with the updated status
   */
  public CalendarEvent withStatus(EventStatus newStatus) {
    return CalendarEvent.from(this).status(newStatus).build();
  }

  /**
   * Creates a new CalendarEvent with updated start and end times.
   * Useful for rescheduling events.
   *
   * @param newStart the new start time
   * @param newEnd   the new end time
   * @return a new CalendarEvent instance with updated times
   * @throws IllegalArgumentException if newEnd is before newStart
   */
  public CalendarEvent withTimes(LocalDateTime newStart, LocalDateTime newEnd) {
    return CalendarEvent.from(this)
        .start(newStart)
        .end(newEnd)
        .build();
  }

  /**
   * Equality based on (subject, start, end) tuple.
   * Note: This means two events with the same subject and times but different
   * descriptions or locations are considered equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CalendarEvent other = (CalendarEvent) obj;
    return Objects.equals(subject, other.subject)
        && Objects.equals(start, other.start)
        && Objects.equals(end, other.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, start, end);
  }

  @Override
  public String toString() {
    return String.format("CalendarEvent{subject='%s', start=%s, end=%s, recurring=%b, status=%s}",
        subject, start, end, isRecurring(), status);
  }

  /**
   * Builder class for constructing CalendarEvent instances.
   * Ensures validation and provides a fluent API for setting optional fields.
   */
  public static class Builder {
    private String subject;
    private LocalDateTime start;
    private LocalDateTime end;

    private String description = "";
    private String location = "";
    private EventStatus status = EventStatus.PUBLIC;

    private String seriesId = null;
    private Weekday[] recurrenceDays = null;
    private Integer repeatCount = null;
    private LocalDateTime repeatUntil = null;

    /**
     * Constructor with required fields.
     *
     * @param subject the event subject (required, non-empty)
     * @param start   the start date-time (required)
     * @param end     the end date-time (required, must be after start)
     * @throws IllegalArgumentException if validation fails
     */
    public Builder(String subject, LocalDateTime start, LocalDateTime end) {
      validateBasicInput(subject, start, end);
      this.subject = subject.trim();
      this.start = start;
      this.end = end;
    }

    /**
     * Validates basic event input.
     */
    private static void validateBasicInput(String subject, LocalDateTime start, LocalDateTime end) {
      if (subject == null || subject.trim().isEmpty()) {
        throw new IllegalArgumentException("Event subject cannot be null or empty");
      }
      if (start == null) {
        throw new IllegalArgumentException("Start time cannot be null");
      }
      if (end == null) {
        throw new IllegalArgumentException("End time cannot be null");
      }
      if (end.isBefore(start)) {
        throw new IllegalArgumentException("End time must be after start time");
      }
    }

    /**
     * Validates recurrence input.
     */
    private static void validateRecurrenceInput(Weekday[] days, Integer count,
                                                LocalDateTime until) {
      if (days == null || days.length == 0) {
        throw new IllegalArgumentException("Recurrence days cannot be null or empty");
      }
      if (count == null && until == null) {
        throw new IllegalArgumentException(
            "Must specify either repeat count or repeat until date");
      }
      if (count != null && count <= 0) {
        throw new IllegalArgumentException("Repeat count must be positive");
      }
    }

    /**
     * Sets the event description.
     *
     * @param description the event description
     * @return this builder
     */
    public Builder description(String description) {
      this.description = description == null ? "" : description;
      return this;
    }

    /**
     * Sets the event location.
     *
     * @param location the event location
     * @return this builder
     */
    public Builder location(String location) {
      this.location = location == null ? "" : location;
      return this;
    }

    /**
     * Sets the event status.
     *
     * @param status the event status
     * @return this builder
     * @throws IllegalArgumentException if status is null
     */
    public Builder status(EventStatus status) {
      if (status == null) {
        throw new IllegalArgumentException("Status cannot be null");
      }
      this.status = status;
      return this;
    }

    /**
     * Updates the start time.
     *
     * @param newStart the new start time
     * @return this builder
     * @throws IllegalArgumentException if validation fails
     */
    public Builder start(LocalDateTime newStart) {
      if (newStart == null) {
        throw new IllegalArgumentException("Start time cannot be null");
      }
      if (this.end != null && this.end.isBefore(newStart)) {
        throw new IllegalArgumentException("End time must be after start time");
      }
      this.start = newStart;
      return this;
    }

    /**
     * Updates the end time.
     *
     * @param newEnd the new end time
     * @return this builder
     * @throws IllegalArgumentException if validation fails
     */
    public Builder end(LocalDateTime newEnd) {
      if (newEnd == null) {
        throw new IllegalArgumentException("End time cannot be null");
      }
      if (this.start != null && newEnd.isBefore(this.start)) {
        throw new IllegalArgumentException("End time must be after start time");
      }
      this.end = newEnd;
      return this;
    }

    /**
     * Configures the event as recurring with specified days.
     *
     * @param recurrenceDays days of the week to repeat on
     * @return this builder
     * @throws IllegalArgumentException if recurrenceDays is null or empty
     */
    public Builder withRecurrence(Weekday[] recurrenceDays) {
      if (recurrenceDays == null || recurrenceDays.length == 0) {
        throw new IllegalArgumentException("Recurrence days cannot be null or empty");
      }
      this.recurrenceDays = Arrays.copyOf(recurrenceDays, recurrenceDays.length);
      this.seriesId = UUID.randomUUID().toString();
      return this;
    }

    /**
     * Sets the series ID for recurring events.
     */
    public Builder seriesId(String seriesId) {
      this.seriesId = seriesId;
      return this;
    }

    /**
     * Sets recurrence days without validation (for copying).
     */
    public Builder recurrenceDays(Weekday[] days) {
      this.recurrenceDays = days == null ? null : Arrays.copyOf(days, days.length);
      return this;
    }

    /**
     * Sets the repeat count for recurring events.
     *
     * @param count the number of occurrences
     * @return this builder
     * @throws IllegalArgumentException if count is not positive
     */
    public Builder repeatCount(Integer count) {
      if (count != null && count <= 0) {
        throw new IllegalArgumentException("Repeat count must be positive");
      }
      this.repeatCount = count;
      this.repeatUntil = null;
      return this;
    }

    /**
     * Sets the end date for recurring events.
     *
     * @param until the end date for the series
     * @return this builder
     */
    public Builder repeatUntil(LocalDateTime until) {
      this.repeatUntil = until;
      this.repeatCount = null;
      return this;
    }

    /**
     * Builds the CalendarEvent instance.
     *
     * @return a new immutable CalendarEvent
     * @throws IllegalArgumentException if validation fails
     */
    public CalendarEvent build() {
      if (recurrenceDays != null) {
        validateRecurrenceInput(recurrenceDays, repeatCount, repeatUntil);
      }

      return new CalendarEvent(this);
    }
  }
}
package calendar.controller.parser;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameters for edit event commands.
 * Uses Builder pattern for validation and consistency.
 */
public class EditEventParams {
  private static final Set<String> VALID_PROPERTIES = Set.of(
      "subject", "start", "end", "description", "location", "status"
  );

  private final String subject;
  private final LocalDateTime dateTime;
  private final String property;
  private final String newValue;

  /**
   * Creates parameters for an edit event command.
   * Kept for backward compatibility.
   *
   * @param subject  the subject of the event to edit
   * @param dateTime the datetime of the event to edit
   * @param property the property to modify (subject, start, end, description, location, status)
   * @param newValue the new value for the property
   */
  public EditEventParams(String subject, LocalDateTime dateTime, String property, String newValue) {
    this.subject = subject;
    this.dateTime = dateTime;
    this.property = property;
    this.newValue = newValue;
  }

  /**
   * Private constructor for builder.
   */
  private EditEventParams(Builder builder) {
    this.subject = builder.subject;
    this.dateTime = builder.dateTime;
    this.property = builder.property;
    this.newValue = builder.newValue;
  }

  /**
   * Creates a builder for EditEventParams.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public String getSubject() {
    return subject;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public String getProperty() {
    return property;
  }

  public String getNewValue() {
    return newValue;
  }

  /**
   * Builder for EditEventParams.
   * Provides a fluent API with validation.
   */
  public static class Builder {
    private String subject;
    private LocalDateTime dateTime;
    private String property;
    private String newValue;

    /**
     * Sets the subject of the event to edit.
     *
     * @param subject the event subject (required)
     * @return this builder
     */
    public Builder subject(String subject) {
      if (subject == null || subject.trim().isEmpty()) {
        throw new IllegalArgumentException("Subject cannot be null or empty");
      }
      this.subject = subject.trim();
      return this;
    }

    /**
     * Sets the date-time of the event to edit.
     *
     * @param dateTime the event date-time (required)
     * @return this builder
     */
    public Builder dateTime(LocalDateTime dateTime) {
      if (dateTime == null) {
        throw new IllegalArgumentException("DateTime cannot be null");
      }
      this.dateTime = dateTime;
      return this;
    }

    /**
     * Sets the property to modify.
     *
     * @param property the property name (required, must be one of: subject, start, end,
     *                 description, location, status)
     * @return this builder
     */
    public Builder property(String property) {
      if (property == null || property.trim().isEmpty()) {
        throw new IllegalArgumentException("Property cannot be null or empty");
      }
      String normalized = property.toLowerCase().trim();
      if (!VALID_PROPERTIES.contains(normalized)) {
        throw new IllegalArgumentException(
            "Invalid property: " + property + ". Must be one of: " + VALID_PROPERTIES);
      }
      this.property = normalized;
      return this;
    }

    /**
     * Sets the new value for the property.
     *
     * @param newValue the new value (required)
     * @return this builder
     */
    public Builder newValue(String newValue) {
      if (newValue == null) {
        throw new IllegalArgumentException("New value cannot be null");
      }
      this.newValue = newValue.trim();
      return this;
    }

    /**
     * Builds the EditEventParams instance.
     *
     * @return a new EditEventParams instance
     * @throws IllegalArgumentException if any required field is missing
     */
    public EditEventParams build() {
      if (subject == null) {
        throw new IllegalArgumentException("Subject is required");
      }
      if (dateTime == null) {
        throw new IllegalArgumentException("DateTime is required");
      }
      if (property == null) {
        throw new IllegalArgumentException("Property is required");
      }
      if (newValue == null) {
        throw new IllegalArgumentException("New value is required");
      }

      return new EditEventParams(this);
    }
  }
}
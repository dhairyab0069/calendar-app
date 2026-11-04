package calendar.controller.gui;

/**
 * Enumeration of all property change event types in the GUI.
 * Replaces magic strings with type-safe constants.
 *
 * @version 1.0
 */
public enum PropertyType {
  // Calendar-related properties
  CALENDAR_SELECTED,
  CREATE_CALENDAR,
  EDIT_CALENDAR,
  EXPORT_CALENDAR,
  IMPORT_CALENDAR,

  // Event-related properties
  CREATE_EVENT,
  EDIT_EVENT,
  EVENT_SELECTED,

  // View-related properties
  VIEW_CHANGED,
  DAY_SELECTED;

  /**
   * Converts a property name string to PropertyType enum.
   *
   * @param propertyName the property name string
   * @return the corresponding PropertyType, or null if not found
   */
  public static PropertyType fromString(String propertyName) {
    if (propertyName == null) {
      return null;
    }

    try {
      // Convert camelCase to UPPER_SNAKE_CASE
      String normalized = propertyName.toUpperCase();
      return valueOf(normalized);
    } catch (IllegalArgumentException e) {
      // Try camelCase to UPPER_SNAKE_CASE conversion
      String snakeCase = camelToSnake(propertyName);
      try {
        return valueOf(snakeCase.toUpperCase());
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
  }

  /**
   * Converts camelCase to SNAKE_CASE.
   *
   * @param camelCase the camelCase string
   * @return the SNAKE_CASE string
   */
  private static String camelToSnake(String camelCase) {
    if (camelCase == null || camelCase.isEmpty()) {
      return camelCase;
    }

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < camelCase.length(); i++) {
      char c = camelCase.charAt(i);
      if (Character.isUpperCase(c) && i > 0) {
        result.append('_');
      }
      result.append(Character.toUpperCase(c));
    }
    return result.toString();
  }
}


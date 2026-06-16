package calendar.controller.parser;

/**
 * Parameters for export command.
 */
public class ExportParams {
  private final String filename;

  /**
   * Creates parameters for export command.
   *
   * @param filename the filename to export to
   * @throws IllegalArgumentException if filename is null or empty
   */
  public ExportParams(String filename) {
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    this.filename = sanitizeFilename(filename);
  }

  /**
   * Sanitizes the filename to ensure it's safe for all operating systems.
   * Removes invalid characters and handles edge cases.
   *
   * @param filename the raw filename to sanitize
   * @return sanitized filename
   */
  private String sanitizeFilename(String filename) {
    String sanitized = filename.trim()
        .replaceAll("^\"|\"$", "")  // Remove surrounding double quotes
        .replaceAll("^'|'$", "");    // Remove surrounding single quotes

    sanitized = sanitized.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_");

    sanitized = sanitized.replaceAll("\\s+", "_");

    sanitized = sanitized.replaceAll("_{2,}", "_");

    sanitized = sanitized.replaceAll("^_+|_+$", "");

    if (sanitized.startsWith(".")) {
      sanitized = "export" + sanitized;
    }

    if (isWindowsReservedName(sanitized)) {
      sanitized = "export_" + sanitized;
    }

    if (sanitized.length() > 200) {
      sanitized = sanitized.substring(0, 200);
    }

    if (sanitized.isEmpty()) {
      sanitized = "export_" + System.currentTimeMillis();
    }

    return sanitized;
  }

  /**
   * Checks if the name is a Windows reserved filename.
   *
   * @param name the filename to check
   * @return true if reserved, false otherwise
   */
  private boolean isWindowsReservedName(String name) {
    String[] reserved = {
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    };

    String baseName = name.contains(".")
        ? name.substring(0, name.indexOf(".")) : name;
    String upperName = baseName.toUpperCase();

    for (String reservedName : reserved) {
      if (upperName.equals(reservedName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the sanitized filename.
   *
   * @return the sanitized filename
   */
  public String getFilename() {
    return filename;
  }
}
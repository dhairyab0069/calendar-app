package calendar.exceptions;

/**
 * Exception thrown when file export operations fail.
 *
 * <p>This exception wraps I/O errors that occur during calendar data export,
 * providing a domain-specific exception type for the calendar application.
 *
 * <p>Common causes include:
 * <ul>
 *   <li>Insufficient file system permissions</li>
 *   <li>Invalid or inaccessible file path</li>
 *   <li>Disk full or I/O errors</li>
 *   <li>File is locked by another process</li>
 *   <li>Invalid file format or encoding issues</li>
 * </ul>
 *
 * <p>Example scenarios:
 * <ul>
 *   <li>export /root/protected.csv (permission denied)</li>
 *   <li>export /nonexistent/path/file.csv (directory doesn't exist)</li>
 *   <li>export C:\locked\file.csv (file in use by another program)</li>
 * </ul>
 *
 * @version 1.0
 */
public class FileExportException extends Exception {

  /**
   * Creates a new FileExportException with the specified message.
   *
   * @param message a description of the export failure
   */
  public FileExportException(String message) {
    super(message);
  }

  /**
   * Creates a new FileExportException with the specified message and cause.
   *
   * <p>Typically the cause will be an IOException from the underlying file operation.
   *
   * @param message a description of the export failure
   * @param cause   the underlying cause (usually an IOException)
   */
  public FileExportException(String message, Throwable cause) {
    super(message, cause);
  }
}
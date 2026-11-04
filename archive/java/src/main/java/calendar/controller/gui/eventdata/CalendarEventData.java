package calendar.controller.gui.eventdata;

import java.time.ZoneId;

/**
 * Typed data class for calendar-related property change events.
 * Eliminates unsafe Object[] casting.
 *
 * @version 1.0
 */
public class CalendarEventData {

  /**
   * Data for create calendar event.
   */
  public static class CreateCalendar {
    private final String name;
    private final ZoneId timezone;

    /**
     * Creates a new create calendar event data.
     *
     * @param name     the calendar name
     * @param timezone the calendar timezone
     */
    public CreateCalendar(String name, ZoneId timezone) {
      this.name = name;
      this.timezone = timezone;
    }

    public String getName() {
      return name;
    }

    public ZoneId getTimezone() {
      return timezone;
    }
  }

  /**
   * Data for edit calendar event.
   */
  public static class EditCalendar {
    private final String oldName;
    private final String newName;
    private final ZoneId newTimezone;

    /**
     * Creates a new edit calendar event data.
     *
     * @param oldName     the original calendar name
     * @param newName     the new calendar name
     * @param newTimezone the new calendar timezone
     */
    public EditCalendar(String oldName, String newName, ZoneId newTimezone) {
      this.oldName = oldName;
      this.newName = newName;
      this.newTimezone = newTimezone;
    }

    public String getOldName() {
      return oldName;
    }

    public String getNewName() {
      return newName;
    }

    public ZoneId getNewTimezone() {
      return newTimezone;
    }
  }

  /**
   * Data for export calendar event.
   */
  public static class ExportCalendar {
    private final String filename;
    private final String format;

    /**
     * Creates a new export calendar event data.
     *
     * @param filename the output filename
     * @param format   the export format (csv or ical)
     */
    public ExportCalendar(String filename, String format) {
      this.filename = filename;
      this.format = format;
    }

    public String getFilename() {
      return filename;
    }

    public String getFormat() {
      return format;
    }
  }

  /**
   * Data for import calendar event.
   */
  public static class ImportCalendar {
    private final String filename;
    private final String format;

    /**
     * Creates a new import calendar event data.
     *
     * @param filename the input filename
     * @param format   the import format (csv or ical)
     */
    public ImportCalendar(String filename, String format) {
      this.filename = filename;
      this.format = format;
    }

    public String getFilename() {
      return filename;
    }

    public String getFormat() {
      return format;
    }
  }
}


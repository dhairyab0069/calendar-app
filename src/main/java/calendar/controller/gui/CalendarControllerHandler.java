package calendar.controller.gui;

import calendar.controller.gui.eventdata.CalendarEventData;
import calendar.exceptions.CalendarNotFoundException;
import calendar.exceptions.DuplicateCalendarException;
import calendar.exceptions.DuplicateEventException;
import calendar.model.CalendarModel;
import calendar.view.gui.SwingCalendarView;
import calendar.view.gui.panels.CalendarListPanel;
import java.beans.PropertyChangeEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Handles calendar-related property change events.
 * Responsible for calendar creation, editing, selection, export, and import operations.
 * Uses PropertyType enum and typed data classes for type safety.
 *
 * @version 2.0
 */
public class CalendarControllerHandler implements PropertyChangeHandler {

  private static final Set<PropertyType> HANDLED_PROPERTIES = Set.of(
      PropertyType.CALENDAR_SELECTED,
      PropertyType.CREATE_CALENDAR,
      PropertyType.EDIT_CALENDAR,
      PropertyType.EXPORT_CALENDAR,
      PropertyType.IMPORT_CALENDAR
  );

  private final CalendarModel model;
  private final SwingCalendarView view;

  /**
   * Creates a new calendar controller handler.
   *
   * @param model the calendar model
   * @param view  the Swing calendar view
   */
  public CalendarControllerHandler(CalendarModel model, SwingCalendarView view) {
    this.model = model;
    this.view = view;
  }

  @Override
  public boolean canHandle(PropertyType propertyType) {
    return propertyType != null && HANDLED_PROPERTIES.contains(propertyType);
  }

  @Override
  public void handle(PropertyType propertyType, PropertyChangeEvent evt) throws Exception {
    switch (propertyType) {
      case CALENDAR_SELECTED:
        handleCalendarSelected((String) evt.getNewValue());
        break;

      case CREATE_CALENDAR:
        CalendarEventData.CreateCalendar data = 
            (CalendarEventData.CreateCalendar) EventDataParser.parseCalendarData(
                propertyType, evt);
        handleCreateCalendar(data.getName(), data.getTimezone());
        break;

      case EDIT_CALENDAR:
        CalendarEventData.EditCalendar editData = 
            (CalendarEventData.EditCalendar) EventDataParser.parseCalendarData(
                propertyType, evt);
        handleEditCalendar(editData.getOldName(), editData.getNewName(),
            editData.getNewTimezone());
        break;

      case EXPORT_CALENDAR:
        CalendarEventData.ExportCalendar exportData = 
            (CalendarEventData.ExportCalendar) EventDataParser.parseCalendarData(
                propertyType, evt);
        handleExportCalendar(exportData.getFilename(), exportData.getFormat());
        break;

      case IMPORT_CALENDAR:
        CalendarEventData.ImportCalendar importData = 
            (CalendarEventData.ImportCalendar) EventDataParser.parseCalendarData(
                propertyType, evt);
        handleImportCalendar(importData.getFilename(), importData.getFormat());
        break;

      default:
        throw new IllegalArgumentException("Unhandled property: " + propertyType);
    }
  }

  /**
   * Handles calendar selection.
   */
  private void handleCalendarSelected(String calendarName) {
    try {
      model.useCalendar(calendarName);
      view.setActiveCalendarName(calendarName);
      refreshCurrentView();
    } catch (CalendarNotFoundException e) {
      view.displayError("Calendar not found: " + calendarName);
    } catch (Exception e) {
      view.displayError("Failed to switch calendar: " + e.getMessage());
    }
  }

  /**
   * Handles calendar creation.
   */
  private void handleCreateCalendar(String name, ZoneId timezone) {
    try {
      model.createCalendar(name, timezone);
      view.getCalendarListPanel().addCalendar(name, timezone);

      model.useCalendar(name);
      view.setActiveCalendarName(name);
      refreshCurrentView();

      view.displayMessage("Calendar '" + name + "' created with timezone "
          + timezone.getId());
    } catch (DuplicateCalendarException e) {
      view.displayError("A calendar with this name already exists");
    } catch (Exception e) {
      view.displayError("Failed to create calendar: " + e.getMessage());
    }
  }

  /**
   * Handles calendar editing.
   */
  private void handleEditCalendar(String oldName, String newName, ZoneId newTimezone) {
    try {
      if (!oldName.equals(newName)) {
        model.renameCalendar(oldName, newName);
      }

      model.updateCalendarTimezone(newName, newTimezone);

      view.getCalendarListPanel().updateCalendar(oldName, newName, newTimezone);

      if (model.hasActiveCalendar() && model.getActiveCalendarName().equals(newName)) {
        view.setActiveCalendarName(newName);
      }

      view.displayMessage("Calendar updated successfully");
      refreshCurrentView();

    } catch (CalendarNotFoundException e) {
      view.displayError("Calendar not found");
    } catch (DuplicateCalendarException e) {
      view.displayError("A calendar with this name already exists");
    } catch (DuplicateEventException e) {
      view.displayError("Cannot change timezone: it would create duplicate events");
    } catch (Exception e) {
      view.displayError("Failed to update calendar: " + e.getMessage());
    }
  }

  /**
   * Handles calendar export.
   */
  private void handleExportCalendar(String filename, String format) {
    if (!model.hasActiveCalendar()) {
      view.displayError("No calendar selected for export");
      return;
    }

    try {
      String content;
      if (format.equals("csv")) {
        content = model.exportToCsv();
      } else {
        content = model.exportToIcal();
      }

      java.nio.file.Files.write(
          java.nio.file.Paths.get(filename),
          content.getBytes(java.nio.charset.StandardCharsets.UTF_8)
      );

      view.displayMessage("Calendar exported successfully to: " + filename);

    } catch (Exception e) {
      view.displayError("Export failed: " + e.getMessage());
    }
  }

  /**
   * Handles calendar import.
   */
  private void handleImportCalendar(String filename, String format) {
    try {
      String content = new String(
          java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filename)),
          java.nio.charset.StandardCharsets.UTF_8
      );

      int importedCount;
      if (format.equals("csv")) {
        importedCount = model.importFromCsv(content);
      } else {
        importedCount = model.importFromIcal(content);
      }

      view.displayMessage("Successfully imported " + importedCount + " events from: " + filename);
      refreshCurrentView();

    } catch (Exception e) {
      view.displayError("Import failed: " + e.getMessage());
    }
  }

  /**
   * Refreshes the current view after calendar operations.
   */
  private void refreshCurrentView() {
    if (!model.hasActiveCalendar()) {
      return;
    }

    YearMonth currentMonth = view.getCurrentMonth();
    LocalDate firstDay = currentMonth.atDay(1);
    LocalDate lastDay = currentMonth.atEndOfMonth();

    try {
      List<calendar.model.CalendarEvent> events = model.getEventsInRange(firstDay, lastDay);
      view.displayEvents(events);
    } catch (Exception e) {
      view.displayError("Failed to refresh view: " + e.getMessage());
    }
  }
}



package calendar.controller.gui;

import calendar.controller.gui.eventdata.CalendarEventData;
import calendar.controller.gui.eventdata.EventEventData;
import calendar.controller.gui.eventdata.ViewEventData;
import calendar.model.CalendarEvent;
import calendar.view.gui.SwingCalendarView;
import java.beans.PropertyChangeEvent;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Utility class to parse PropertyChangeEvent data into typed data classes.
 * Eliminates unsafe casting and provides type-safe access to event data.
 *
 * @version 1.0
 */
public class EventDataParser {

  /**
   * Parses calendar-related event data.
   *
   * @param propertyType the property type
   * @param evt          the property change event
   * @return typed data object, or null if not a calendar event
   */
  public static Object parseCalendarData(PropertyType propertyType,
                                         PropertyChangeEvent evt) {
    if (propertyType == null) {
      return null;
    }

    switch (propertyType) {
      case CREATE_CALENDAR:
        Object[] createData = (Object[]) evt.getNewValue();
        return new CalendarEventData.CreateCalendar(
            (String) createData[0],
            (ZoneId) createData[1]
        );

      case EDIT_CALENDAR:
        Object[] oldData = (Object[]) evt.getOldValue();
        Object[] newData = (Object[]) evt.getNewValue();
        return new CalendarEventData.EditCalendar(
            (String) oldData[0],
            (String) newData[0],
            (ZoneId) newData[1]
        );

      case EXPORT_CALENDAR:
        Object[] exportData = (Object[]) evt.getNewValue();
        return new CalendarEventData.ExportCalendar(
            (String) exportData[0],
            (String) exportData[1]
        );

      case IMPORT_CALENDAR:
        Object[] importData = (Object[]) evt.getNewValue();
        return new CalendarEventData.ImportCalendar(
            (String) importData[0],
            (String) importData[1]
        );

      default:
        return null;
    }
  }

  /**
   * Parses event-related event data.
   *
   * @param propertyType the property type
   * @param evt          the property change event
   * @return typed data object, or null if not an event-related property
   */
  public static Object parseEventData(PropertyType propertyType,
                                      PropertyChangeEvent evt) {
    if (propertyType == null) {
      return null;
    }

    switch (propertyType) {
      case EDIT_EVENT:
        return new EventEventData.EditEvent(
            (CalendarEvent) evt.getOldValue(),
            (CalendarEvent) evt.getNewValue()
        );

      default:
        return null;
    }
  }

  /**
   * Parses view-related event data.
   *
   * @param propertyType the property type
   * @param evt          the property change event
   * @return typed data object, or null if not a view-related property
   */
  public static Object parseViewData(PropertyType propertyType,
                                     PropertyChangeEvent evt) {
    if (propertyType == null) {
      return null;
    }

    switch (propertyType) {
      case VIEW_CHANGED:
        Object[] viewData = (Object[]) evt.getNewValue();
        return new ViewEventData.ViewChanged(
            (SwingCalendarView.ViewMode) viewData[0],
            (LocalDate) viewData[1]
        );

      default:
        return null;
    }
  }
}


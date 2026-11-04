package calendar.controller.gui;

import calendar.controller.gui.eventdata.EventEventData;
import calendar.exceptions.DuplicateEventException;
import calendar.exceptions.EventNotFoundException;
import calendar.model.CalendarEvent;
import calendar.model.CalendarModel;
import calendar.view.gui.SwingCalendarView;
import java.beans.PropertyChangeEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Handles event-related property change events.
 * Responsible for event creation, editing, and selection operations.
 * Uses PropertyType enum and typed data classes for type safety.
 *
 * @version 2.0
 */
public class EventControllerHandler implements PropertyChangeHandler {

  private static final Set<PropertyType> HANDLED_PROPERTIES = Set.of(
      PropertyType.CREATE_EVENT,
      PropertyType.EDIT_EVENT,
      PropertyType.EVENT_SELECTED
  );

  private final CalendarModel model;
  private final SwingCalendarView view;

  /**
   * Creates a new event controller handler.
   *
   * @param model the calendar model
   * @param view  the Swing calendar view
   */
  public EventControllerHandler(CalendarModel model, SwingCalendarView view) {
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
      case CREATE_EVENT:
        handleCreateEvent((CalendarEvent) evt.getNewValue());
        break;

      case EDIT_EVENT:
        EventEventData.EditEvent editData = 
            (EventEventData.EditEvent) EventDataParser.parseEventData(propertyType, evt);
        handleEditEvent(editData.getOldEvent(), editData.getNewEvent());
        break;

      case EVENT_SELECTED:
        handleEventSelected((CalendarEvent) evt.getNewValue());
        break;

      default:
        throw new IllegalArgumentException("Unhandled property: " + propertyType);
    }
  }

  /**
   * Handles event creation.
   */
  private void handleCreateEvent(CalendarEvent event) {
    if (!model.hasActiveCalendar()) {
      view.displayError("Please select a calendar first");
      return;
    }

    try {
      model.addEvent(event);
      view.displayMessage("Event created: " + event.getSubject());
      refreshCurrentView();
    } catch (DuplicateEventException e) {
      view.displayError("An event with the same subject and time already exists");
    } catch (Exception e) {
      view.displayError("Failed to create event: " + e.getMessage());
    }
  }

  /**
   * Handles event editing.
   */
  private void handleEditEvent(CalendarEvent oldEvent, CalendarEvent newEvent) {
    if (!model.hasActiveCalendar()) {
      view.displayError("Please select a calendar first");
      return;
    }

    try {
      model.editEvent(
          oldEvent.getSubject(),
          oldEvent.getStart(),
          newEvent.getSubject(),
          newEvent.getStart(),
          newEvent.getEnd(),
          newEvent.getDescription(),
          newEvent.getLocation(),
          newEvent.getStatus()
      );
      view.displayMessage("Event updated: " + newEvent.getSubject());
      refreshCurrentView();
    } catch (EventNotFoundException e) {
      view.displayError("Event not found. It may have been deleted.");
    } catch (DuplicateEventException e) {
      view.displayError("Cannot update: an event with these details already exists");
    } catch (Exception e) {
      view.displayError("Failed to update event: " + e.getMessage());
    }
  }

  /**
   * Handles event selection (opens edit dialog).
   */
  private void handleEventSelected(CalendarEvent event) {
    view.showEditEventDialog(event);
  }

  /**
   * Refreshes the current view after event operations.
   */
  private void refreshCurrentView() {
    if (!model.hasActiveCalendar()) {
      return;
    }

    YearMonth currentMonth = view.getCurrentMonth();
    LocalDate firstDay = currentMonth.atDay(1);
    LocalDate lastDay = currentMonth.atEndOfMonth();

    try {
      List<CalendarEvent> events = model.getEventsInRange(firstDay, lastDay);
      view.displayEvents(events);
    } catch (Exception e) {
      view.displayError("Failed to refresh view: " + e.getMessage());
    }
  }
}



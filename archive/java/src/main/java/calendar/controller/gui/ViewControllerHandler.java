package calendar.controller.gui;

import calendar.controller.gui.eventdata.ViewEventData;
import calendar.model.CalendarEvent;
import calendar.model.CalendarModel;
import calendar.view.gui.SwingCalendarView;
import java.beans.PropertyChangeEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Handles view-related property change events.
 * Responsible for view changes, day selection, and loading events for different views.
 * Uses PropertyType enum and typed data classes for type safety.
 *
 * @version 2.0
 */
public class ViewControllerHandler implements PropertyChangeHandler {

  private static final Set<PropertyType> HANDLED_PROPERTIES = Set.of(
      PropertyType.VIEW_CHANGED,
      PropertyType.DAY_SELECTED
  );

  private final CalendarModel model;
  private final SwingCalendarView view;

  /**
   * Creates a new view controller handler.
   *
   * @param model the calendar model
   * @param view  the Swing calendar view
   */
  public ViewControllerHandler(CalendarModel model, SwingCalendarView view) {
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
      case VIEW_CHANGED:
        ViewEventData.ViewChanged viewData = 
            (ViewEventData.ViewChanged) EventDataParser.parseViewData(propertyType, evt);
        handleViewChanged(viewData.getViewMode(), viewData.getDate());
        break;

      case DAY_SELECTED:
        handleDaySelected((LocalDate) evt.getNewValue());
        break;

      default:
        throw new IllegalArgumentException("Unhandled property: " + propertyType);
    }
  }

  /**
   * Handles view mode changes (Day, Week, Month).
   */
  private void handleViewChanged(SwingCalendarView.ViewMode viewMode, LocalDate date) {
    if (!model.hasActiveCalendar()) {
      return;
    }

    try {
      List<CalendarEvent> events;

      switch (viewMode) {
        case DAY:
          events = model.getEventsOnDate(date);
          view.displayEvents(events);
          view.displayMessage("Day view: " + date);
          break;

        case WEEK:
          LocalDate weekStart = getStartOfWeek(date);
          LocalDate weekEnd = weekStart.plusDays(6);
          events = model.getEventsInRange(weekStart, weekEnd);
          view.displayEvents(events);
          view.displayMessage("Week view: " + weekStart + " to " + weekEnd);
          break;

        case MONTH:
          YearMonth month = YearMonth.from(date);
          LocalDate monthStart = month.atDay(1);
          LocalDate monthEnd = month.atEndOfMonth();
          events = model.getEventsInRange(monthStart, monthEnd);
          view.displayEvents(events);
          view.displayMessage("Month view: " + month);
          break;

        default:
          throw new IllegalArgumentException("Unknown view mode: " + viewMode);
      }
    } catch (Exception e) {
      view.displayError("Failed to load events: " + e.getMessage());
    }
  }

  /**
   * Handles day selection.
   */
  private void handleDaySelected(LocalDate date) {
    if (!model.hasActiveCalendar()) {
      return;
    }

    try {
      List<CalendarEvent> dayEvents = model.getEventsOnDate(date);
      if (dayEvents.isEmpty()) {
        view.displayMessage("No events on " + date);
      } else {
        view.displayMessage(dayEvents.size() + " event(s) on " + date);
      }
    } catch (Exception e) {
      view.displayError("Failed to load day events: " + e.getMessage());
    }
  }

  /**
   * Calculates the start of the week (Sunday) for a given date.
   *
   * @param date the date to find the week start for
   * @return the date of the Sunday of that week
   */
  private LocalDate getStartOfWeek(LocalDate date) {
    while (date.getDayOfWeek().getValue() != 7) {
      date = date.minusDays(1);
    }
    return date;
  }
}



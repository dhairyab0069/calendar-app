package calendar.controller.gui.eventdata;

import calendar.model.CalendarEvent;

/**
 * Typed data class for event-related property change events.
 * Eliminates unsafe Object[] casting.
 *
 * @version 1.0
 */
public class EventEventData {

  /**
   * Data for edit event operation.
   */
  public static class EditEvent {
    private final CalendarEvent oldEvent;
    private final CalendarEvent newEvent;

    /**
     * Creates a new edit event data.
     *
     * @param oldEvent the original event before editing
     * @param newEvent the modified event after editing
     */
    public EditEvent(CalendarEvent oldEvent, CalendarEvent newEvent) {
      this.oldEvent = oldEvent;
      this.newEvent = newEvent;
    }

    public CalendarEvent getOldEvent() {
      return oldEvent;
    }

    public CalendarEvent getNewEvent() {
      return newEvent;
    }
  }
}


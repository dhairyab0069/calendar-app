package calendar.controller.gui.eventdata;

import calendar.view.gui.SwingCalendarView;
import java.time.LocalDate;

/**
 * Typed data class for view-related property change events.
 * Eliminates unsafe Object[] casting.
 *
 * @version 1.0
 */
public class ViewEventData {

  /**
   * Data for view changed event.
   */
  public static class ViewChanged {
    private final SwingCalendarView.ViewMode viewMode;
    private final LocalDate date;

    /**
     * Creates a new view changed event data.
     *
     * @param viewMode the new view mode (Day, Week, or Month)
     * @param date     the date for the view
     */
    public ViewChanged(SwingCalendarView.ViewMode viewMode, LocalDate date) {
      this.viewMode = viewMode;
      this.date = date;
    }

    public SwingCalendarView.ViewMode getViewMode() {
      return viewMode;
    }

    public LocalDate getDate() {
      return date;
    }
  }
}


package calendar.controller.gui;

import calendar.controller.CalendarController;
import calendar.controller.gui.PropertyType;
import calendar.exceptions.InvalidCommandException;
import calendar.model.CalendarEvent;
import calendar.model.CalendarModel;
import calendar.view.gui.SwingCalendarView;
import calendar.view.gui.panels.CalendarListPanel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Swing-based controller for the calendar GUI application.
 * Coordinates interaction between the Swing view and the calendar model.
 * Delegates specific operations to specialized handlers following Single Responsibility Principle.
 *
 * @version 2.0
 */
public class SwingController implements CalendarController, PropertyChangeListener {

  private final CalendarModel model;
  private final SwingCalendarView view;
  private final List<PropertyChangeHandler> handlers;
  private boolean running = false;

  /**
   * Creates a new SwingController.
   *
   * @param model is the calendar model
   * @param view  is the Swing calendar view
   */
  public SwingController(CalendarModel model, SwingCalendarView view) {
    this.model = model;
    this.view = view;
    this.handlers = createHandlers();
    setupPropertyListeners();
    initializeCalendars();
  }

  /**
   * Creates and initializes the property change handlers.
   *
   * @return list of handlers
   */
  private List<PropertyChangeHandler> createHandlers() {
    List<PropertyChangeHandler> handlerList = new ArrayList<>();
    handlerList.add(new CalendarControllerHandler(model, view));
    handlerList.add(new EventControllerHandler(model, view));
    handlerList.add(new ViewControllerHandler(model, view));
    return handlerList;
  }

  private void setupPropertyListeners() {
    FilteredPropertyChangeListener filteredListener =
        new FilteredPropertyChangeListener(this);

    view.addPropertyChangeListener(filteredListener);

    CalendarListPanel listPanel = view.getCalendarListPanel();
    if (listPanel != null) {
      listPanel.addPropertyChangeListener(filteredListener);
    }

    if (view.getMonthPanel() != null) {
      view.getMonthPanel().addPropertyChangeListener(filteredListener);
    }
  }

  private void initializeCalendars() {
    SwingUtilities.invokeLater(() -> {
      try {
        List<String> calendars = model.listCalendars();
        CalendarListPanel listPanel = view.getCalendarListPanel();

        for (String calendarName : calendars) {
          ZoneId zone = ZoneId.systemDefault();
          if (model.hasActiveCalendar()
              && model.getActiveCalendarName().equals(calendarName)) {
            zone = model.getActiveCalendarZone();
          }
          listPanel.addCalendar(calendarName, zone);
        }

        if (model.hasActiveCalendar()) {
          String activeName = model.getActiveCalendarName();
          view.setActiveCalendarName(activeName);
          loadEventsForCurrentMonth();
        }

      } catch (Exception e) {
        view.displayError("Failed to initialize calendars: " + e.getMessage());
      }
    });
  }

  private void loadEventsForCurrentMonth() {
    if (!model.hasActiveCalendar()) {
      return;
    }

    YearMonth currentMonth = view.getCurrentMonth();
    LocalDate firstDay = currentMonth.atDay(1);
    LocalDate lastDay = currentMonth.atEndOfMonth();

    try {
      List<CalendarEvent> events = model.getEventsInRange(firstDay, lastDay);
      view.displayEvents(events);
      view.displayMessage("Loaded " + events.size() + " events for " + currentMonth);
    } catch (Exception e) {
      view.displayError("Failed to load events: " + e.getMessage());
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    String propertyName = evt.getPropertyName();
    
    // Convert string property name to type-safe enum
    PropertyType propertyType = PropertyType.fromString(propertyName);
    
    if (propertyType == null) {
      // Unknown property - silently ignore (could be system property)
      return;
    }

    // Delegate to appropriate handler
    for (PropertyChangeHandler handler : handlers) {
      if (handler.canHandle(propertyType)) {
        try {
          handler.handle(propertyType, evt);
          return; // Handler processed the event
        } catch (Exception e) {
          view.displayError("Error handling " + propertyType + ": " + e.getMessage());
          return;
        }
      }
    }

    // Property type recognized but no handler found - this shouldn't happen
    // but we'll silently ignore to be safe
  }



  @Override
  public void processCommand(String command) throws InvalidCommandException {
    throw new InvalidCommandException("Text commands not supported in GUI mode");
  }

  @Override
  public void run() {
    if (running) {
      return;
    }

    running = true;

    SwingUtilities.invokeLater(() -> {
      view.setVisible(true);
      view.displayMessage("Virtual Calendar - GUI Mode");

      if (!model.hasActiveCalendar()) {
        view.displayMessage("No calendars found. Create a calendar to get started.");

        int result = JOptionPane.showConfirmDialog(view,
            "No calendars found. Would you like to create one?",
            "Create Calendar",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
          view.displayMessage("Click '+ Create Calendar' to create your first calendar");
        }
      } else {
        view.displayMessage("Using calendar: " + model.getActiveCalendarName());
        loadEventsForCurrentMonth();
      }
    });
  }

}
package calendar.view.gui.panels;

import calendar.model.CalendarEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Panel for displaying a single day view of the calendar.
 * Shows detailed hour-by-hour schedule with events.
 *
 * @version 1.1
 */
public class CalendarDayPanel extends JPanel {

  private static final int HOUR_HEIGHT = 80;
  private static final int TIME_COLUMN_WIDTH = 100;
  private static final Color CURRENT_TIME_COLOR = new Color(255, 100, 100);
  private static final Color EVENT_COLOR = new Color(100, 149, 237);
  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private final PropertyChangeSupport propertyChangeSupport;
  private LocalDate currentDate;
  private List<CalendarEvent> events;
  private JPanel schedulePanel;
  private JScrollPane scrollPane;
  private JLabel dateLabel;
  private JLabel eventCountLabel;
  private Timer currentTimeTimer;


  /**
   * Creates a new calendar day panel.
   */
  public CalendarDayPanel() {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.currentDate = LocalDate.now();
    this.events = new ArrayList<>();

    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    initializeComponents();
    startCurrentTimeIndicator();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    schedulePanel = createSchedulePanel();

    scrollPane = new JScrollPane(schedulePanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    // Scroll to current time or 8 AM
    SwingUtilities.invokeLater(() -> {
      LocalDateTime now = LocalDateTime.now();
      int scrollTo = (now.toLocalDate().equals(currentDate))
          ? now.getHour() * HOUR_HEIGHT : 8 * HOUR_HEIGHT;
      scrollPane.getVerticalScrollBar().setValue(scrollTo - 100);
    });

    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Creates the header panel with date and summary.
   */
  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(new Color(245, 245, 245));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY),
        BorderFactory.createEmptyBorder(10, 20, 10, 20)
    ));

    DateTimeFormatter fullDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    dateLabel = new JLabel(currentDate.format(fullDateFormat));
    dateLabel.setFont(new Font("Arial", Font.BOLD, 18));

    eventCountLabel = new JLabel(getEventCountText());
    eventCountLabel.setFont(new Font("Arial", Font.PLAIN, 14));
    eventCountLabel.setForeground(Color.GRAY);

    panel.add(dateLabel, BorderLayout.WEST);
    panel.add(eventCountLabel, BorderLayout.EAST);

    return panel;
  }

  /**
   * Creates the main schedule panel.
   */
  private JPanel createSchedulePanel() {
    JPanel panel = new JPanel();
    panel.setLayout(null); // Use absolute positioning for events
    panel.setBackground(Color.WHITE);

    // Calculate total height
    int totalHeight = 24 * HOUR_HEIGHT;
    int totalWidth = 800; // Fixed width
    panel.setPreferredSize(new Dimension(totalWidth, totalHeight));

    // Draw hour lines and labels
    for (int hour = 0; hour < 24; hour++) {
      int y = hour * HOUR_HEIGHT;

      // Time label
      JLabel timeLabel = new JLabel(String.format("%02d:00", hour));
      timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
      timeLabel.setForeground(Color.GRAY);
      timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      timeLabel.setBounds(10, y - 10, TIME_COLUMN_WIDTH - 20, 20);
      panel.add(timeLabel);

      // Hour line
      JSeparator separator = new JSeparator();
      separator.setForeground(GRID_COLOR);
      separator.setBounds(TIME_COLUMN_WIDTH, y, totalWidth - TIME_COLUMN_WIDTH, 1);
      panel.add(separator);

      // Half-hour line (dotted)
      JSeparator halfHourSep = new JSeparator();
      halfHourSep.setForeground(new Color(240, 240, 240));
      halfHourSep.setBounds(TIME_COLUMN_WIDTH, y + HOUR_HEIGHT / 2,
          totalWidth - TIME_COLUMN_WIDTH, 1);
      panel.add(halfHourSep);
    }

    return panel;
  }

  /**
   * Updates the display with events.
   */
  private void updateEvents() {
    // Remove old event panels
    Component[] components = schedulePanel.getComponents();
    for (Component comp : components) {
      if (comp instanceof EventPanel) {
        schedulePanel.remove(comp);
      }
    }

    // Sort events by start time
    events.sort(Comparator.comparing(CalendarEvent::getStart));

    // Group overlapping events
    List<List<CalendarEvent>> eventGroups = groupOverlappingEvents(events);

    // Add event panels
    for (List<CalendarEvent> group : eventGroups) {
      addEventGroup(group);
    }

    // Update event count
    eventCountLabel.setText(getEventCountText());

    schedulePanel.revalidate();
    schedulePanel.repaint();
  }

  /**
   * Groups overlapping events for proper display.
   */
  private List<List<CalendarEvent>> groupOverlappingEvents(List<CalendarEvent> sortedEvents) {
    List<List<CalendarEvent>> groups = new ArrayList<>();

    for (CalendarEvent event : sortedEvents) {
      boolean added = false;

      // Try to add to existing group
      for (List<CalendarEvent> group : groups) {
        boolean overlaps = false;
        for (CalendarEvent groupEvent : group) {
          if (eventsOverlap(event, groupEvent)) {
            overlaps = true;
            break;
          }
        }

        if (!overlaps) {
          group.add(event);
          added = true;
          break;
        }
      }

      // Create new group if needed
      if (!added) {
        List<CalendarEvent> newGroup = new ArrayList<>();
        newGroup.add(event);
        groups.add(newGroup);
      }
    }

    return groups;
  }

  /**
   * Checks if two events overlap in time.
   */
  private boolean eventsOverlap(CalendarEvent e1, CalendarEvent e2) {
    return !e1.getEnd().isBefore(e2.getStart()) && !e1.getStart().isAfter(e2.getEnd());
  }

  /**
   * Adds a group of overlapping events to the display.
   */
  private void addEventGroup(List<CalendarEvent> group) {
    int groupSize = group.size();
    int availableWidth = schedulePanel.getWidth() - TIME_COLUMN_WIDTH - 20;
    int eventWidth = availableWidth / groupSize;

    for (int i = 0; i < groupSize; i++) {
      CalendarEvent event = group.get(i);

      // Calculate position
      int startMinutes = event.getStart().getHour() * 60 + event.getStart().getMinute();
      int endMinutes = event.getEnd().getHour() * 60 + event.getEnd().getMinute();
      int duration = endMinutes - startMinutes;

      int y = (startMinutes * HOUR_HEIGHT) / 60;
      int height = Math.max(20, (duration * HOUR_HEIGHT) / 60);
      int x = TIME_COLUMN_WIDTH + (i * eventWidth) + 5;

      // Create event panel
      EventPanel eventPanel = new EventPanel(event);
      eventPanel.setBounds(x, y, eventWidth - 10, height);
      schedulePanel.add(eventPanel);
      schedulePanel.setComponentZOrder(eventPanel, 0); // Bring to front
    }
  }

  /**
   * Starts the current time indicator timer.
   */
  private void startCurrentTimeIndicator() {
    currentTimeTimer = new Timer(60000, e -> { // Update every minute
      if (currentDate.equals(LocalDate.now())) {
        repaint();
      }
    });
    currentTimeTimer.start();
  }

  /**
   * Paints the current time line.
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Draw current time line if viewing today
    if (currentDate.equals(LocalDate.now())) {
      LocalTime now = LocalTime.now();
      int minutes = now.getHour() * 60 + now.getMinute();
      int y = (minutes * HOUR_HEIGHT) / 60;

      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(CURRENT_TIME_COLOR);
      g2.setStroke(new BasicStroke(2));

      // Draw in the scroll pane viewport
      Component viewport = scrollPane.getViewport().getView();
      if (viewport == schedulePanel) {
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        g2.drawLine(TIME_COLUMN_WIDTH, y - viewPosition.y,
            getWidth(), y - viewPosition.y);

        // Draw time indicator
        g2.fillOval(TIME_COLUMN_WIDTH - 5, y - viewPosition.y - 5, 10, 10);
      }
    }
  }

  /**
   * Gets the event count text.
   */
  private String getEventCountText() {
    if (events.isEmpty()) {
      return "No events scheduled";
    } else if (events.size() == 1) {
      return "1 event";
    } else {
      return events.size() + " events";
    }
  }

  /**
   * Sets the date to display.
   */
  public void setDate(LocalDate date) {
    this.currentDate = date;
    initializeComponents();
    updateEvents();
  }

  /**
   * Sets the events to display.
   */
  public void setEvents(List<CalendarEvent> events) {
    this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    updateEvents();
  }

  /**
   * Gets the current date being displayed.
   */
  public LocalDate getCurrentDate() {
    return currentDate;
  }

  /**
   * Adds a property change listener.
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Removes a property change listener.
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * Fires a property change event.
   */
  @Override
  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    if (propertyChangeSupport != null) {
      propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    } else {
      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  /**
   * Cleanup when panel is removed.
   */
  @Override
  public void removeNotify() {
    super.removeNotify();
    if (currentTimeTimer != null) {
      currentTimeTimer.stop();
    }
  }

  /**
   * Panel representing a single event.
   */
  private class EventPanel extends JPanel {
    private final CalendarEvent event;

    public EventPanel(CalendarEvent event) {
      this.event = event;
      setLayout(new BorderLayout());
      setBackground(new Color(EVENT_COLOR.getRed(), EVENT_COLOR.getGreen(),
          EVENT_COLOR.getBlue(), 200));
      setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(EVENT_COLOR.darker(), 1),
          BorderFactory.createEmptyBorder(5, 5, 5, 5)
      ));
      setCursor(new Cursor(Cursor.HAND_CURSOR));

      // Create content
      JLabel titleLabel = new JLabel(event.getSubject());
      titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
      titleLabel.setForeground(Color.WHITE);

      DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
      JLabel timeLabel = new JLabel(
          event.getStart().format(timeFormat) + " - "
              + event.getEnd().format(timeFormat)
      );
      timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
      timeLabel.setForeground(Color.WHITE);

      JPanel contentPanel = new JPanel();
      contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
      contentPanel.setOpaque(false);
      contentPanel.add(titleLabel);
      contentPanel.add(timeLabel);

      if (event.getLocation() != null && !event.getLocation().isEmpty()) {
        JLabel locationLabel = new JLabel("Location: " + event.getLocation());
        locationLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        locationLabel.setForeground(Color.WHITE);
        contentPanel.add(locationLabel);
      }

      add(contentPanel, BorderLayout.NORTH);

      // Add tooltip
      setToolTipText(createTooltip());

      // Add click listener
      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          CalendarDayPanel.this.firePropertyChange("eventSelected", null, event);
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent evt) {
          setBackground(EVENT_COLOR.brighter());
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent evt) {
          setBackground(new Color(EVENT_COLOR.getRed(), EVENT_COLOR.getGreen(),
              EVENT_COLOR.getBlue(), 200));
        }
      });
    }

    private String createTooltip() {
      DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
      return String.format("<html><b>%s</b><br>%s - %s<br>%s%s%s</html>",
          event.getSubject(),
          event.getStart().format(format),
          event.getEnd().format(format),
          event.getLocation() != null ? "Location: " + event.getLocation() + "<br>" : "",
          event.getDescription() != null ? event.getDescription() + "<br>" : "",
          event.isRecurring() ? "Recurring" : ""
      );
    }
  }
}
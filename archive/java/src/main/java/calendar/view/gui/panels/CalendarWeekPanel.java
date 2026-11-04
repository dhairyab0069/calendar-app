package calendar.view.gui.panels;

import calendar.model.CalendarEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Panel for displaying a week view of the calendar.
 * Shows a 7-day grid with hourly time slots for events.
 *
 * @version 1.1
 */
public class CalendarWeekPanel extends JPanel {

  private static final int HOUR_HEIGHT = 60;
  private static final int TIME_COLUMN_WIDTH = 80;
  private static final Color HEADER_COLOR = new Color(70, 130, 180);
  private static final Color GRID_COLOR = new Color(230, 230, 230);
  private static final Color TODAY_HIGHLIGHT = new Color(255, 248, 220);
  private static final Color EVENT_COLOR = new Color(100, 149, 237);
  private final PropertyChangeSupport propertyChangeSupport;
  private LocalDate weekStartDate;
  private Map<LocalDate, List<CalendarEvent>> eventsByDate;
  private JPanel timeGridPanel;
  private JScrollPane scrollPane;


  /**
   * Creates a new calendar week panel.
   */
  public CalendarWeekPanel() {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.weekStartDate = getStartOfWeek(LocalDate.now());
    this.eventsByDate = new HashMap<>();


    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    initializeComponents();
  }

  /**
   * Initializes the panel components.
   */
  private void initializeComponents() {
    // Create header with day labels
    // Create time grid
    timeGridPanel = createTimeGrid();

    // Put time grid in scroll pane
    scrollPane = new JScrollPane(timeGridPanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    // Set initial scroll position to 8 AM
    SwingUtilities.invokeLater(() -> {
      JScrollBar vertical = scrollPane.getVerticalScrollBar();
      vertical.setValue(8 * HOUR_HEIGHT);
    });

    JPanel headerPanel = createHeaderPanel();
    add(headerPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * Creates the header panel with day labels.
   */
  private JPanel createHeaderPanel() {
    JPanel headerPanel = new JPanel(new GridLayout(1, 8));
    headerPanel.setBackground(HEADER_COLOR);
    headerPanel.setPreferredSize(new Dimension(0, 50));

    // Time column header (empty)
    JLabel timeHeader = new JLabel("");
    timeHeader.setBackground(HEADER_COLOR);
    timeHeader.setOpaque(true);
    headerPanel.add(timeHeader);

    // Day headers
    String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday"};
    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("MMM d");
    LocalDate today = LocalDate.now();

    for (int i = 0; i < 7; i++) {
      LocalDate date = weekStartDate.plusDays(i);

      JPanel dayPanel = new JPanel(new BorderLayout());
      dayPanel.setBackground(date.equals(today) ? HEADER_COLOR.brighter() : HEADER_COLOR);
      dayPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.WHITE));

      JLabel dayNameLabel = new JLabel(dayNames[i], SwingConstants.CENTER);
      dayNameLabel.setForeground(Color.WHITE);
      dayNameLabel.setFont(new Font("Arial", Font.BOLD, 12));

      JLabel dateLabel = new JLabel(date.format(dayFormat), SwingConstants.CENTER);
      dateLabel.setForeground(Color.WHITE);
      dateLabel.setFont(new Font("Arial", Font.PLAIN, 11));

      dayPanel.add(dayNameLabel, BorderLayout.CENTER);
      dayPanel.add(dateLabel, BorderLayout.SOUTH);

      headerPanel.add(dayPanel);
    }

    return headerPanel;
  }

  /**
   * Creates the time grid panel.
   */
  private JPanel createTimeGrid() {
    JPanel gridPanel = new JPanel();
    gridPanel.setLayout(null); // Absolute positioning for events
    gridPanel.setBackground(Color.WHITE);

    // Calculate dimensions
    int totalWidth = TIME_COLUMN_WIDTH + (7 * 120); // 120px per day
    int totalHeight = 24 * HOUR_HEIGHT;
    gridPanel.setPreferredSize(new Dimension(totalWidth, totalHeight));

    // Draw hour lines and labels
    for (int hour = 0; hour < 24; hour++) {
      int y = hour * HOUR_HEIGHT;

      // Time label
      JLabel timeLabel = new JLabel(String.format("%02d:00", hour));
      timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
      timeLabel.setForeground(Color.GRAY);
      timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      timeLabel.setBounds(5, y - 8, TIME_COLUMN_WIDTH - 10, 20);
      gridPanel.add(timeLabel);

      // Horizontal line
      JSeparator separator = new JSeparator();
      separator.setForeground(GRID_COLOR);
      separator.setBounds(TIME_COLUMN_WIDTH, y, totalWidth - TIME_COLUMN_WIDTH, 1);
      gridPanel.add(separator);
    }

    // Draw vertical lines for days
    LocalDate today = LocalDate.now();
    for (int day = 0; day <= 7; day++) {
      int x = TIME_COLUMN_WIDTH + (day * 120);

      JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
      separator.setForeground(GRID_COLOR);
      separator.setBounds(x, 0, 1, totalHeight);
      gridPanel.add(separator);

      // Highlight today's column
      if (day < 7) {
        LocalDate date = weekStartDate.plusDays(day);
        if (date.equals(today)) {
          JPanel todayHighlight = new JPanel();
          todayHighlight.setBackground(TODAY_HIGHLIGHT);
          todayHighlight.setOpaque(true);
          todayHighlight.setBounds(x + 1, 0, 119, totalHeight);
          gridPanel.add(todayHighlight);
          gridPanel.setComponentZOrder(todayHighlight, gridPanel.getComponentCount() - 1);
        }
      }
    }

    return gridPanel;
  }

  /**
   * Updates the display with events.
   */
  private void updateEvents() {
    // Remove existing event panels
    Component[] components = timeGridPanel.getComponents();
    for (Component comp : components) {
      if (comp instanceof EventPanel) {
        timeGridPanel.remove(comp);
      }
    }

    // Add event panels
    for (Map.Entry<LocalDate, List<CalendarEvent>> entry : eventsByDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<CalendarEvent> events = entry.getValue();

      // Calculate day index
      long dayIndex = weekStartDate.until(date).getDays();
      if (dayIndex >= 0 && dayIndex < 7) {
        for (CalendarEvent event : events) {
          addEventToGrid(event, (int) dayIndex);
        }
      }
    }

    timeGridPanel.revalidate();
    timeGridPanel.repaint();
  }

  /**
   * Adds an event panel to the time grid.
   */
  private void addEventToGrid(CalendarEvent event, int dayIndex) {
    LocalDateTime startTime = event.getStart();
    LocalDateTime endTime = event.getEnd();

    // Calculate position
    int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
    int endMinutes = endTime.getHour() * 60 + endTime.getMinute();
    int duration = endMinutes - startMinutes;

    int x = TIME_COLUMN_WIDTH + (dayIndex * 120) + 2;
    int y = (startMinutes * HOUR_HEIGHT) / 60;
    int width = 116;
    int height = Math.max(20, (duration * HOUR_HEIGHT) / 60);

    // Create event panel
    EventPanel eventPanel = new EventPanel(event);
    eventPanel.setBounds(x, y, width, height);
    timeGridPanel.add(eventPanel);
    timeGridPanel.setComponentZOrder(eventPanel, 0); // Bring to front
  }

  /**
   * Gets the start of the week (Sunday) for a given date.
   */
  private LocalDate getStartOfWeek(LocalDate date) {
    while (date.getDayOfWeek().getValue() != 7) { // Sunday = 7
      date = date.minusDays(1);
    }
    return date;
  }

  /**
   * Sets the events to display.
   */
  public void setEventsByDate(Map<LocalDate, List<CalendarEvent>> eventsByDate) {
    this.eventsByDate = new HashMap<>(eventsByDate);
    updateEvents();
  }

  /**
   * Gets the current week start date.
   */
  public LocalDate getWeekStartDate() {
    return weekStartDate;
  }

  /**
   * Sets the week start date.
   */
  public void setWeekStartDate(LocalDate date) {
    this.weekStartDate = getStartOfWeek(date);
    initializeComponents();
    updateEvents();
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
   * Panel representing a single event.
   */
  private class EventPanel extends JPanel {
    private final CalendarEvent event;

    public EventPanel(CalendarEvent event) {
      this.event = event;
      setLayout(new BorderLayout());
      setBackground(new Color(EVENT_COLOR.getRed(), EVENT_COLOR.getGreen(),
          EVENT_COLOR.getBlue(), 200));
      setBorder(BorderFactory.createLineBorder(EVENT_COLOR.darker(), 1));
      setCursor(new Cursor(Cursor.HAND_CURSOR));

      // Create content
      DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

      JLabel titleLabel = new JLabel(truncateText(event.getSubject(), 15));
      titleLabel.setFont(new Font("Arial", Font.BOLD, 11));
      titleLabel.setForeground(Color.WHITE);

      JLabel timeLabel = new JLabel(String.format("%s-%s",
          event.getStart().format(timeFormat),
          event.getEnd().format(timeFormat)));
      timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
      timeLabel.setForeground(Color.WHITE);

      JPanel contentPanel = new JPanel();
      contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
      contentPanel.setOpaque(false);
      contentPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
      contentPanel.add(titleLabel);
      contentPanel.add(timeLabel);

      add(contentPanel, BorderLayout.NORTH);

      // Add tooltip
      setToolTipText(createTooltip());

      // Add click listener
      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          CalendarWeekPanel.this.firePropertyChange("eventSelected", null, event);
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
          setBackground(EVENT_COLOR.brighter());
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
          setBackground(new Color(EVENT_COLOR.getRed(), EVENT_COLOR.getGreen(),
              EVENT_COLOR.getBlue(), 200));
        }
      });
    }

    private String truncateText(String text, int maxLength) {
      if (text.length() <= maxLength) {
        return text;
      }
      return text.substring(0, maxLength - 3) + "...";
    }

    private String createTooltip() {
      return String.format("<html><b>%s</b><br>%s - %s<br>%s%s</html>",
          event.getSubject(),
          event.getStart().toLocalTime(),
          event.getEnd().toLocalTime(),
          event.getLocation() != null ? "Location: " + event.getLocation() + "<br>" : "",
          event.getDescription() != null ? event.getDescription() : ""
      );
    }
  }
}
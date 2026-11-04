package calendar.view.gui.panels;

import calendar.model.CalendarEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Panel for displaying a month view of the calendar.
 * Shows a traditional calendar grid with event indicators.
 *
 * @version 1.1
 */
public class CalendarMonthPanel extends JPanel {

  private static final Color HEADER_COLOR = new Color(70, 130, 180);
  private static final Color TODAY_COLOR = new Color(255, 248, 220);
  private static final Color WEEKEND_COLOR = new Color(250, 250, 250);
  private static final Color EVENT_INDICATOR = new Color(46, 204, 113);

  private final PropertyChangeSupport propertyChangeSupport;
  private final Map<LocalDate, List<CalendarEvent>> eventsByDate;
  private YearMonth currentMonth;
  private DayCell[][] dayCells;

  /**
   * Creates a new calendar month panel.
   */
  public CalendarMonthPanel() {

    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.currentMonth = YearMonth.now();
    this.eventsByDate = new HashMap<>();

    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    initializeCalendar();
  }

  private void initializeCalendar() {
    removeAll();

    JPanel headerPanel = new JPanel(new GridLayout(1, 7));
    headerPanel.setBackground(HEADER_COLOR);
    headerPanel.setPreferredSize(new Dimension(0, 30));

    String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    for (String dayName : dayNames) {
      JLabel label = new JLabel(dayName, SwingConstants.CENTER);
      label.setForeground(Color.WHITE);
      label.setFont(new Font("Arial", Font.BOLD, 12));
      headerPanel.add(label);
    }

    JPanel gridPanel = new JPanel(new GridLayout(6, 7));
    gridPanel.setBackground(Color.WHITE);

    dayCells = new DayCell[6][7];
    for (int row = 0; row < 6; row++) {
      for (int col = 0; col < 7; col++) {
        dayCells[row][col] = new DayCell();
        gridPanel.add(dayCells[row][col]);
      }
    }

    add(headerPanel, BorderLayout.NORTH);
    add(gridPanel, BorderLayout.CENTER);

    updateCalendar();
  }

  private void updateCalendar() {
    LocalDate firstOfMonth = currentMonth.atDay(1);
    LocalDate firstDisplayDate = getFirstDisplayDate(firstOfMonth);

    LocalDate currentDate = firstDisplayDate;
    for (int row = 0; row < 6; row++) {
      for (int col = 0; col < 7; col++) {
        dayCells[row][col].setDate(currentDate);
        List<CalendarEvent> dayEvents = eventsByDate.get(currentDate);
        dayCells[row][col].setEvents(dayEvents);
        currentDate = currentDate.plusDays(1);
      }
    }
  }

  private LocalDate getFirstDisplayDate(LocalDate firstOfMonth) {
    DayOfWeek firstDayOfWeek = firstOfMonth.getDayOfWeek();
    if (firstDayOfWeek == DayOfWeek.SUNDAY) {
      return firstOfMonth;
    }
    int daysToSubtract = firstDayOfWeek.getValue() % 7;
    return firstOfMonth.minusDays(daysToSubtract);
  }

  /**
   * Sets the year and month to display on the calendar.
   *
   * @param yearMonth the year and month to display
   */
  public void setYearMonth(YearMonth yearMonth) {
    this.currentMonth = yearMonth;
    updateCalendar();
  }

  /**
   * Sets the list of events to display on the calendar.
   *
   * @param events the list of calendar events
   */
  public void setEvents(List<CalendarEvent> events) {
    eventsByDate.clear();

    if (events != null) {
      for (CalendarEvent event : events) {
        LocalDate date = event.getStart().toLocalDate();
        eventsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
      }
    }

    updateCalendar();
  }

  public YearMonth getCurrentMonth() {
    return currentMonth;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (propertyChangeSupport != null) {
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  @Override
  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    if (propertyChangeSupport != null) {
      propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    } else {
      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  private static class EventListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value instanceof CalendarEvent) {
        CalendarEvent event = (CalendarEvent) value;
        setText(String.format("%s - %s: %s",
            event.getStart().toLocalTime(),
            event.getEnd().toLocalTime(),
            event.getSubject()));
      }

      return this;
    }
  }

  private class DayCell extends JPanel {
    private final JLabel dayLabel;
    private final JPanel eventPanel;
    private LocalDate date;
    private List<CalendarEvent> events;

    public DayCell() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
      setBackground(Color.WHITE);

      dayLabel = new JLabel();
      dayLabel.setFont(new Font("Arial", Font.PLAIN, 12));
      dayLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 0));

      eventPanel = new JPanel();
      eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
      eventPanel.setOpaque(false);

      add(dayLabel, BorderLayout.NORTH);
      add(eventPanel, BorderLayout.CENTER);

      setCursor(new Cursor(Cursor.HAND_CURSOR));

      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          if (date != null && date.getMonth() == currentMonth.getMonth()) {
            handleDayClick();
          }
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent evt) {
          if (date != null && date.getMonth() == currentMonth.getMonth()) {
            setBorder(BorderFactory.createLineBorder(HEADER_COLOR, 2));
          }
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent evt) {
          setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }
      });
    }

    public void setDate(LocalDate date) {
      this.date = date;
      this.events = new ArrayList<>();

      if (date != null) {
        dayLabel.setText(String.valueOf(date.getDayOfMonth()));

        if (date.equals(LocalDate.now())) {
          setBackground(TODAY_COLOR);
          dayLabel.setFont(new Font("Arial", Font.BOLD, 12));
        } else if (date.getMonth() != currentMonth.getMonth()) {
          setBackground(Color.LIGHT_GRAY);
          dayLabel.setForeground(Color.GRAY);
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY
            || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
          setBackground(WEEKEND_COLOR);
        } else {
          setBackground(Color.WHITE);
        }
      }
    }

    public void setEvents(List<CalendarEvent> dayEvents) {
      this.events = dayEvents != null ? dayEvents : new ArrayList<>();
      eventPanel.removeAll();

      if (!events.isEmpty() && date.getMonth() == currentMonth.getMonth()) {
        int count = Math.min(events.size(), 3);
        for (int i = 0; i < count; i++) {
          CalendarEvent event = events.get(i);
          JLabel eventLabel = new JLabel("• " + truncateText(event.getSubject(), 15));
          eventLabel.setFont(new Font("Arial", Font.PLAIN, 10));
          eventLabel.setForeground(EVENT_INDICATOR);
          eventLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
          eventPanel.add(eventLabel);
        }

        if (events.size() > 3) {
          JLabel moreLabel = new JLabel("+" + (events.size() - 3) + " more");
          moreLabel.setFont(new Font("Arial", Font.ITALIC, 9));
          moreLabel.setForeground(Color.GRAY);
          moreLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
          eventPanel.add(moreLabel);
        }
      }

      revalidate();
      repaint();
    }

    private String truncateText(String text, int maxLength) {
      if (text.length() <= maxLength) {
        return text;
      }
      return text.substring(0, maxLength - 3) + "...";
    }

    private void handleDayClick() {
      CalendarMonthPanel.this.firePropertyChange("daySelected", null, date);

      if (!events.isEmpty()) {
        showEventsDialog();
      }
    }

    private void showEventsDialog() {
      JDialog dialog = new JDialog(
          (Frame) SwingUtilities.getWindowAncestor(CalendarMonthPanel.this),
          "Events on " + date,
          true
      );

      dialog.setSize(400, 300);
      dialog.setLocationRelativeTo(CalendarMonthPanel.this);

      JPanel content = new JPanel(new BorderLayout());
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      DefaultListModel<CalendarEvent> listModel = new DefaultListModel<>();
      for (CalendarEvent event : events) {
        listModel.addElement(event);
      }

      JList<CalendarEvent> eventList = new JList<>(listModel);
      eventList.setCellRenderer(new EventListRenderer());
      eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      JScrollPane scrollPane = new JScrollPane(eventList);
      content.add(scrollPane, BorderLayout.CENTER);

      JButton closeButton = new JButton("Close");
      JButton editButton = new JButton("Edit");
      editButton.addActionListener(e -> {
        CalendarEvent selected = eventList.getSelectedValue();
        if (selected != null) {
          CalendarMonthPanel.this.firePropertyChange("eventSelected", null, selected);
          dialog.dispose();
        }
      });

      closeButton.addActionListener(e -> dialog.dispose());

      eventList.addListSelectionListener(e ->
          editButton.setEnabled(eventList.getSelectedValue() != null));

      editButton.setEnabled(false);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(editButton);
      buttonPanel.add(closeButton);

      content.add(buttonPanel, BorderLayout.SOUTH);
      dialog.setContentPane(content);
      dialog.setVisible(true);
    }
  }
}
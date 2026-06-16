package calendar.view.gui.panels;

import calendar.view.gui.dialogs.CalendarDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Panel for displaying and managing multiple calendars.
 * Provides a sidebar interface for calendar selection and management.
 *
 * <p>Features:
 * - List of available calendars with color coding
 * - Calendar creation and editing
 * - Calendar selection for active operations
 * - Timezone display for each calendar
 *
 * @version 1.0
 */
public class CalendarListPanel extends JPanel {

  private static final Color[] CALENDAR_COLORS = {
      new Color(70, 130, 180),   // Steel Blue
      new Color(46, 204, 113),   // Emerald
      new Color(231, 76, 60),    // Alizarin
      new Color(155, 89, 182),   // Amethyst
      new Color(52, 152, 219),   // Peter River
      new Color(241, 196, 15),   // Sun Flower
      new Color(230, 126, 34),   // Carrot
      new Color(149, 165, 166)   // Concrete
  };
  private final Map<String, Color> calendarColors;
  private JList<CalendarItem> calendarList;
  private DefaultListModel<CalendarItem> listModel;
  private JButton createButton;
  private JButton editButton;
  private JButton deleteButton;
  private int colorIndex = 0;

  /**
   * Creates a new calendar list panel.
   */
  public CalendarListPanel() {
    this.calendarColors = new HashMap<>();
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(250, 0));
    setBackground(new Color(245, 245, 245));
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

    initializeComponents();
    layoutComponents();
    setupEventHandlers();
  }

  /**
   * Initializes all components.
   */
  private void initializeComponents() {
    // Calendar list
    listModel = new DefaultListModel<>();
    calendarList = new JList<>(listModel);
    calendarList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    calendarList.setCellRenderer(new CalendarListRenderer());
    calendarList.setFixedCellHeight(45);
    calendarList.setBackground(Color.WHITE);

    // Buttons
    createButton = new JButton("+ Create Calendar");
    editButton = new JButton("Edit");
    deleteButton = new JButton("Delete");

    styleButton(createButton);
    styleButton(editButton);
    styleButton(deleteButton);

    editButton.setEnabled(false);
    deleteButton.setEnabled(false);
  }

  /**
   * Layouts the components.
   */
  private void layoutComponents() {
    // Title
    JLabel titleLabel = new JLabel("My Calendars");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
    titleLabel.setBorder(new EmptyBorder(15, 15, 10, 15));

    // List scroll pane
    JScrollPane scrollPane = new JScrollPane(calendarList);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    // Button panel
    JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 5));
    buttonPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
    buttonPanel.setOpaque(false);
    buttonPanel.add(createButton);
    buttonPanel.add(editButton);
    buttonPanel.add(deleteButton);

    // Main panel
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setOpaque(false);
    mainPanel.add(titleLabel, BorderLayout.NORTH);
    mainPanel.add(scrollPane, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel, BorderLayout.CENTER);
  }

  /**
   * Sets up event handlers.
   */
  private void setupEventHandlers() {
    calendarList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        boolean hasSelection = calendarList.getSelectedValue() != null;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection && listModel.getSize() > 1);

        if (hasSelection) {
          CalendarItem item = calendarList.getSelectedValue();
          firePropertyChange("calendarSelected", null, item.getName());
        }
      }
    });

    createButton.addActionListener(e -> showCreateCalendarDialog());
    editButton.addActionListener(e -> showEditCalendarDialog());
    deleteButton.addActionListener(e -> deleteSelectedCalendar());
  }

  /**
   * Shows the create calendar dialog.
   */
  private void showCreateCalendarDialog() {
    CalendarDialog dialog = new CalendarDialog(
        SwingUtilities.getWindowAncestor(this), null, null);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      String name = dialog.getCalendarName();
      ZoneId timezone = dialog.getTimezone();

      // Fire property change for controller to handle
      firePropertyChange("createCalendar", null,
          new Object[] {name, timezone});
    }
  }

  /**
   * Shows the edit calendar dialog.
   */
  private void showEditCalendarDialog() {
    CalendarItem selected = calendarList.getSelectedValue();
    if (selected == null) {
      return;
    }

    CalendarDialog dialog = new CalendarDialog(
        SwingUtilities.getWindowAncestor(this),
        selected.getName(),
        selected.getTimezone());
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      String newName = dialog.getCalendarName();
      ZoneId newTimezone = dialog.getTimezone();

      // Fire property change for controller to handle
      firePropertyChange("editCalendar",
          new Object[] {selected.getName(), selected.getTimezone()},
          new Object[] {newName, newTimezone});
    }
  }

  /**
   * Deletes the selected calendar.
   */
  private void deleteSelectedCalendar() {
    CalendarItem selected = calendarList.getSelectedValue();
    if (selected == null) {
      return;
    }

    int result = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to delete calendar '" + selected.getName() + "'?\n"
            + "All events in this calendar will be lost.",
        "Delete Calendar",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (result == JOptionPane.YES_OPTION) {
      firePropertyChange("deleteCalendar", selected.getName(), null);
    }
  }

  /**
   * Adds a calendar to the list.
   */
  public void addCalendar(String name, ZoneId timezone) {
    Color color = calendarColors.computeIfAbsent(name, k -> {
      Color c = CALENDAR_COLORS[colorIndex % CALENDAR_COLORS.length];
      colorIndex++;
      return c;
    });

    CalendarItem item = new CalendarItem(name, timezone, color);
    listModel.addElement(item);

    // Auto-select if it's the first calendar
    if (listModel.getSize() == 1) {
      calendarList.setSelectedIndex(0);
    }
  }

  /**
   * Removes a calendar from the list.
   */
  public void removeCalendar(String name) {
    for (int i = 0; i < listModel.getSize(); i++) {
      if (listModel.getElementAt(i).getName().equals(name)) {
        listModel.removeElementAt(i);
        calendarColors.remove(name);
        break;
      }
    }
  }

  /**
   * Updates a calendar in the list.
   */
  public void updateCalendar(String oldName, String newName, ZoneId newTimezone) {
    for (int i = 0; i < listModel.getSize(); i++) {
      CalendarItem item = listModel.getElementAt(i);
      if (item.getName().equals(oldName)) {
        Color color = calendarColors.remove(oldName);
        calendarColors.put(newName, color);

        CalendarItem newItem = new CalendarItem(newName, newTimezone, color);
        listModel.setElementAt(newItem, i);
        break;
      }
    }
  }

  /**
   * Sets the selected calendar by name.
   */
  public void setSelectedCalendar(String name) {
    for (int i = 0; i < listModel.getSize(); i++) {
      if (listModel.getElementAt(i).getName().equals(name)) {
        calendarList.setSelectedIndex(i);
        break;
      }
    }
  }

  /**
   * Gets the color associated with a calendar.
   */
  public Color getCalendarColor(String name) {
    return calendarColors.getOrDefault(name, Color.GRAY);
  }

  /**
   * Gets all calendar names.
   */
  public List<String> getCalendarNames() {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < listModel.getSize(); i++) {
      names.add(listModel.getElementAt(i).getName());
    }
    return names;
  }

  /**
   * Styles a button.
   */
  private void styleButton(JButton button) {
    button.setFont(new Font("Arial", Font.PLAIN, 12));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
  }

  /**
   * Represents a calendar item in the list.
   */
  private static class CalendarItem {
    private final String name;
    private final ZoneId timezone;
    private final Color color;

    public CalendarItem(String name, ZoneId timezone, Color color) {
      this.name = name;
      this.timezone = timezone;
      this.color = color;
    }

    @Override
    public String toString() {
      return name;
    }

    public String getName() {
      return name;
    }

    public ZoneId getTimezone() {
      return timezone;
    }

    public Color getColor() {
      return color;
    }
  }

  /**
   * Custom renderer for calendar items with color coding.
   */
  private static class CalendarListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      JLabel label = (JLabel) super.getListCellRendererComponent(
          list, value, index, isSelected, cellHasFocus);

      if (value instanceof CalendarItem) {
        CalendarItem item = (CalendarItem) value;

        // Create panel with color indicator
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

        // Color indicator
        JLabel colorLabel = new JLabel("  ");
        colorLabel.setOpaque(true);
        colorLabel.setBackground(item.getColor());
        colorLabel.setPreferredSize(new Dimension(5, 20));

        // Calendar name and timezone
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        nameLabel.setBorder(new EmptyBorder(2, 5, 2, 5));

        JLabel tzLabel = new JLabel(item.getTimezone().getId());
        tzLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        tzLabel.setForeground(Color.GRAY);
        tzLabel.setBorder(new EmptyBorder(0, 5, 2, 5));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(tzLabel);

        panel.add(colorLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
      }

      return label;
    }




  }
}
package calendar.view.gui;

import calendar.model.CalendarEvent;
import calendar.view.CalendarView;
import calendar.view.gui.dialogs.EventDialog;
import calendar.view.gui.panels.CalendarDayPanel;
import calendar.view.gui.panels.CalendarListPanel;
import calendar.view.gui.panels.CalendarMonthPanel;
import calendar.view.gui.panels.CalendarWeekPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Swing-based implementation of CalendarView with import/export capabilities.
 *
 * @version 2.1
 */
public class SwingCalendarView extends JFrame implements CalendarView {

  // Core panels
  private CalendarListPanel calendarListPanel;
  private CalendarMonthPanel monthPanel;
  private CalendarWeekPanel weekPanel;
  private CalendarDayPanel dayPanel;
  private JPanel mainContentPanel;
  private JPanel calendarContentPanel;
  private JPanel headerPanel;

  // Navigation components
  private JButton todayButton;
  private JButton prevButton;
  private JButton nextButton;
  private JLabel currentDateLabel;
  private JComboBox<ViewMode> viewModeSelector;

  // Status and messaging
  private JTextArea messageArea;
  private JLabel statusBar;

  // Current state
  private ViewMode currentViewMode = ViewMode.MONTH;
  private LocalDate currentDate;
  private String activeCalendarName;

  /**
   * Creates the Swing calendar view with default settings.
   * In headless mode (for testing), only initializes minimal state.
   */
  public SwingCalendarView() {
    super("Virtual Calendar");
    this.currentDate = LocalDate.now();

    // Check if running in headless mode (for testing)
    if (GraphicsEnvironment.isHeadless()) {
      // Initialize only non-GUI components for testing
      initializeHeadlessMode();
    } else {
      // Full GUI initialization
      initializeFrame();
      createMenuBar();
      createComponents();
      layoutComponents();
      setVisible(true);
    }
  }

  /**
   * Initializes minimal state for headless testing environment.
   */
  private void initializeHeadlessMode() {
    // Initialize only the essential fields that tests might access
    // Don't create any Swing components
    currentDate = LocalDate.now();
    currentViewMode = ViewMode.MONTH;
    activeCalendarName = null;
  }

  /**
   * Initializes the main frame properties.
   */
  private void initializeFrame() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1100, 750);
    setMinimumSize(new Dimension(900, 600));
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());

    try {
      Image icon = new ImageIcon(getClass().getResource("/calendar-icon.png")).getImage();
      setIconImage(icon);
    } catch (Exception e) {
      // Icon not found, continue without it
    }
  }

  /**
   * Creates the menu bar with File menu for import/export.
   */
  private void createMenuBar() {
    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');

    JMenuItem importCsvItem = new JMenuItem("From CSV...");
    JMenuItem importIcalItem = new JMenuItem("From iCal...");

    int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    importCsvItem.addActionListener(e -> handleImport("csv"));
    importIcalItem.addActionListener(e -> handleImport("ical"));
    importCsvItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, modifierKey));
    importIcalItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, modifierKey
        | InputEvent.SHIFT_DOWN_MASK));

    JMenu importMenu = new JMenu("Import Calendar");
    importMenu.add(importCsvItem);
    importMenu.add(importIcalItem);

    JMenuItem exportCsvItem = new JMenuItem("To CSV...");
    JMenuItem exportIcalItem = new JMenuItem("To iCal...");

    exportCsvItem.addActionListener(e -> handleExport("csv"));
    exportIcalItem.addActionListener(e -> handleExport("ical"));
    exportCsvItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, modifierKey));
    exportIcalItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, modifierKey
        | InputEvent.SHIFT_DOWN_MASK));

    JMenu exportMenu = new JMenu("Export Calendar");
    exportMenu.add(exportCsvItem);
    exportMenu.add(exportIcalItem);

    fileMenu.add(importMenu);
    fileMenu.add(exportMenu);
    fileMenu.addSeparator();

    // Exit
    JMenuItem exitItem = new JMenuItem("Exit");
    exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, modifierKey));
    exitItem.addActionListener(e -> System.exit(0));
    fileMenu.add(exitItem);

    // Help Menu
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('H');

    JMenuItem aboutItem = new JMenuItem("About");
    aboutItem.addActionListener(e -> showAboutDialog());
    helpMenu.add(aboutItem);

    JMenuBar menuBar = new JMenuBar();
    menuBar.add(fileMenu);
    menuBar.add(helpMenu);

    setJMenuBar(menuBar);
  }

  /**
   * Handles importing a calendar file.
   */
  private void handleImport(String format) {
    if (activeCalendarName == null) {
      JOptionPane.showMessageDialog(this,
          "Please select a calendar before importing.",
          "No Calendar Selected",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Import Calendar");

    if (format.equals("csv")) {
      fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
    } else {
      fileChooser.setFileFilter(new FileNameExtensionFilter(
          "iCal Files (*.ics, *.ical)", "ics", "ical"));
    }

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      firePropertyChange("importCalendar", null,
          new Object[] {file.getAbsolutePath(), format});
    }
  }

  /**
   * Handles exporting the current calendar.
   */
  private void handleExport(String format) {
    if (activeCalendarName == null) {
      JOptionPane.showMessageDialog(this,
          "Please select a calendar to export.",
          "No Calendar Selected",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Calendar");

    String extension = format.equals("csv") ? "csv" : "ics";
    String description = format.equals("csv") ? "CSV Files" : "iCal Files";
    fileChooser.setFileFilter(new FileNameExtensionFilter(
        description + " (*." + extension + ")", extension));

    // Suggest filename based on calendar name
    String suggestedName = activeCalendarName.replaceAll("[^a-zA-Z0-9-_]", "_")
        + "." + extension;
    fileChooser.setSelectedFile(new File(suggestedName));

    int result = fileChooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      String path = file.getAbsolutePath();

      // Ensure correct extension
      if (!path.toLowerCase().endsWith("." + extension)) {
        path += "." + extension;
      }

      firePropertyChange("exportCalendar", null,
          new Object[] {path, format});
    }
  }

  /**
   * Shows the About dialog.
   */
  private void showAboutDialog() {
    String message = "<html><body style='width: 350px; padding: 10px;'>"
        + "<h2>Virtual Calendar</h2>"
        + "<p><b>Version:</b> 2.1</p>"
        + "<p><b>Features:</b></p>"
        + "<ul>" + "<li>Multiple calendar support with timezones</li>"
        + "<li>Month, Week, and Day views</li>" + "<li>Single and recurring events</li>"
        + "<li>Import/Export (CSV, iCal formats)</li>" + "<li>Event editing and management</li>"
        + "</ul>" + "<p><b>Keyboard Shortcuts:</b></p>" + "<ul>"
        + "<li><b>Ctrl or Commamd(mac) +I</b> - Import calendar from CSV</li>"
        + "<li><b>Ctrl or Commamd(mac) + Shift + I</b> - Import calendar from ICAL/ICS</li>"
        + "<li><b>Ctrl or Command(mac) +E</b> - Export calendar to CSV</li>"
        + "<li><b>Ctrl or Command(mac) +SHIFT + E</b> - Export calendar to file</li>"
        + "<li><b>Ctrl+Q</b> - Exit application</li>" + "</ul>" + "<p><b>Usage:</b></p>" + "<ul>"
        + "<li>Create calendars in the sidebar</li>" + "<li>Click '+ New Event' to add events</li>"
        + "<li>Click on events to edit them</li>" + "<li>Use File menu to import/export</li>"
        + "</ul>" + "</body></html>";

    JOptionPane.showMessageDialog(this,
        message,
        "About Virtual Calendar",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private void createComponents() {
    calendarListPanel = new CalendarListPanel();
    monthPanel = new CalendarMonthPanel();
    weekPanel = new CalendarWeekPanel();
    dayPanel = new CalendarDayPanel();

    todayButton = new JButton("Today");
    prevButton = new JButton("◀");
    nextButton = new JButton("▶");

    viewModeSelector = new JComboBox<>(ViewMode.values());
    viewModeSelector.setSelectedItem(currentViewMode);

    currentDateLabel = new JLabel();
    updateDateLabel();
    currentDateLabel.setFont(new Font("Arial", Font.BOLD, 16));

    messageArea = new JTextArea(3, 40);
    messageArea.setEditable(false);
    messageArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
    messageArea.setBackground(new Color(245, 245, 245));

    statusBar = new JLabel("Ready");
    statusBar.setBorder(new EmptyBorder(5, 10, 5, 10));
    statusBar.setFont(new Font("Arial", Font.PLAIN, 12));
  }

  private void layoutComponents() {
    headerPanel = createHeaderPanel();

    calendarContentPanel = new JPanel(new CardLayout());
    calendarContentPanel.setBackground(Color.WHITE);
    calendarContentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    calendarContentPanel.add(monthPanel, ViewMode.MONTH.name());
    calendarContentPanel.add(weekPanel, ViewMode.WEEK.name());
    calendarContentPanel.add(dayPanel, ViewMode.DAY.name());

    mainContentPanel = new JPanel(new BorderLayout());
    mainContentPanel.setBackground(Color.WHITE);
    mainContentPanel.add(calendarContentPanel, BorderLayout.CENTER);

    JPanel messagePanel = new JPanel(new BorderLayout());
    messagePanel.setBorder(BorderFactory.createCompoundBorder(
        new EmptyBorder(5, 5, 5, 5),
        BorderFactory.createTitledBorder("Messages")
    ));
    messagePanel.setPreferredSize(new Dimension(0, 100));
    JScrollPane messageScroll = new JScrollPane(messageArea);
    messageScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    messagePanel.add(messageScroll, BorderLayout.CENTER);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(headerPanel, BorderLayout.NORTH);
    centerPanel.add(mainContentPanel, BorderLayout.CENTER);
    centerPanel.add(messagePanel, BorderLayout.SOUTH);

    add(calendarListPanel, BorderLayout.WEST);
    add(centerPanel, BorderLayout.CENTER);
    add(statusBar, BorderLayout.SOUTH);
  }

  private JPanel createHeaderPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(new Color(70, 130, 180));
    panel.setBorder(new EmptyBorder(10, 15, 10, 15));
    panel.setPreferredSize(new Dimension(0, 60));

    JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    navPanel.setOpaque(false);

    styleButton(todayButton, Color.WHITE, new Color(255, 255, 255));
    styleButton(prevButton, Color.WHITE, new Color(255, 255, 255));
    styleButton(nextButton, Color.WHITE, new Color(255, 255, 255));

    navPanel.add(todayButton);
    navPanel.add(Box.createHorizontalStrut(20));
    navPanel.add(prevButton);
    navPanel.add(nextButton);
    navPanel.add(Box.createHorizontalStrut(20));

    currentDateLabel.setForeground(Color.WHITE);
    navPanel.add(currentDateLabel);

    JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    centerPanel.setOpaque(false);

    JLabel viewLabel = new JLabel("View: ");
    viewLabel.setForeground(Color.WHITE);
    viewLabel.setFont(new Font("Arial", Font.BOLD, 12));

    viewModeSelector.setFont(new Font("Arial", Font.PLAIN, 12));
    viewModeSelector.setPreferredSize(new Dimension(100, 25));

    centerPanel.add(viewLabel);
    centerPanel.add(viewModeSelector);

    JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    rightPanel.setOpaque(false);

    JButton addEventButton = new JButton("+ New Event");
    styleButton(addEventButton, new Color(46, 204, 113), Color.WHITE);
    addEventButton.addActionListener(e -> showCreateEventDialog());
    rightPanel.add(addEventButton);

    panel.add(navPanel, BorderLayout.WEST);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(rightPanel, BorderLayout.EAST);

    setupNavigationHandlers();

    return panel;
  }

  private void setupNavigationHandlers() {
    todayButton.addActionListener(e -> {
      currentDate = LocalDate.now();
      updateCurrentView();
    });

    prevButton.addActionListener(e -> navigatePrevious());
    nextButton.addActionListener(e -> navigateNext());

    viewModeSelector.addActionListener(e -> {
      currentViewMode = (ViewMode) viewModeSelector.getSelectedItem();
      switchView();
    });
  }

  private void navigatePrevious() {
    switch (currentViewMode) {
      case DAY:
        currentDate = currentDate.minusDays(1);
        break;
      case WEEK:
        currentDate = currentDate.minusWeeks(1);
        break;
      case MONTH:
        currentDate = currentDate.minusMonths(1);
        break;
      default:
        break;
    }
    updateCurrentView();
  }

  private void navigateNext() {
    switch (currentViewMode) {
      case DAY:
        currentDate = currentDate.plusDays(1);
        break;
      case WEEK:
        currentDate = currentDate.plusWeeks(1);
        break;
      case MONTH:
        currentDate = currentDate.plusMonths(1);
        break;
      default:
        break;
    }
    updateCurrentView();
  }

  private void switchView() {
    if (calendarContentPanel != null) {
      CardLayout cl = (CardLayout) calendarContentPanel.getLayout();
      cl.show(calendarContentPanel, currentViewMode.name());
    }
    updateCurrentView();
  }

  private void updateCurrentView() {
    updateDateLabel();

    if (dayPanel != null && weekPanel != null && monthPanel != null) {
      switch (currentViewMode) {
        case DAY:
          dayPanel.setDate(currentDate);
          break;
        case WEEK:
          weekPanel.setWeekStartDate(getStartOfWeek(currentDate));
          break;
        case MONTH:
          monthPanel.setYearMonth(YearMonth.from(currentDate));
          break;
        default:
          break;
      }
    }

    firePropertyChange("viewChanged", null,
        new Object[] {currentViewMode, currentDate});
  }

  private LocalDate getStartOfWeek(LocalDate date) {
    while (date.getDayOfWeek().getValue() != 7) {
      date = date.minusDays(1);
    }
    return date;
  }

  private void updateDateLabel() {
    if (currentDateLabel == null) {
      return; // Skip in headless mode
    }

    DateTimeFormatter monthYearFormat = DateTimeFormatter.ofPattern("MMMM yyyy");
    DateTimeFormatter fullDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    DateTimeFormatter weekFormat = DateTimeFormatter.ofPattern("MMM d, yyyy");

    switch (currentViewMode) {
      case DAY:
        currentDateLabel.setText(currentDate.format(fullDateFormat));
        break;
      case WEEK:
        LocalDate weekStart = getStartOfWeek(currentDate);
        LocalDate weekEnd = weekStart.plusDays(6);
        currentDateLabel.setText(String.format("Week of %s - %s",
            weekStart.format(weekFormat),
            weekEnd.format(weekFormat)));
        break;
      case MONTH:
        currentDateLabel.setText(currentDate.format(monthYearFormat));
        break;
      default:
        break;
    }
  }

  /**
   * Shows the create event dialog.
   */
  private void showCreateEventDialog() {
    EventDialog dialog = new EventDialog(this, null);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      CalendarEvent event = dialog.getEvent();
      firePropertyChange("createEvent", null, event);
    }
  }

  /**
   * Shows the edit event dialog for the specified event.
   *
   * @param event the event to edit
   */
  public void showEditEventDialog(CalendarEvent event) {
    EventDialog dialog = new EventDialog(this, event);
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      CalendarEvent updatedEvent = dialog.getEvent();
      firePropertyChange("editEvent", event, updatedEvent);
    }
  }

  private void styleButton(JButton button, Color bg, Color fg) {
    button.setBackground(bg);
    button.setForeground(fg);
    button.setFocusPainted(false);
    button.setBorderPainted(true);
    button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setFont(new Font("Arial", Font.BOLD, 12));

    button.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseEntered(java.awt.event.MouseEvent e) {
        button.setBackground(bg.brighter());
      }

      @Override
      public void mouseExited(java.awt.event.MouseEvent e) {
        button.setBackground(bg);
      }
    });
  }

  @Override
  public void displayMessage(String message) {
    if (GraphicsEnvironment.isHeadless()) {
      // In headless mode, just print to console or do nothing
      return;
    }

    SwingUtilities.invokeLater(() -> {
      if (message != null && messageArea != null && statusBar != null) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        statusBar.setText(message);

        Timer timer = new Timer(3000, e -> statusBar.setText("Ready"));
        timer.setRepeats(false);
        timer.start();
      }
    });
  }

  @Override
  public void displayError(String error) {
    if (GraphicsEnvironment.isHeadless()) {
      // In headless mode, just print to console or do nothing
      return;
    }

    SwingUtilities.invokeLater(() -> {
      if (error != null && messageArea != null && statusBar != null) {
        messageArea.append("ERROR: " + error + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        statusBar.setText("Error: " + error);

        JOptionPane.showMessageDialog(this,
            error,
            "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    });
  }

  @Override
  public void displayEvents(List<CalendarEvent> events) {
    if (GraphicsEnvironment.isHeadless()) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      if (events != null) {
        switch (currentViewMode) {
          case DAY:
            if (dayPanel != null) {
              dayPanel.setEvents(events);
            }
            break;
          case WEEK:
            if (weekPanel != null) {
              Map<LocalDate, List<CalendarEvent>> eventsByDate = new HashMap<>();
              for (CalendarEvent event : events) {
                LocalDate date = event.getStart().toLocalDate();
                eventsByDate.computeIfAbsent(date, k -> new java.util.ArrayList<>())
                    .add(event);
              }
              weekPanel.setEventsByDate(eventsByDate);
            }
            break;
          case MONTH:
            if (monthPanel != null) {
              monthPanel.setEvents(events);
            }
            break;
          default:
            break;
        }
      }
    });
  }

  public CalendarListPanel getCalendarListPanel() {
    return calendarListPanel;
  }

  public CalendarMonthPanel getMonthPanel() {
    return monthPanel;
  }

  public CalendarWeekPanel getWeekPanel() {
    return weekPanel;
  }

  public CalendarDayPanel getDayPanel() {
    return dayPanel;
  }

  public ViewMode getCurrentViewMode() {
    return currentViewMode;
  }

  public LocalDate getCurrentDate() {
    return currentDate;
  }

  public YearMonth getCurrentMonth() {
    return YearMonth.from(currentDate);
  }

  public String getActiveCalendarName() {
    return activeCalendarName;
  }

  /**
   * Sets the active calendar by name.
   *
   * @param name the name of the calendar to set as active
   */
  public void setActiveCalendarName(String name) {
    this.activeCalendarName = name;
    if (calendarListPanel != null) {
      calendarListPanel.setSelectedCalendar(name);
    }
    if (statusBar != null) {
      statusBar.setText("Using calendar: " + name);
    }
  }

  /**
   * Refreshes the current view to reflect any data changes.
   */
  public void refresh() {
    if (GraphicsEnvironment.isHeadless()) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      updateCurrentView();
      repaint();
    });
  }

  /**
   * Enum representing the different calendar view modes.
   */
  public enum ViewMode {
    MONTH("Month"),
    WEEK("Week"),
    DAY("Day");

    private final String displayName;

    ViewMode(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }
}
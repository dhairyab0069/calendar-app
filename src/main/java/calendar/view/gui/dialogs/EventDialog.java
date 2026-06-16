package calendar.view.gui.dialogs;

import calendar.model.CalendarEvent;
import calendar.model.EventStatus;
import calendar.model.Weekday;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * Dialog for creating and editing calendar events.
 * Supports both single and recurring events with full validation.
 *
 * @version 1.0
 */
public class EventDialog extends JDialog {

  // State
  private final CalendarEvent originalEvent;
  // Basic fields
  private JTextField subjectField;
  private JTextField locationField;
  private JTextArea descriptionArea;
  private JTextField dateField;
  private JTextField startTimeField;
  private JTextField endTimeField;
  private JComboBox<EventStatus> statusCombo;
  // Recurrence fields
  private JCheckBox recurringCheckbox;
  private JPanel recurrencePanel;
  private JCheckBox[] dayCheckboxes;
  private JRadioButton repeatCountRadio;
  private JRadioButton repeatUntilRadio;
  private JSpinner repeatCountSpinner;
  private JTextField repeatUntilField;
  // Buttons
  private JButton saveButton;
  private JButton cancelButton;
  private CalendarEvent resultEvent;
  private boolean confirmed = false;

  /**
   * Creates a dialog for creating a new event.
   */
  public EventDialog(Window parent, CalendarEvent event) {
    super(parent, event == null ? "New Event" : "Edit Event",
        Dialog.ModalityType.APPLICATION_MODAL);

    this.originalEvent = event;

    setSize(500, 600);
    setResizable(false);
    setLocationRelativeTo(parent);

    initializeComponents();
    layoutComponents();
    setupEventHandlers();

    if (event != null) {
      populateFields(event);
    } else {
      setDefaultValues();
    }
  }

  /**
   * Initializes all dialog components.
   */
  private void initializeComponents() {
    // Basic fields
    subjectField = new JTextField(30);
    locationField = new JTextField(30);
    descriptionArea = new JTextArea(3, 30);
    descriptionArea.setLineWrap(true);
    descriptionArea.setWrapStyleWord(true);

    dateField = new JTextField(12);
    startTimeField = new JTextField(8);
    endTimeField = new JTextField(8);

    statusCombo = new JComboBox<>(EventStatus.values());

    // Recurrence components
    recurringCheckbox = new JCheckBox("Recurring event");

    String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    dayCheckboxes = new JCheckBox[7];
    for (int i = 0; i < 7; i++) {
      dayCheckboxes[i] = new JCheckBox(dayNames[i]);
    }

    repeatCountRadio = new JRadioButton("After");
    repeatUntilRadio = new JRadioButton("Until");
    ButtonGroup group = new ButtonGroup();
    group.add(repeatCountRadio);
    group.add(repeatUntilRadio);
    repeatCountRadio.setSelected(true);

    repeatCountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 365, 1));
    repeatUntilField = new JTextField(12);
    repeatUntilField.setEnabled(false);

    // Buttons
    saveButton = new JButton("Save");
    cancelButton = new JButton("Cancel");
  }

  /**
   * Layouts all components in the dialog.
   */
  private void layoutComponents() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

    // Basic Information Section
    mainPanel.add(createSectionPanel("Event Information", createBasicInfoPanel()));
    mainPanel.add(Box.createVerticalStrut(15));

    // Date and Time Section
    mainPanel.add(createSectionPanel("Date and Time", createDateTimePanel()));
    mainPanel.add(Box.createVerticalStrut(15));

    // Recurrence Section
    JPanel recurrenceSection = new JPanel(new BorderLayout());
    recurrenceSection.add(recurringCheckbox, BorderLayout.NORTH);
    recurrencePanel = createRecurrencePanel();
    recurrencePanel.setVisible(false);
    recurrenceSection.add(recurrencePanel, BorderLayout.CENTER);
    mainPanel.add(createSectionPanel("Recurrence", recurrenceSection));

    // Scroll pane for main panel
    JScrollPane scrollPane = new JScrollPane(mainPanel);
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
    buttonPanel.add(cancelButton);
    buttonPanel.add(saveButton);

    // Main content panel
    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(scrollPane, BorderLayout.CENTER);
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(contentPanel);
  }

  /**
   * Creates a section panel with title.
   */
  private JPanel createSectionPanel(String title, JPanel content) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12)
        ),
        new EmptyBorder(10, 10, 10, 10)
    ));
    panel.add(content, BorderLayout.CENTER);
    return panel;
  }

  /**
   * Creates the basic information panel.
   */
  private JPanel createBasicInfoPanel() {

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Subject
    gbc.gridx = 0;
    gbc.gridy = 0;
    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(new JLabel("Subject:*"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    panel.add(subjectField, gbc);

    // Location
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    panel.add(new JLabel("Location:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    panel.add(locationField, gbc);

    // Status
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    panel.add(new JLabel("Status:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    panel.add(statusCombo, gbc);

    // Description
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    panel.add(new JLabel("Description:"), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    JScrollPane descScroll = new JScrollPane(descriptionArea);
    descScroll.setPreferredSize(new Dimension(0, 60));
    panel.add(descScroll, gbc);

    return panel;
  }

  /**
   * Creates the date and time panel.
   */
  private JPanel createDateTimePanel() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    // Date
    gbc.gridx = 0;
    gbc.gridy = 0;

    JPanel panel = new JPanel(new GridBagLayout());
    panel.add(new JLabel("Date:*"), gbc);
    gbc.gridx = 1;
    dateField.setToolTipText("Format: YYYY-MM-DD");
    panel.add(dateField, gbc);
    gbc.gridx = 2;
    panel.add(new JLabel("(YYYY-MM-DD)"), gbc);

    // Start time
    gbc.gridx = 0;
    gbc.gridy = 1;
    panel.add(new JLabel("Start Time:*"), gbc);
    gbc.gridx = 1;
    startTimeField.setToolTipText("Format: HH:MM");
    panel.add(startTimeField, gbc);
    gbc.gridx = 2;
    panel.add(new JLabel("(HH:MM)"), gbc);

    // End time
    gbc.gridx = 0;
    gbc.gridy = 2;
    panel.add(new JLabel("End Time:*"), gbc);
    gbc.gridx = 1;
    endTimeField.setToolTipText("Format: HH:MM");
    panel.add(endTimeField, gbc);
    gbc.gridx = 2;
    panel.add(new JLabel("(HH:MM)"), gbc);

    return panel;
  }

  /**
   * Creates the recurrence panel.
   */
  private JPanel createRecurrencePanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(new EmptyBorder(10, 20, 10, 10));

    // Days selection
    JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    daysPanel.add(new JLabel("Repeat on:"));
    for (JCheckBox cb : dayCheckboxes) {
      daysPanel.add(cb);
    }
    panel.add(daysPanel);
    panel.add(Box.createVerticalStrut(10));

    // End condition
    JPanel endPanel = new JPanel();
    endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.Y_AXIS));
    endPanel.setBorder(BorderFactory.createTitledBorder("End Condition"));

    // Repeat count option
    JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    countPanel.add(repeatCountRadio);
    countPanel.add(repeatCountSpinner);
    countPanel.add(new JLabel("occurrences"));
    endPanel.add(countPanel);

    // Repeat until option
    JPanel untilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    untilPanel.add(repeatUntilRadio);
    untilPanel.add(repeatUntilField);
    untilPanel.add(new JLabel("(YYYY-MM-DD)"));
    endPanel.add(untilPanel);

    panel.add(endPanel);

    return panel;
  }

  /**
   * Sets up event handlers.
   */
  private void setupEventHandlers() {
    recurringCheckbox.addActionListener(e -> {
      recurrencePanel.setVisible(recurringCheckbox.isSelected());
      pack();
    });

    repeatCountRadio.addActionListener(e -> {
      repeatCountSpinner.setEnabled(true);
      repeatUntilField.setEnabled(false);
    });

    repeatUntilRadio.addActionListener(e -> {
      repeatCountSpinner.setEnabled(false);
      repeatUntilField.setEnabled(true);
    });

    saveButton.addActionListener(e -> handleSave());
    cancelButton.addActionListener(e -> handleCancel());

    // Enter key support
    getRootPane().setDefaultButton(saveButton);
  }

  /**
   * Handles the save action.
   */
  private void handleSave() {
    try {
      validateInput();
      resultEvent = createEventFromInput();
      confirmed = true;
      dispose();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          e.getMessage(),
          "Validation Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Handles the cancel action.
   */
  private void handleCancel() {
    confirmed = false;
    dispose();
  }

  /**
   * Validates the input fields.
   */
  private void validateInput() throws Exception {
    // Check required fields
    if (subjectField.getText().trim().isEmpty()) {
      throw new Exception("Subject is required");
    }

    if (dateField.getText().trim().isEmpty()) {
      throw new Exception("Date is required");
    }

    if (startTimeField.getText().trim().isEmpty()) {
      throw new Exception("Start time is required");
    }

    if (endTimeField.getText().trim().isEmpty()) {
      throw new Exception("End time is required");
    }

    // Validate date format
    try {
      LocalDate.parse(dateField.getText().trim());
    } catch (DateTimeParseException e) {
      throw new Exception("Invalid date format. Use YYYY-MM-DD");
    }

    // Validate time format
    try {
      LocalTime.parse(startTimeField.getText().trim());
      LocalTime.parse(endTimeField.getText().trim());
    } catch (DateTimeParseException e) {
      throw new Exception("Invalid time format. Use HH:MM");
    }

    // Validate times
    LocalTime start = LocalTime.parse(startTimeField.getText().trim());
    LocalTime end = LocalTime.parse(endTimeField.getText().trim());
    if (end.isBefore(start) || end.equals(start)) {
      throw new Exception("End time must be after start time");
    }

    // Validate recurrence
    if (recurringCheckbox.isSelected()) {
      boolean daySelected = false;
      for (JCheckBox cb : dayCheckboxes) {
        if (cb.isSelected()) {
          daySelected = true;
          break;
        }
      }

      if (!daySelected) {
        throw new Exception("Select at least one day for recurrence");
      }

      if (repeatUntilRadio.isSelected()) {
        try {
          LocalDate until = LocalDate.parse(repeatUntilField.getText().trim());
          LocalDate eventDate = LocalDate.parse(dateField.getText().trim());
          if (!until.isAfter(eventDate)) {
            throw new Exception("'Until' date must be after event date");
          }
        } catch (DateTimeParseException e) {
          throw new Exception("Invalid 'until' date format. Use YYYY-MM-DD");
        }
      }
    }
  }

  /**
   * Creates an event from the input fields.
   */
  private CalendarEvent createEventFromInput() {
    String subject = subjectField.getText().trim();
    LocalDate date = LocalDate.parse(dateField.getText().trim());
    LocalTime startTime = LocalTime.parse(startTimeField.getText().trim());
    LocalTime endTime = LocalTime.parse(endTimeField.getText().trim());

    LocalDateTime start = LocalDateTime.of(date, startTime);
    LocalDateTime end = LocalDateTime.of(date, endTime);

    CalendarEvent.Builder builder = CalendarEvent.builder(subject, start, end);

    // Set optional fields
    if (!locationField.getText().trim().isEmpty()) {
      builder.location(locationField.getText().trim());
    }

    if (!descriptionArea.getText().trim().isEmpty()) {
      builder.description(descriptionArea.getText().trim());
    }

    builder.status((EventStatus) statusCombo.getSelectedItem());

    // Handle recurrence
    if (recurringCheckbox.isSelected()) {
      List<Weekday> selectedDays = new ArrayList<>();
      Weekday[] weekdays = {Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY,
          Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY,
          Weekday.SUNDAY};

      for (int i = 0; i < dayCheckboxes.length; i++) {
        if (dayCheckboxes[i].isSelected()) {
          selectedDays.add(weekdays[i]);
        }
      }

      builder.withRecurrence(selectedDays.toArray(new Weekday[0]));

      if (repeatCountRadio.isSelected()) {
        builder.repeatCount((Integer) repeatCountSpinner.getValue());
      } else {
        LocalDate untilDate = LocalDate.parse(repeatUntilField.getText().trim());
        builder.repeatUntil(untilDate.atTime(23, 59));
      }
    }

    return builder.build();
  }

  /**
   * Populates fields with existing event data.
   */
  private void populateFields(CalendarEvent event) {
    subjectField.setText(event.getSubject());
    locationField.setText(event.getLocation() != null ? event.getLocation() : "");
    descriptionArea.setText(event.getDescription() != null ? event.getDescription() : "");

    dateField.setText(event.getStart().toLocalDate().toString());
    startTimeField.setText(event.getStart().toLocalTime().toString());
    endTimeField.setText(event.getEnd().toLocalTime().toString());

    statusCombo.setSelectedItem(event.getStatus());

    // Note: Editing recurring events would need special handling
    // For now, we don't populate recurrence for editing
  }

  /**
   * Sets default values for a new event.
   */
  private void setDefaultValues() {
    LocalDate today = LocalDate.now();
    dateField.setText(today.toString());
    startTimeField.setText("09:00");
    endTimeField.setText("10:00");
    statusCombo.setSelectedItem(EventStatus.PUBLIC);
  }

  /**
   * Returns whether the dialog was confirmed.
   */
  public boolean isConfirmed() {
    return confirmed;
  }

  /**
   * Returns the created/edited event.
   */
  public CalendarEvent getEvent() {
    return resultEvent;
  }
}
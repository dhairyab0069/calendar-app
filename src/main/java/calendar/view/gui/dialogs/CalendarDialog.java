package calendar.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Dialog for creating and editing calendars.
 * Allows users to set calendar name and timezone.
 *
 * @version 1.0
 */
public class CalendarDialog extends JDialog {

  private final String originalName;
  private JTextField nameField;
  private JComboBox<String> timezoneCombo;
  private JButton saveButton;
  private JButton cancelButton;
  private boolean confirmed = false;

  /**
   * Creates a dialog for calendar management.
   *
   * @param parent   parent window
   * @param name     existing calendar name (null for new)
   * @param timezone existing timezone (null for new)
   */
  public CalendarDialog(Window parent, String name, ZoneId timezone) {
    super(parent, name == null ? "Create Calendar" : "Edit Calendar",
        Dialog.ModalityType.APPLICATION_MODAL);

    this.originalName = name;

    setSize(450, 250);
    setResizable(false);
    setLocationRelativeTo(parent);

    initializeComponents();
    layoutComponents();
    setupEventHandlers();

    if (name != null) {
      nameField.setText(name);
    }

    if (timezone != null) {
      timezoneCombo.setSelectedItem(timezone.getId());
    }
  }

  /**
   * Initializes all dialog components.
   */
  private void initializeComponents() {
    nameField = new JTextField(25);

    // Get available timezones and sort them
    Set<String> zoneIds = ZoneId.getAvailableZoneIds();
    String[] sortedZones = zoneIds.toArray(new String[0]);
    Arrays.sort(sortedZones);

    timezoneCombo = new JComboBox<>(sortedZones);
    timezoneCombo.setEditable(true);

    // Set default to system timezone
    timezoneCombo.setSelectedItem(ZoneId.systemDefault().getId());

    // Add auto-complete functionality
    AutoCompleteDecorator.decorate(timezoneCombo);

    saveButton = new JButton("Save");
    cancelButton = new JButton("Cancel");
  }

  /**
   * Layouts all components in the dialog.
   */
  private void layoutComponents() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

    // Form panel

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(10, 10, 10, 10);

    // Calendar name
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    JPanel formPanel = new JPanel(new GridBagLayout());
    formPanel.add(new JLabel("Calendar Name:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(nameField, gbc);

    // Timezone
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Timezone:"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(timezoneCombo, gbc);

    // Helper text
    JPanel helperPanel = new JPanel(new BorderLayout());
    JLabel helperLabel = new JLabel(
        "<html><small>Examples: America/New_York, Europe/London, Asia/Tokyo</small></html>"
    );
    helperLabel.setForeground(Color.GRAY);
    helperPanel.add(helperLabel, BorderLayout.WEST);

    gbc.gridx = 1;
    gbc.gridy = 2;
    formPanel.add(helperPanel, gbc);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(cancelButton);
    buttonPanel.add(saveButton);

    mainPanel.add(formPanel, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    setContentPane(mainPanel);
  }

  /**
   * Sets up event handlers.
   */
  private void setupEventHandlers() {
    saveButton.addActionListener(e -> handleSave());
    cancelButton.addActionListener(e -> handleCancel());

    // Enter key support
    getRootPane().setDefaultButton(saveButton);

    // Focus on name field
    SwingUtilities.invokeLater(() -> nameField.requestFocus());
  }

  /**
   * Handles the save action.
   */
  private void handleSave() {
    String name = nameField.getText().trim();
    String timezoneId = (String) timezoneCombo.getSelectedItem();

    // Validate name
    if (name.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "Calendar name cannot be empty",
          "Validation Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Validate name doesn't contain special characters that might cause issues
    if (!name.matches("[a-zA-Z0-9 _-]+")) {
      JOptionPane.showMessageDialog(this,
          "Calendar name can only contain letters, numbers, spaces, hyphens, and underscores",
          "Validation Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Validate timezone
    try {
      ZoneId.of(timezoneId);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Invalid timezone: " + timezoneId,
          "Validation Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    confirmed = true;
    dispose();
  }

  /**
   * Handles the cancel action.
   */
  private void handleCancel() {
    confirmed = false;
    dispose();
  }

  /**
   * Returns whether the dialog was confirmed.
   */
  public boolean isConfirmed() {
    return confirmed;
  }

  /**
   * Returns the calendar name entered.
   */
  public String getCalendarName() {
    return nameField.getText().trim();
  }

  /**
   * Returns the selected timezone.
   */
  public ZoneId getTimezone() {
    String timezoneId = (String) timezoneCombo.getSelectedItem();
    return ZoneId.of(timezoneId);
  }

  /**
   * Simple auto-complete decorator for combo box.
   */
  private static class AutoCompleteDecorator {
    public static void decorate(JComboBox<String> comboBox) {
      comboBox.setEditable(true);
      JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();

      textField.addKeyListener(new java.awt.event.KeyAdapter() {
        @Override
        public void keyReleased(java.awt.event.KeyEvent e) {
          String text = textField.getText();
          if (text.length() == 0) {
            comboBox.hidePopup();
            comboBox.setSelectedIndex(-1);
          } else {
            DefaultComboBoxModel<String> model =
                (DefaultComboBoxModel<String>) comboBox.getModel();

            String selectedItem = null;
            int caretPosition = textField.getCaretPosition();

            // Find matching items
            for (int i = 0; i < model.getSize(); i++) {
              String item = model.getElementAt(i);
              if (item.toLowerCase().startsWith(text.toLowerCase())) {
                selectedItem = item;
                break;
              }
            }

            if (selectedItem != null) {
              textField.setText(selectedItem);
              textField.setCaretPosition(caretPosition);
              textField.moveCaretPosition(selectedItem.length());
            }
          }
        }
      });
    }
  }
}
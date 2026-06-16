package calendar.controller.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Filters out system-generated property change events.
 * This listener wraps another listener and only forwards application-specific
 * property changes, filtering out Swing system properties.
 *
 * @version 1.0
 */
public class FilteredPropertyChangeListener implements PropertyChangeListener {

  private final PropertyChangeListener delegate;

  /**
   * Creates a new filtered property change listener.
   *
   * @param delegate the listener to forward filtered events to
   */
  public FilteredPropertyChangeListener(PropertyChangeListener delegate) {
    this.delegate = delegate;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    String propertyName = evt.getPropertyName();

    // Filter out system properties
    if (propertyName == null || propertyName.startsWith("Frame.")
        || propertyName.startsWith("ancestor")
        || propertyName.startsWith("JComponent")
        || propertyName.startsWith("Dialog.")
        || propertyName.equals("dropTarget")
        || propertyName.equals("graphicsConfiguration")
        || propertyName.equals("focusOwner")
        || propertyName.equals("permanentFocusOwner")
        || propertyName.equals("activeWindow")) {
      return;
    }

    // Pass through to delegate
    delegate.propertyChange(evt);
  }
}
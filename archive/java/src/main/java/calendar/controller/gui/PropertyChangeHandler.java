package calendar.controller.gui;

import java.beans.PropertyChangeEvent;

/**
 * Interface for handling specific property change events in the GUI.
 * Each handler is responsible for a specific domain of functionality.
 * Uses PropertyType enum for type-safe property identification.
 *
 * @version 2.0
 */
public interface PropertyChangeHandler {

  /**
   * Checks if this handler can handle the given property type.
   *
   * @param propertyType the type of the property that changed
   * @return true if this handler can handle the event, false otherwise
   */
  boolean canHandle(PropertyType propertyType);

  /**
   * Handles the property change event.
   *
   * @param propertyType the type of the property that changed
   * @param evt          the property change event
   * @throws Exception if handling fails
   */
  void handle(PropertyType propertyType, PropertyChangeEvent evt) throws Exception;
}



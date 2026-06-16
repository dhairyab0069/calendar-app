package calendar.controller;

import calendar.exceptions.InvalidCommandException;

/**
 * Interface for calendar application controllers.
 * Defines the contract for processing commands and running the application.
 *
 * <p>Controllers coordinate between the Model (business logic) and View (display).
 * They handle:
 * <ul>
 *   <li>Reading user input</li>
 *   <li>Parsing commands</li>
 *   <li>Executing commands via the model</li>
 *   <li>Directing the view to display results</li>
 * </ul>
 *
 * <p>Implementations support different execution modes:
 * <ul>
 *   <li>Interactive: Read from console, loop until "exit"</li>
 *   <li>Headless: Read from file, process all commands</li>
 * </ul>
 *
 * @version 1.0
 */
public interface CalendarController {

  /**
   * Processes a single command string.
   *
   * @param command the command to process
   * @throws InvalidCommandException if the command is invalid or malformed
   */
  void processCommand(String command) throws InvalidCommandException;

  /**
   * Runs the main application loop.
   *
   * <p>Behavior depends on implementation:
   * <ul>
   *   <li>Interactive: Continuously reads and processes commands until "exit"</li>
   *   <li>Headless: Processes all commands from input source, then exits</li>
   * </ul>
   */
  void run();
}
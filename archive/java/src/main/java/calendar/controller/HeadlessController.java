package calendar.controller;

import calendar.controller.parser.CommandParser;
import calendar.controller.parser.ParsedCommand;
import calendar.exceptions.InvalidCommandException;
import calendar.model.CalendarModel;
import calendar.view.CalendarView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

/**
 * Headless controller for the calendar application.
 * Reads and executes commands from a file sequentially.
 *
 * <p>This controller processes commands in batch mode:
 * <ul>
 *   <li>Reads all commands from specified file</li>
 *   <li>Executes each command in order</li>
 *   <li>Does not require user interaction</li>
 *   <li>Exits after processing all commands or encountering "exit"</li>
 * </ul>
 *
 * <p>If the file does not end with an "exit" command, displays an error
 * but still terminates gracefully.
 *
 * @version 1.0
 */
public class HeadlessController implements CalendarController {

  private final CalendarModel model;
  private final CalendarView view;
  private final CommandParser parser;
  private final CommandHandler handler;
  private final String inputFile;

  /**
   * Creates a headless controller.
   *
   * @param model     the calendar model
   * @param view      the calendar view
   * @param inputFile path to the input file containing commands
   */
  public HeadlessController(CalendarModel model, CalendarView view, String inputFile) {
    if (inputFile == null || inputFile.trim().isEmpty()) {
      throw new IllegalArgumentException("Input file path cannot be null or empty");
    }

    this.model = Objects.requireNonNull(model);
    this.view = view;
    this.parser = new CommandParser();
    this.handler = new CommandHandler(model, view);
    this.inputFile = inputFile;
  }

  @Override
  public void processCommand(String command) throws InvalidCommandException {
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);
  }

  @Override
  public void run() {
    view.displayMessage("Calendar Application - Headless Mode");
    view.displayMessage("Reading commands from: " + inputFile + "\n");

    boolean exitCommandFound = false;

    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
      String line;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;

        // Skip empty lines and comments
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }

        view.displayMessage("Executing: " + trimmed);

        try {
          processCommand(trimmed);

          // Check if this was an exit command
          if (trimmed.equalsIgnoreCase("exit")) {
            exitCommandFound = true;
            break;
          }
        } catch (InvalidCommandException e) {
          view.displayError("Line " + lineNumber + ": " + e.getMessage());
        } catch (Exception e) {
          view.displayError("Line " + lineNumber + ": Unexpected error: " + e.getMessage());
        }
      }

      if (!exitCommandFound) {
        view.displayError("Warning: File ended without 'exit' command");
      }

    } catch (IOException e) {
      view.displayError("Failed to read input file: " + e.getMessage());
    }

    view.displayMessage("\nHeadless execution complete.");
  }
}
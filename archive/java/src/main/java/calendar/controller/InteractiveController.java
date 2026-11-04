package calendar.controller;

import calendar.controller.parser.CommandParser;
import calendar.controller.parser.ParsedCommand;
import calendar.exceptions.InvalidCommandException;
import calendar.model.CalendarModel;
import calendar.view.CalendarView;
import java.util.Objects;
import java.util.Scanner;

/**
 * Interactive controller for the calendar application.
 * Reads commands from standard input in a continuous loop.
 *
 * <p>This controller provides an interactive REPL (Read-Eval-Print Loop):
 * <ul>
 *   <li>Read: Gets user input from console</li>
 *   <li>Eval: Parses and executes the command</li>
 *   <li>Print: Displays results via the view</li>
 *   <li>Loop: Repeats until "exit" command</li>
 * </ul>
 *
 * @version 1.0
 */
public class InteractiveController implements CalendarController {

  private final CalendarModel model;
  private final CalendarView view;
  private final CommandParser parser;
  private final CommandHandler handler;
  private final Scanner scanner;

  /**
   * Creates an interactive controller.
   *
   * @param model the calendar model
   * @param view  the calendar view
   */
  public InteractiveController(CalendarModel model, CalendarView view) {
    this.model = Objects.requireNonNull(model);
    this.view = view;
    this.parser = new CommandParser();
    this.handler = new CommandHandler(model, view);
    this.scanner = new Scanner(System.in);
  }

  @Override
  public void processCommand(String command) throws InvalidCommandException {
    ParsedCommand parsed = parser.parse(command);
    handler.execute(parsed);
  }

  @Override
  public void run() {
    view.displayMessage("Calendar Application - Interactive Mode");
    view.displayMessage("Type 'exit' to quit\n");

    boolean running = true;
    while (running) {
      System.out.print("> ");
      System.out.flush();

      if (!scanner.hasNextLine()) {
        break;
      }

      String command = scanner.nextLine();

      if (command.trim().isEmpty()) {
        continue;
      }

      try {
        processCommand(command);

        // Check if exit command
        if (command.trim().equalsIgnoreCase("exit")) {
          running = false;
        }
      } catch (InvalidCommandException e) {
        view.displayError(e.getMessage());
      } catch (Exception e) {
        view.displayError("Unexpected error: " + e.getMessage());
      }
    }

    scanner.close();
  }
}
package calendar.controller;

import calendar.controller.gui.SwingController;
import calendar.model.CalendarModel;
import calendar.model.SparseHashCalendar;
import calendar.view.CalendarView;
import calendar.view.ConsoleView;
import calendar.view.gui.SwingCalendarView;
import java.awt.GraphicsEnvironment;
import java.time.ZoneId;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point for the Virtual Calendar.
 *
 * <p>Supports three execution modes:
 * <ul>
 *   <li><b>GUI</b>: java -jar calendar.jar (default, no arguments)</li>
 *   <li><b>Interactive</b>: java -jar calendar.jar --mode interactive</li>
 *   <li><b>Headless</b>: java -jar calendar.jar --mode headless [filename]</li>
 * </ul>
 *
 * <p>This class wires together all dependencies (Model, View, Controller)
 * following Dependency Inversion Principle.
 *
 * @version 2.0
 */
public class CalendarApp {

  /**
   * Main entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    // Default to GUI mode when no arguments provided (double-click JAR)
    // But show usage if running in headless environment (e.g., tests)
    if (args.length == 0) {
      rungui();
      return;
    }

    // Check for minimum arguments
    if (args.length < 2) {
      System.err.println("Error: Missing required arguments");
      printUsage();
      return;
    }

    // Verify --mode flag
    if (!args[0].equals("--mode")) {
      System.err.println("Error: First argument must be '--mode'");
      printUsage();
      return;
    }

    // Create Model (shared by all modes)
    CalendarModel model = new SparseHashCalendar();

    // Determine execution mode
    String mode = args[1].toLowerCase();

    switch (mode) {
      case "interactive":
        runInteractive(model);
        break;
      case "headless":
        if (args.length < 3) {
          System.err.println("Error: Headless mode requires input file");
          printUsage();
          return;
        }
        runHeadless(model, args[2]);
        break;
      default:
        System.err.println("Error: Unknown mode '" + mode + "'");
        printUsage();
    }
  }

  /**
   * Runs the application in GUI mode.
   */
  private static void rungui() {
    // Set system look and feel
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // Use default look and feel
    }

    // Initialize model with default calendar
    CalendarModel model = new SparseHashCalendar();

    // Create default calendar with system timezone
    try {
      ZoneId systemZone = ZoneId.systemDefault();
      model.createCalendar("Personal", systemZone);
      model.useCalendar("Personal");
    } catch (Exception e) {
      // Model will handle no calendar state
    }

    // Launch GUI on EDT
    SwingUtilities.invokeLater(() -> {
      SwingCalendarView view = new SwingCalendarView();
      CalendarController controller = new SwingController(model, view);
      controller.run();
    });
  }

  /**
   * Runs the application in interactive mode.
   */
  private static void runInteractive(CalendarModel model) {
    CalendarView view = new ConsoleView();
    CalendarController controller = new InteractiveController(model, view);
    controller.run();
  }

  /**
   * Runs the application in headless mode.
   */
  private static void runHeadless(CalendarModel model, String inputFile) {
    CalendarView view = new ConsoleView();
    CalendarController controller = new HeadlessController(model, view, inputFile);
    controller.run();
  }

  /**
   * Displays help information and usage instructions to the user.
   */
  private static void printUsage() {
    System.out.println("Virtual Calendar Application");
    System.out.println();
    System.out.println("Usage:");
    System.out.println("  GUI mode (default):  java -jar calendar.jar");
    System.out.println("  Interactive mode:    java -jar calendar.jar --mode interactive");
    System.out.println(
        "  Headless mode:       java -jar calendar.jar --mode headless <commands.txt>");
    System.out.println();
    System.out.println("GUI Features:");
    System.out.println("  - Multiple calendar support with different timezones");
    System.out.println("  - Month view with navigation");
    System.out.println("  - Create and edit events (single and recurring)");
    System.out.println("  - Export calendars to CSV or iCal format");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  create calendar --name <name> --timezone Area/Location");
    System.out.println("  edit calendar --name <name> --property <name|timezone> <value>");
    System.out.println("  use calendar --name <name>");
    System.out.println("  create <subject> from <datetime> to <datetime>");
    System.out.println(
        "  create <subject> from <datetime> to <datetime> repeats <days> <count> times");
    System.out.println(
        "  create <subject> from <datetime> to <datetime> repeats <days> until <date>");
    System.out.println("  edit event <property> <subject> from <datetime> with <newValue>");
    System.out.println("  print <date>");
    System.out.println("  print from <date> to <date>");
    System.out.println("  view day <date>");
    System.out.println("  view week <start-date>");
    System.out.println("  view month <year-month>");
    System.out.println("  status <datetime>");
    System.out.println("  copy event <subject> on <datetime> --target <calendar> to <datetime>");
    System.out.println("  copy events on <date> --target <calendar> to <date>");
    System.out.println("  copy events between <start> and <end> --target <calendar> to <date>");
    System.out.println("  export <filename> (.csv | .ical | .ics)");
    System.out.println("  exit");
    System.out.println();
    System.out.println("Date/Time formats:");
    System.out.println("  Date: YYYY-MM-DD (e.g., 2025-01-15)");
    System.out.println("  DateTime: YYYY-MM-DDThh:mm (e.g., 2025-01-15T10:30)");
    System.out.println(
        "  Days: M(Monday) T(Tuesday) W(Wednesday) R(Thursday) F(Friday) S(Saturday) U(Sunday)");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java -jar calendar.jar --mode interactive");
    System.out.println("  java -jar calendar.jar --mode headless test_commands.txt");
  }
}
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarApp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for CalendarApp main application entry point.
 *
 * <p>Tests command-line argument handling and application modes.
 *
 * @version 1.0
 */
public class CalendarAppTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private InputStream originalIn;
  private File testFile;

  /**
   * Redirects System.out and System.err before each test.
   */
  @Before
  public void setUpStreams() {
    outContent.reset();
    errContent.reset();
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
    originalIn = System.in;
  }

  /**
   * Test interactive mode processes scripted input and exits cleanly.
   */
  @Test
  public void testInteractiveModeRunsWithScriptedInput() {
    System.setIn(new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8)));
    CalendarApp.main(new String[] {"--mode", "interactive"});

    String output = outContent.toString();
    assertTrue(output.contains("Calendar Application - Interactive Mode"));
    assertTrue(output.contains("Type 'exit' to quit"));
  }

  /**
   * Restores original System.out and System.err after each test.
   */
  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(originalIn);
    if (testFile != null && testFile.exists()) {
      testFile.delete();
    }
  }

  /**
   * Test with only --mode argument - should show error and usage.
   */
  @Test
  public void testNoArguments() {
    CalendarApp.main(new String[] {"--mode"});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("Missing required arguments"));
    assertTrue(output.contains("Virtual Calendar Application"));
    assertTrue(output.contains("Usage:"));
  }

  /**
   * Test with only one argument - should show error and usage.
   */
  @Test
  public void testOnlyOneArgument() {
    CalendarApp.main(new String[] {"--mode"});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("Missing required arguments"));
    assertTrue(output.contains("Virtual Calendar Application"));
  }

  /**
   * Test with wrong first argument - should show error and usage.
   */
  @Test
  public void testWrongFirstArgument() {
    CalendarApp.main(new String[] {"--wrong", "interactive"});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("First argument must be '--mode'"));
    assertTrue(output.contains("Virtual Calendar Application"));
  }

  /**
   * Test with unknown mode - should show error and usage.
   */
  @Test
  public void testUnknownMode() {
    CalendarApp.main(new String[] {"--mode", "batch"});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("Unknown mode 'batch'"));
    assertTrue(output.contains("Virtual Calendar Application"));
  }

  /**
   * Test headless mode without file - should show error and usage.
   */
  @Test
  public void testHeadlessModeWithoutFile() {
    CalendarApp.main(new String[] {"--mode", "headless"});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("Headless mode requires input file"));
    assertTrue(output.contains("Virtual Calendar Application"));
  }

  /**
   * Test headless mode with valid file.
   */
  @Test
  public void testHeadlessModeWithFile() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "headless",
        testFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.contains("Calendar Application - Headless Mode"));
    assertTrue(output.contains("Headless execution complete."));
  }

  /**
   * Test interactive mode - should start interactive controller.
   * Note: This test will hang if run without modification since
   * interactive mode waits for user input. We verify the mode is accepted.
   */
  @Test(timeout = 1000)
  public void testInteractiveModeStartup() {
    // Create a thread to run main and interrupt it quickly
    Thread mainThread = new Thread(() -> {
      CalendarApp.main(new String[] {"--mode", "interactive"});
    });

    mainThread.start();

    // Give it a moment to initialize
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Expected
    }

    // Interrupt the thread
    mainThread.interrupt();

    // Wait a bit for thread to finish
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Expected
    }

    // Verify no error was printed (mode was accepted)
    String error = errContent.toString();
    assertFalse(error.contains("Unknown mode"));
    assertFalse(error.contains("Missing required"));
  }

  /**
   * Test that printUsage displays all required information.
   */
  @Test
  public void testPrintUsageContent() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();
    String newline = System.lineSeparator();

    // Verify header
    assertTrue(output.contains("Virtual Calendar Application"));
    assertTrue(output.contains("Virtual Calendar Application" + newline + newline + "Usage:"));

    // Verify usage section
    assertTrue(output.contains("Usage:"));
    assertTrue(output.contains("Interactive mode"));
    assertTrue(output.contains("Headless mode"));

    // Verify commands section
    assertTrue(output.contains("Commands:"));
    assertTrue(output.contains(newline + newline + "Commands:"));
    assertTrue(output.contains("create calendar"));
    assertTrue(output.contains("edit calendar"));
    assertTrue(output.contains("use calendar"));
    assertTrue(output.contains("create <subject> from <datetime> to <datetime>"));
    assertTrue(output.contains("repeats <days> <count> times"));
    assertTrue(output.contains("repeats <days> until <date>"));
    assertTrue(output.contains("print <date>"));
    assertTrue(output.contains("print from <date> to <date>"));
    assertTrue(output.contains("view day <date>"));
    assertTrue(output.contains("view week <start-date>"));
    assertTrue(output.contains("view month <year-month>"));
    assertTrue(output.contains("status <datetime>"));
    assertTrue(output.contains("copy event"));
    assertTrue(output.contains("copy events on"));
    assertTrue(output.contains("copy events between"));
    assertTrue(output.contains("export <filename>"));
    assertTrue(output.contains("exit"));

    // Verify date/time format section
    assertTrue(output.contains("Date/Time formats:"));
    assertTrue(output.contains(newline + newline + "Date/Time formats:"));
    assertTrue(output.contains("Date: YYYY-MM-DD"));
    assertTrue(output.contains("DateTime: YYYY-MM-DDThh:mm"));
    assertTrue(output.contains(
        "Days: M(Monday) T(Tuesday) W(Wednesday) R(Thursday) F(Friday) S(Saturday) U(Sunday)"));

    // Verify examples section
    assertTrue(output.contains("Examples:"));
    assertTrue(output.contains(newline + newline + "Examples:"));
    assertTrue(output.contains("java -jar calendar.jar --mode interactive"));
    assertTrue(output.contains("java -jar calendar.jar --mode headless test_commands.txt"));
  }

  /**
   * Test mode case insensitivity.
   */
  @Test
  public void testModeCaseInsensitive() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "HEADLESS",
        testFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.contains("Calendar Application - Headless Mode"));
  }

  /**
   * Test interactive mode with lowercase.
   */
  @Test(timeout = 1000)
  public void testInteractiveModeLowercase() {
    Thread mainThread = new Thread(() -> {
      CalendarApp.main(new String[] {"--mode", "INTERACTIVE"});
    });

    mainThread.start();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Expected
    }

    mainThread.interrupt();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Expected
    }

    String error = errContent.toString();
    assertFalse(error.contains("Unknown mode"));
  }

  /**
   * Test that all command syntax is shown in usage.
   */
  @Test
  public void testUsageShowsAllCommands() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("create"));
    assertTrue(output.contains("repeats"));
    assertTrue(output.contains("print from"));
    assertTrue(output.contains("view day"));
    assertTrue(output.contains("view week"));
    assertTrue(output.contains("view month"));
  }

  /**
   * Test that weekday abbreviations are shown.
   */
  @Test
  public void testUsageShowsWeekdayAbbreviations() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("Days:"));
    assertTrue(output.contains("M(Monday)"));
    assertTrue(output.contains("T(Tuesday)"));
    assertTrue(output.contains("W(Wednesday)"));
    assertTrue(output.contains("R(Thursday)"));
    assertTrue(output.contains("F(Friday)"));
    assertTrue(output.contains("S(Saturday)"));
    assertTrue(output.contains("U(Sunday)"));
  }

  /**
   * Test headless mode with non-existent file.
   */
  @Test
  public void testHeadlessModeWithNonExistentFile() {
    CalendarApp.main(new String[] {"--mode", "headless",
        "nonexistent_file_12345.txt"});

    String error = errContent.toString();
    String output = outContent.toString();

    // Should show an error (either from constructor or run method)
    assertTrue(error.length() > 0 || output.length() > 0);
  }

  /**
   * Test that error messages are specific.
   */
  @Test
  public void testSpecificErrorMessages() {
    // Test missing arguments
    CalendarApp.main(new String[] {"--mode"});
    String error1 = errContent.toString();
    assertTrue(error1.contains("Missing required arguments"));

    // Reset streams
    errContent.reset();
    outContent.reset();

    // Test wrong first argument
    CalendarApp.main(new String[] {"wrong", "interactive"});
    String error2 = errContent.toString();
    assertTrue(error2.contains("First argument must be '--mode'"));

    // Reset streams
    errContent.reset();
    outContent.reset();

    // Test unknown mode
    CalendarApp.main(new String[] {"--mode", "unknown"});
    String error3 = errContent.toString();
    assertTrue(error3.contains("Unknown mode"));

    // Reset streams
    errContent.reset();
    outContent.reset();

    // Test headless without file
    CalendarApp.main(new String[] {"--mode", "headless"});
    String error4 = errContent.toString();
    assertTrue(error4.contains("requires input file"));
  }

  /**
   * Test boundary condition: exactly 2 args for headless (missing file).
   */
  @Test
  public void testHeadlessExactlyTwoArgs() {
    CalendarApp.main(new String[] {"--mode", "headless"});

    String error = errContent.toString();
    assertTrue(error.contains("requires input file"));
  }

  /**
   * Test boundary condition: exactly 3 args for headless (valid).
   */
  @Test
  public void testHeadlessExactlyThreeArgs() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "headless",
        testFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.length() > 0);
  }

  /**
   * Test empty mode string.
   */
  @Test
  public void testEmptyMode() {
    CalendarApp.main(new String[] {"--mode", ""});

    String error = errContent.toString();
    String output = outContent.toString();

    assertTrue(error.contains("Unknown mode"));
    assertTrue(output.contains("Virtual Calendar Application"));
  }

  /**
   * Test that printUsage method outputs to System.out, not System.err.
   */
  @Test
  public void testPrintUsageOutputsToStdout() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    // Usage info should be in stdout
    assertTrue(output.contains("Virtual Calendar Application"));
    assertTrue(output.contains("Usage:"));
    assertTrue(output.length() > 500); // Substantial output
  }

  /**
   * Test extra arguments are ignored.
   */
  @Test
  public void testExtraArgumentsIgnored() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "headless",
        testFile.getAbsolutePath(), "extra", "args"});

    String output = outContent.toString();
    assertTrue(output.contains("Headless")
        || output.contains("Goodbye"));
  }

  /**
   * Test mode with mixed case.
   */
  @Test
  public void testModeMixedCase() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "HeAdLeSs",
        testFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.contains("Headless")
        || output.contains("Goodbye"));
  }

  /**
   * Test space in mode argument.
   */
  @Test
  public void testModeWithSpace() {
    CalendarApp.main(new String[] {"--mode", " interactive "});

    String error = errContent.toString();
    assertTrue(error.contains("Unknown mode"));
  }

  /**
   * Test usage shows calendar commands.
   */
  @Test
  public void testUsageShowsCalendarCommands() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("create calendar"));
    assertTrue(output.contains("edit calendar"));
    assertTrue(output.contains("use calendar"));
  }

  /**
   * Test usage shows copy commands.
   */
  @Test
  public void testUsageShowsCopyCommands() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("copy event"));
    assertTrue(output.contains("copy events on"));
    assertTrue(output.contains("copy events between"));
  }

  /**
   * Test usage shows all date formats.
   */
  @Test
  public void testUsageShowsDateFormats() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("YYYY-MM-DD"));
    assertTrue(output.contains("2025-01-15"));
    assertTrue(output.contains("YYYY-MM-DDThh:mm"));
    assertTrue(output.contains("2025-01-15T10:30"));
  }

  /**
   * Test flag variation.
   */
  @Test
  public void testFlagVariation() {
    CalendarApp.main(new String[] {"-mode", "interactive"});

    String error = errContent.toString();
    assertTrue(error.contains("First argument must be '--mode'"));
  }

  /**
   * Test mode only without value.
   */
  @Test
  public void testModeOnlyWithoutValue() {
    CalendarApp.main(new String[] {"--mode"});

    String error = errContent.toString();
    assertTrue(error.contains("Missing required arguments"));
  }

  /**
   * Test headless with relative path.
   */
  @Test
  public void testHeadlessRelativePath() throws IOException {
    testFile = File.createTempFile("test_commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    String relativePath = "./" + testFile.getName();
    File relativeFile = new File(relativePath);
    try (FileWriter writer = new FileWriter(relativeFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "headless", relativePath});

    String output = outContent.toString();
    assertTrue(output.contains("Headless")
        || output.contains("execution complete"));

    relativeFile.delete();
  }

  /**
   * Test printUsage shows repeats syntax.
   */
  @Test
  public void testUsageShowsRepeatsSyntax() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("repeats <days> <count> times"));
    assertTrue(output.contains("repeats <days> until <date>"));
  }

  /**
   * Test printUsage shows edit commands.
   */
  @Test
  public void testUsageShowsEditCommands() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("edit calendar"));
    assertTrue(output.contains("--property"));
  }

  /**
   * Test printUsage shows timezone format.
   */
  @Test
  public void testUsageShowsTimezoneFormat() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains("Area/Location"));
  }

  /**
   * Test printUsage shows export formats.
   */
  @Test
  public void testUsageShowsExportFormats() {
    CalendarApp.main(new String[] {"--mode", "unknown"});

    String output = outContent.toString();

    assertTrue(output.contains(".csv"));
    assertTrue(output.contains(".ical"));
    assertTrue(output.contains(".ics"));
  }

  /**
   * Test case sensitivity of mode flag.
   */
  @Test
  public void testModeFlagCaseSensitive() {
    CalendarApp.main(new String[] {"--MODE", "interactive"});

    String error = errContent.toString();
    assertTrue(error.contains("First argument must be '--mode'"));
  }

  /**
   * Test headless with whitespace in filename.
   */
  @Test
  public void testHeadlessFilenameWithSpaces() throws IOException {
    testFile = File.createTempFile("test commands", ".txt");
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write("exit\n");
    }

    CalendarApp.main(new String[] {"--mode", "headless",
        testFile.getAbsolutePath()});

    String output = outContent.toString();
    assertTrue(output.length() > 0);
  }
}
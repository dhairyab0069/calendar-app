import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import calendar.controller.parser.ExportParams;
import org.junit.Test;

/**
 * Unit tests for {@link ExportParams}.
 */
public class ExportParamsTest {

  @Test(expected = IllegalArgumentException.class)
  public void constructorRejectsBlankFilenames() {
    new ExportParams("   ");
  }

  @Test
  public void reservedWindowsNameGetsPrefixed() {
    ExportParams params = new ExportParams("CON");
    assertEquals("export_CON", params.getFilename());
  }

  @Test
  public void leadingDotGetsPrefixed() {
    ExportParams params = new ExportParams(".hidden.csv");
    assertEquals("export.hidden.csv", params.getFilename());
  }

  @Test
  public void filenamesLongerThanLimitAreTruncated() {
    String longName = "a".repeat(205) + ".csv";
    ExportParams params = new ExportParams(longName);
    assertEquals(200, params.getFilename().length());
  }

  @Test
  public void filenamesAtLimitRemainUntouched() {
    String exactLimit = "b".repeat(200);
    ExportParams params = new ExportParams(exactLimit);
    assertEquals(exactLimit, params.getFilename());
  }

  @Test
  public void allQuotesRemovedFallbacksToGeneratedName() {
    ExportParams params = new ExportParams("\"\"");
    assertTrue(params.getFilename().startsWith("export_"));
  }
}


package calendar.controller.parser;

import java.time.LocalDateTime;

/**
 * Parameters for the {@code copy event} command.
 */
public class CopyEventParams {

  private final String subject;
  private final LocalDateTime sourceStart;
  private final String targetCalendar;
  private final LocalDateTime targetStart;

  /**
   * Creates parameters for {@code copy event} command.
   *
   * @param subject        subject of the source event
   * @param sourceStart    start date/time of the source event
   * @param targetCalendar destination calendar name
   * @param targetStart    start date/time for the copied event
   */
  public CopyEventParams(String subject, LocalDateTime sourceStart,
                         String targetCalendar, LocalDateTime targetStart) {
    this.subject = subject;
    this.sourceStart = sourceStart;
    this.targetCalendar = targetCalendar;
    this.targetStart = targetStart;
  }

  public String getSubject() {
    return subject;
  }

  public LocalDateTime getSourceStart() {
    return sourceStart;
  }

  public String getTargetCalendar() {
    return targetCalendar;
  }

  public LocalDateTime getTargetStart() {
    return targetStart;
  }
}


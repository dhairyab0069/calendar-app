package calendar.controller.command;

import calendar.controller.CommandHandler;

/**
 * Functional command interface bridging parsed commands to concrete handlers.
 */
@FunctionalInterface
public interface Command {

  /**
   * Executes the command using the provided handler, parameters, and context.
   *
   * @param handler the command handler coordinating model/view operations
   * @param params  the parsed parameters for this command
   * @param context additional execution context (e.g., calendar or timezone)
   */
  void execute(CommandHandler handler, Object params, CommandContext context);
}

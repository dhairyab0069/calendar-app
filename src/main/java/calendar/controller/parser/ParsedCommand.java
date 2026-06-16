package calendar.controller.parser;

import calendar.controller.command.CommandContext;

/**
 * Parsed command container.
 */
public class ParsedCommand {
  private final CommandType type;
  private final Object params;
  private final CommandContext context;

  /**
   * Creates a parsed command.
   *
   * @param type    the command type
   * @param params  the command parameters
   * @param context contextual information for execution
   */
  public ParsedCommand(CommandType type, Object params, CommandContext context) {
    this.type = type;
    this.params = params;
    this.context = context == null ? CommandContext.empty() : context;
  }

  /**
   * Creates a parsed command without explicit context (defaults to
   * {@link CommandContext#empty()}).
   *
   * @param type   the command type
   * @param params the command parameters
   */
  public ParsedCommand(CommandType type, Object params) {
    this(type, params, CommandContext.empty());
  }

  public CommandType getType() {
    return type;
  }

  public Object getParams() {
    return params;
  }

  public CommandContext getContext() {
    return context;
  }
}

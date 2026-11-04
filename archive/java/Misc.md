# Misc.md

## Design Changes

This document outlines the design changes made to improve the codebase structure and functionality.

### Builder Pattern Implementation

The Builder pattern was implemented for the parameter classes `CreateEventParams`, `EditEventParams`, and `CopyEventsBetweenParams`. The original constructors remain available for backward compatibility. The new builder API improves code readability and enables validation at build time rather than runtime. This approach also facilitates the addition of new optional parameters without requiring multiple constructor overloads.

### GUI Controller Refactoring

The `SwingController` class had grown to approximately 470 lines and contained a large switch statement that was difficult to maintain. It was refactored into specialized handler classes: `CalendarControllerHandler`, `EventControllerHandler`, and `ViewControllerHandler`. This reduced the main controller to approximately 177 lines, representing a 62% reduction in size. Each handler is responsible for its own domain, which improves testability and maintainability. This refactoring better adheres to the Single Responsibility Principle.

### Type Safety Improvements

Magic strings were replaced with a `PropertyType` enum, and typed data classes were introduced: `CalendarEventData`, `EventEventData`, and `ViewEventData`. Previously, the code relied on unsafe casting with `Object[]` arrays, which was error-prone. The new implementation provides compile-time type checking, enabling earlier detection of bugs. This also makes refactoring safer, as IDEs can properly track references throughout the codebase.

### Encoding Fixes

Emoji characters (📍, 🔄) in `CalendarDayPanel` and `CalendarWeekPanel` were causing compilation errors on Windows systems. These were replaced with plain text equivalents such as "Location: " and "Recurring". While this reduces visual appeal, it ensures compatibility across all platforms.

### Import/Export Functionality

Import functionality was added for CSV, ICAL, and ICS formats. This enables users to import calendar data from other applications, improving interoperability. The model layer handles parsing these formats and adding events to the active calendar.

### Keyboard Shortcuts

Keyboard shortcuts were implemented for import/export operations:
- Ctrl/Cmd + I for CSV import
- Ctrl/Cmd + Shift + I for ICAL/ICS import  
- Ctrl/Cmd + E for CSV export
- Ctrl/Cmd + Shift + E for ICAL/ICS export
- Ctrl/Cmd + Q to quit

These shortcuts improve efficiency for users familiar with the application.

### Multiple View Support

Day, Week, and Month views were implemented in the GUI. Users can switch between these views to examine their calendar data from different perspectives. The month view serves as the default, while day and week views provide more detailed views when needed.

## Features That Work

### Command Line Interface

All command-line features are functional:
- Creating calendars with timezones
- Editing calendar names and timezones
- Switching between calendars
- Creating single events, all-day events, and recurring events (with count or until date)
- Editing single events and event series (forward and all)
- Printing events on a date or in a date range
- Viewing day/week/month
- Checking status/availability
- Copying events (single, on a date, or between dates)
- Exporting to CSV and iCal/ICS formats
- Exit command

### GUI Features

The GUI implementation is complete:
- Creating new calendars with timezone selection
- Selecting calendars from the list
- Day, Week, and Month views are all functional
- Creating events (both single and recurring) through the dialog interface
- Editing events through the dialog with options for editing a single occurrence, forward occurrences, or all occurrences in a series
- Importing from CSV and iCal/ICS through the File menu
- Exporting to CSV and iCal/ICS through the File menu
- All keyboard shortcuts are functional
- Help menu with About dialog that displays available shortcuts
- Default calendar is created automatically on startup to allow immediate use

## Features That Do Not Work

No major issues have been identified. All core features are functional. Some edge cases may not have been tested thoroughly, but the main functionality operates as expected.

## Additional Notes for Grading

**Backward Compatibility**: All changes maintain backward compatibility. Original constructors remain functional, ensuring that existing code and tests continue to work without modification.

**Code Quality**: The codebase follows SOLID principles and incorporates appropriate design patterns (Builder, Handler, Strategy). Code smells such as magic strings, unsafe casting, and large switch statements have been addressed. The overall code quality has been improved.

**Testing**: The following areas have been tested:
- Command parsing (valid and invalid commands)
- Error handling for invalid input
- Event creation and editing
- Calendar management
- Import/export functionality
- GUI operations

While comprehensive testing has been conducted, additional testing could always be performed to cover more edge cases.

**Platform Independence**: The application is designed to work on Windows, macOS, and Linux. Platform-specific code has been avoided, and Java's standard libraries are used for file paths and related operations.

**Error Handling**: The application provides clear error messages for various failure scenarios, including invalid commands, missing parameters, invalid dates/times, conflicting events, and file I/O errors. These messages should help users understand what went wrong.

**GUI Design**: The GUI was designed with usability in mind:
- Default calendar is created automatically so users can begin immediately
- Calendars are visually distinct through color coding
- Menu structure is intuitive
- Keyboard shortcuts are available for common operations
- Dialog boxes are clear and straightforward

The application should be ready for use. Please report any issues that are discovered during testing.

# Calendar Application Usage Guide

## Building the Application

To build the calendar application, run:
```bash
./gradlew jar
```

This will create a JAR file in the `build/libs/` directory.

## Running the Application

### GUI Mode (Default)

To run the application in GUI mode:
```bash
java -jar build/libs/calendar-1.0.jar
```

Or explicitly:
```bash
java -jar build/libs/calendar-1.0.jar --mode gui
```

The GUI will open with a default calendar in your system's timezone. You can immediately start creating events and managing calendars through the graphical interface.

### Interactive Mode

To run the application in interactive mode:
```bash
java -jar build/libs/calendar-1.0.jar --mode interactive
```

In interactive mode, you can type commands and see results immediately. Type `exit` to quit.

### Headless Mode

To run the application in headless mode with a commands file:
```bash
java -jar build/libs/calendar-1.0.jar --mode headless test_commands.txt
```

The commands file should contain one command per line, with the last command being `exit`.

## Example Commands

### Managing Calendars

Create a calendar with timezone:
```
create calendar --name "Work" --timezone America/New_York
```

Rename a calendar:
```
edit calendar --name Work --property name "Work 2025"
```

Change calendar timezone:
```
edit calendar --name "Work 2025" --property timezone Europe/London
```

Switch the active calendar:
```
use calendar --name "Work 2025"
```

### Creating Events

Create a single event:
```
create event "Team Meeting" from 2025-01-15T10:00 to 2025-01-15T11:00
```

Create an all-day event:
```
create event "Company Holiday" on 2025-01-20
```

Create a recurring event series:
```
create event "Weekly Standup" from 2025-01-06T09:00 to 2025-01-06T09:30 repeats MWF for 4 times
```

Create a recurring event until a specific date:
```
create event "Monthly Review" from 2025-01-01T15:00 to 2025-01-01T16:00 repeats M until 2025-06-01
```

### Copying Events Across Calendars

Copy a single event to another calendar and start time:
```
copy event "Monthly Review" on 2025-01-15T10:00 --target "Work 2025" to 2025-01-17T09:00
```

Copy all events on a day into another calendar:
```
copy events on 2025-02-01 --target "Work 2025" to 2025-02-05
```

Copy a range of events into another calendar:
```
copy events between 2025-03-01 and 2025-03-31 --target "Work 2025" to 2025-08-01
```

### Editing Events

Edit a single event:
```
edit event subject "Team Meeting" from 2025-01-15T10:00 with "Updated Team Meeting"
```

Edit all events in a series starting from a specific date:
```
edit events location "Weekly Standup" from 2025-01-06T09:00 with "Conference Room B"
```

Edit all events in a series:
```
edit series description "Monthly Review" from 2025-01-06T15:00 with "Monthly team review meeting"
```

### Querying Events

Print events on a specific date:
```
print events on 2025-01-15
```

Print events in a date range:
```
print events from 2025-01-15T08:00 to 2025-01-15T17:00
```

Check availability:
```
show status on 2025-01-15T10:30
```

### Exporting Calendar

Export calendar to CSV:
```
export cal calendar_export.csv
```

Export calendar to iCal:
```
export "Work 2025" calendar_export.ics
```

### Exiting

Exit the application:
```
exit
```

## Command Format

- **Date format**: YYYY-MM-DD (e.g., 2025-01-15)
- **Time format**: HH:mm (e.g., 10:00)
- **DateTime format**: YYYY-MM-DDTHH:mm (e.g., 2025-01-15T10:00)
- **Weekday codes**: M (Monday), T (Tuesday), W (Wednesday), R (Thursday), F (Friday), S (Saturday), U (Sunday)
- **Properties**: subject, start, end, description, location, status
- **Edit types**: event (single), events (forward), series (all)

## Error Handling

The application provides clear error messages for:
- Invalid command syntax
- Conflicting events
- Invalid dates/times
- Missing required parameters
- File I/O errors

## GUI Usage Guide

### Using the GUI Interface

The Calendar Application provides a graphical user interface with the following features:

#### Menu Bar

The application has a menu bar with two main menus:

1. **File Menu**
   - Import Calendar
     - From CSV... (Ctrl/Cmd + I)
     - From iCal... (Ctrl/Cmd + Shift + I)
   - Export Calendar
     - To CSV... (Ctrl/Cmd + E)
     - To iCal... (Ctrl/Cmd + Shift + E)
   - Exit (Ctrl/Cmd + Q)

2. **Help Menu**
   - About (displays application information and keyboard shortcuts)

#### Keyboard Shortcuts

- **Ctrl/Cmd + I**: Import calendar from CSV
- **Ctrl/Cmd + Shift + I**: Import calendar from ICAL/ICS
- **Ctrl/Cmd + E**: Export calendar to CSV
- **Ctrl/Cmd + Shift + E**: Export calendar to ICAL/ICS
- **Ctrl/Cmd + Q**: Quit the application

#### GUI Operations

##### Creating a Calendar
- Click the "+" button or use the calendar creation dialog
- Enter calendar name and select timezone
- The new calendar will appear in the calendar list on the left

##### Selecting a Calendar
- Click on a calendar name in the calendar list panel on the left
- The selected calendar will be highlighted
- All operations (create, edit, view) will apply to the selected calendar

##### Viewing Events - Day View
- Click the "Day" view button or select Day from view options
- Click on a specific date in the month view to see that day's events
- Events are displayed chronologically with their times, subjects, and details

##### Viewing Events - Week View
- Click the "Week" view button or select Week from view options
- The week view shows all events for the current week
- Navigate between weeks using the navigation buttons

##### Viewing Events - Month View
- Click the "Month" view button or select Month from view options (default)
- The month view displays a calendar grid with dates
- Days with events are highlighted or marked
- Click on a date to see events for that day

##### Creating an Event
- Select a date by clicking on it in the month/week view, or navigate to a specific day
- Click the "Create Event" button or double-click on a date
- Fill in the event dialog:
  - Subject (required)
  - Start date and time (required)
  - End date and time (required)
  - Description (optional)
  - Location (optional)
  - Status (optional)
- For recurring events:
  - Check the "Recurring" checkbox
  - Select weekdays (M, T, W, R, F, S, U)
  - Choose either "Repeat for X times" or "Repeat until date"
- Click "Create" to add the event

##### Editing an Event
- Navigate to the day containing the event
- Click on the event in the day/week view
- Click the "Edit" button or double-click the event
- Modify the event details in the dialog
- Choose edit scope:
  - "This event only" - edits only the selected occurrence
  - "This and future events" - edits from this occurrence forward
  - "All events in series" - edits all occurrences
- Click "Save" to apply changes

##### Importing a Calendar
- Select the target calendar from the calendar list
- Go to File > Import Calendar
- Choose "From CSV..." or "From iCal..." based on your file format
- Select the file from the file chooser dialog
- Events will be imported into the selected calendar
- A confirmation message will show how many events were imported

##### Exporting a Calendar
- Select the calendar you want to export from the calendar list
- Go to File > Export Calendar
- Choose "To CSV..." or "To iCal..." based on desired format
- Select the destination file in the file chooser dialog
- The calendar will be exported to the selected file
- A confirmation message will appear when export is complete

##### Navigating Between Dates
- Use the arrow buttons (← →) to navigate between months/weeks/days
- Click on the month/year header to jump to a specific date
- Use the "Today" button to quickly return to the current date

##### Viewing Event Details
- Click on any event in the day/week view to see full details
- The event details panel shows:
  - Subject
  - Start and end times
  - Description (if available)
  - Location (if available)
  - Status
  - Recurrence information (if applicable)

##### Switching Between Views
- Use the view buttons (Day, Week, Month) at the top of the calendar panel
- Each view provides a different perspective:
  - **Day View**: Detailed hourly view of a single day
  - **Week View**: Overview of events across a week
  - **Month View**: Calendar grid showing all days in a month

#### Example GUI Workflow

1. **Start the application**: `java -jar build/libs/calendar-1.0.jar`
2. **Create a new calendar**: Click "+" button, enter "Work" as name, select timezone
3. **Select the calendar**: Click "Work" in the calendar list
4. **Create an event**: Click on January 15, 2025, click "Create Event", fill in details, click "Create"
5. **View the event**: The event appears in the month view; click on the date to see it in day view
6. **Edit the event**: Click on the event, click "Edit", modify details, click "Save"
7. **Import events**: File > Import Calendar > From CSV..., select a CSV file
8. **Export calendar**: File > Export Calendar > To CSV..., choose destination file
9. **Switch views**: Click "Week" button to see weekly view, click "Day" for detailed day view

## Platform Independence

All file paths are platform-independent and work on Windows, macOS, and Linux.

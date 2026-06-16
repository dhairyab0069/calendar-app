import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import calendar.controller.gui.CalendarControllerHandler;
import calendar.controller.gui.EventControllerHandler;
import calendar.controller.gui.EventDataParser;
import calendar.controller.gui.FilteredPropertyChangeListener;
import calendar.controller.gui.PropertyChangeHandler;
import calendar.controller.gui.PropertyType;
import calendar.controller.gui.ViewControllerHandler;
import calendar.controller.gui.eventdata.CalendarEventData;
import calendar.controller.gui.eventdata.ViewEventData;
import calendar.view.gui.SwingCalendarView;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Test suite for calendar controller GUI components.
 * Tests data classes, property types, event parsing, and filtering.
 */
public class CalendarControllerTests {

  /**
   * Tests CreateCalendar data class stores name correctly.
   */
  @Test
  public void testCreateCalendarGetName() {
    CalendarEventData.CreateCalendar data =
        new CalendarEventData.CreateCalendar("Work", ZoneId.of("UTC"));
    assertEquals("Work", data.getName());
  }

  /**
   * Tests CreateCalendar data class stores timezone correctly.
   */
  @Test
  public void testCreateCalendarGetTimezone() {
    ZoneId zone = ZoneId.of("America/New_York");
    CalendarEventData.CreateCalendar data = new CalendarEventData.CreateCalendar("Work", zone);
    assertEquals(zone, data.getTimezone());
  }

  /**
   * Tests CreateCalendar with system default timezone.
   */
  @Test
  public void testCreateCalendarSystemDefaultTimezone() {
    ZoneId zone = ZoneId.systemDefault();
    CalendarEventData.CreateCalendar data = new CalendarEventData.CreateCalendar("Default", zone);
    assertEquals(ZoneId.systemDefault(), data.getTimezone());
  }

  /**
   * Tests CreateCalendar with empty name.
   */
  @Test
  public void testCreateCalendarEmptyName() {
    CalendarEventData.CreateCalendar data =
        new CalendarEventData.CreateCalendar("", ZoneId.of("UTC"));
    assertEquals("", data.getName());
  }

  /**
   * Tests CreateCalendar with special characters.
   */
  @Test
  public void testCreateCalendarSpecialCharacters() {
    CalendarEventData.CreateCalendar data =
        new CalendarEventData.CreateCalendar("Work & Personal!", ZoneId.of("UTC"));
    assertEquals("Work & Personal!", data.getName());
  }

  /**
   * Tests EditCalendar data class stores old name correctly.
   */
  @Test
  public void testEditCalendarGetOldName() {
    CalendarEventData.EditCalendar data =
        new CalendarEventData.EditCalendar("Old", "New", ZoneId.of("UTC"));
    assertEquals("Old", data.getOldName());
  }

  /**
   * Tests EditCalendar data class stores new name correctly.
   */
  @Test
  public void testEditCalendarGetNewName() {
    CalendarEventData.EditCalendar data =
        new CalendarEventData.EditCalendar("Old", "New", ZoneId.of("UTC"));
    assertEquals("New", data.getNewName());
  }

  /**
   * Tests EditCalendar data class stores new timezone correctly.
   */
  @Test
  public void testEditCalendarGetNewTimezone() {
    ZoneId zone = ZoneId.of("Europe/London");
    CalendarEventData.EditCalendar data = new CalendarEventData.EditCalendar("Old", "New", zone);
    assertEquals(zone, data.getNewTimezone());
  }

  /**
   * Tests EditCalendar when names are same.
   */
  @Test
  public void testEditCalendarSameName() {
    CalendarEventData.EditCalendar data =
        new CalendarEventData.EditCalendar("Same", "Same", ZoneId.of("UTC"));
    assertEquals(data.getOldName(), data.getNewName());
  }

  /**
   * Tests EditCalendar with empty old name.
   */
  @Test
  public void testEditCalendarEmptyOldName() {
    CalendarEventData.EditCalendar data =
        new CalendarEventData.EditCalendar("", "New", ZoneId.of("UTC"));
    assertEquals("", data.getOldName());
  }

  /**
   * Tests EditCalendar with empty new name.
   */
  @Test
  public void testEditCalendarEmptyNewName() {
    CalendarEventData.EditCalendar data =
        new CalendarEventData.EditCalendar("Old", "", ZoneId.of("UTC"));
    assertEquals("", data.getNewName());
  }

  /**
   * Tests ExportCalendar data class stores filename correctly.
   */
  @Test
  public void testExportCalendarGetFilename() {
    CalendarEventData.ExportCalendar data =
        new CalendarEventData.ExportCalendar("/path/export.csv", "csv");
    assertEquals("/path/export.csv", data.getFilename());
  }

  /**
   * Tests ExportCalendar data class stores csv format correctly.
   */
  @Test
  public void testExportCalendarGetFormatCsv() {
    CalendarEventData.ExportCalendar data =
        new CalendarEventData.ExportCalendar("/path/export.csv", "csv");
    assertEquals("csv", data.getFormat());
  }

  /**
   * Tests ExportCalendar data class stores ical format correctly.
   */
  @Test
  public void testExportCalendarGetFormatIcal() {
    CalendarEventData.ExportCalendar data =
        new CalendarEventData.ExportCalendar("/path/export.ics", "ical");
    assertEquals("ical", data.getFormat());
  }

  /**
   * Tests ExportCalendar with absolute path.
   */
  @Test
  public void testExportCalendarAbsolutePath() {
    CalendarEventData.ExportCalendar data =
        new CalendarEventData.ExportCalendar("/home/user/calendar.csv", "csv");
    assertEquals("/home/user/calendar.csv", data.getFilename());
  }

  /**
   * Tests ExportCalendar with relative path.
   */
  @Test
  public void testExportCalendarRelativePath() {
    CalendarEventData.ExportCalendar data =
        new CalendarEventData.ExportCalendar("./export/cal.csv", "csv");
    assertEquals("./export/cal.csv", data.getFilename());
  }

  /**
   * Tests ImportCalendar data class stores filename correctly.
   */
  @Test
  public void testImportCalendarGetFilename() {
    CalendarEventData.ImportCalendar data =
        new CalendarEventData.ImportCalendar("/path/import.csv", "csv");
    assertEquals("/path/import.csv", data.getFilename());
  }

  /**
   * Tests ImportCalendar data class stores csv format correctly.
   */
  @Test
  public void testImportCalendarGetFormatCsv() {
    CalendarEventData.ImportCalendar data =
        new CalendarEventData.ImportCalendar("/path/import.csv", "csv");
    assertEquals("csv", data.getFormat());
  }

  /**
   * Tests ImportCalendar data class stores ical format correctly.
   */
  @Test
  public void testImportCalendarGetFormatIcal() {
    CalendarEventData.ImportCalendar data =
        new CalendarEventData.ImportCalendar("/path/import.ics", "ical");
    assertEquals("ical", data.getFormat());
  }

  /**
   * Tests ImportCalendar with Windows path.
   */
  @Test
  public void testImportCalendarWindowsPath() {
    CalendarEventData.ImportCalendar data =
        new CalendarEventData.ImportCalendar("C:\\Users\\calendar.csv", "csv");
    assertEquals("C:\\Users\\calendar.csv", data.getFilename());
  }

  /**
   * Tests ViewChanged data class stores DAY view mode correctly.
   */
  @Test
  public void testViewChangedGetViewModeDayMode() {
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.DAY, LocalDate.now());
    assertEquals(SwingCalendarView.ViewMode.DAY, data.getViewMode());
  }

  /**
   * Tests ViewChanged data class stores WEEK view mode correctly.
   */
  @Test
  public void testViewChangedGetViewModeWeekMode() {
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.WEEK, LocalDate.now());
    assertEquals(SwingCalendarView.ViewMode.WEEK, data.getViewMode());
  }

  /**
   * Tests ViewChanged data class stores MONTH view mode correctly.
   */
  @Test
  public void testViewChangedGetViewModeMonthMode() {
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.MONTH, LocalDate.now());
    assertEquals(SwingCalendarView.ViewMode.MONTH, data.getViewMode());
  }

  /**
   * Tests ViewChanged data class stores date correctly.
   */
  @Test
  public void testViewChangedGetDate() {
    LocalDate date = LocalDate.of(2024, 6, 15);
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.DAY, date);
    assertEquals(date, data.getDate());
  }

  /**
   * Tests ViewChanged with current date.
   */
  @Test
  public void testViewChangedCurrentDate() {
    LocalDate today = LocalDate.now();
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.MONTH, today);
    assertEquals(today, data.getDate());
  }

  /**
   * Tests ViewChanged with past date.
   */
  @Test
  public void testViewChangedPastDate() {
    LocalDate pastDate = LocalDate.of(2020, 1, 1);
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.WEEK, pastDate);
    assertEquals(pastDate, data.getDate());
  }

  /**
   * Tests ViewChanged with future date.
   */
  @Test
  public void testViewChangedFutureDate() {
    LocalDate futureDate = LocalDate.of(2030, 12, 31);
    ViewEventData.ViewChanged data =
        new ViewEventData.ViewChanged(SwingCalendarView.ViewMode.DAY, futureDate);
    assertEquals(futureDate, data.getDate());
  }

  /**
   * Tests PropertyType fromString for calendarSelected.
   */
  @Test
  public void testPropertyTypeFromStringCalendarSelected() {
    assertEquals(PropertyType.CALENDAR_SELECTED, PropertyType.fromString("calendarSelected"));
  }

  /**
   * Tests PropertyType fromString for createCalendar.
   */
  @Test
  public void testPropertyTypeFromStringCreateCalendar() {
    assertEquals(PropertyType.CREATE_CALENDAR, PropertyType.fromString("createCalendar"));
  }

  /**
   * Tests PropertyType fromString for editCalendar.
   */
  @Test
  public void testPropertyTypeFromStringEditCalendar() {
    assertEquals(PropertyType.EDIT_CALENDAR, PropertyType.fromString("editCalendar"));
  }

  /**
   * Tests PropertyType fromString for exportCalendar.
   */
  @Test
  public void testPropertyTypeFromStringExportCalendar() {
    assertEquals(PropertyType.EXPORT_CALENDAR, PropertyType.fromString("exportCalendar"));
  }

  /**
   * Tests PropertyType fromString for importCalendar.
   */
  @Test
  public void testPropertyTypeFromStringImportCalendar() {
    assertEquals(PropertyType.IMPORT_CALENDAR, PropertyType.fromString("importCalendar"));
  }

  /**
   * Tests PropertyType fromString for createEvent.
   */
  @Test
  public void testPropertyTypeFromStringCreateEvent() {
    assertEquals(PropertyType.CREATE_EVENT, PropertyType.fromString("createEvent"));
  }

  /**
   * Tests PropertyType fromString for editEvent.
   */
  @Test
  public void testPropertyTypeFromStringEditEvent() {
    assertEquals(PropertyType.EDIT_EVENT, PropertyType.fromString("editEvent"));
  }

  /**
   * Tests PropertyType fromString for eventSelected.
   */
  @Test
  public void testPropertyTypeFromStringEventSelected() {
    assertEquals(PropertyType.EVENT_SELECTED, PropertyType.fromString("eventSelected"));
  }

  /**
   * Tests PropertyType fromString for viewChanged.
   */
  @Test
  public void testPropertyTypeFromStringViewChanged() {
    assertEquals(PropertyType.VIEW_CHANGED, PropertyType.fromString("viewChanged"));
  }

  /**
   * Tests PropertyType fromString for daySelected.
   */
  @Test
  public void testPropertyTypeFromStringDaySelected() {
    assertEquals(PropertyType.DAY_SELECTED, PropertyType.fromString("daySelected"));
  }

  /**
   * Tests PropertyType fromString returns null for null input.
   */
  @Test
  public void testPropertyTypeFromStringNull() {
    assertNull(PropertyType.fromString(null));
  }

  /**
   * Tests PropertyType fromString returns null for unknown property.
   */
  @Test
  public void testPropertyTypeFromStringUnknown() {
    assertNull(PropertyType.fromString("unknownProperty"));
  }

  /**
   * Tests PropertyType fromString returns null for empty string.
   */
  @Test
  public void testPropertyTypeFromStringEmpty() {
    assertNull(PropertyType.fromString(""));
  }

  /**
   * Tests PropertyType fromString with uppercase input.
   */
  @Test
  public void testPropertyTypeFromStringUpperCase() {
    assertEquals(PropertyType.CREATE_CALENDAR, PropertyType.fromString("CREATE_CALENDAR"));
  }

  /**
   * Tests PropertyType fromString with mixed case camelCase.
   */
  @Test
  public void testPropertyTypeFromStringMixedCaseCamel() {
    assertEquals(PropertyType.CALENDAR_SELECTED, PropertyType.fromString("calendarSelected"));
  }

  /**
   * Tests PropertyType values count.
   */
  @Test
  public void testPropertyTypeValuesCount() {
    assertEquals(10, PropertyType.values().length);
  }

  /**
   * Tests PropertyType round trip conversion.
   */
  @Test
  public void testPropertyTypeRoundTrip() {
    for (PropertyType type : PropertyType.values()) {
      String name = type.name();
      PropertyType converted = PropertyType.fromString(name);
      assertEquals(type, converted);
    }
  }

  /**
   * Tests FilteredPropertyChangeListener passes valid property.
   */
  @Test
  public void testFilteredListenerPassesValidProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    PropertyChangeEvent evt = new PropertyChangeEvent(this, "createCalendar", null, "value");
    filtered.propertyChange(evt);

    assertEquals(1, received.size());
    assertEquals("createCalendar", received.get(0).getPropertyName());
  }

  /**
   * Tests FilteredPropertyChangeListener passes custom property.
   */
  @Test
  public void testFilteredListenerPassesCustomProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    PropertyChangeEvent evt = new PropertyChangeEvent(this, "myCustomProperty", "old", "new");
    filtered.propertyChange(evt);

    assertEquals(1, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks Frame properties.
   */
  @Test
  public void testFilteredListenerBlocksFrameProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "Frame.something", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks ancestor properties.
   */
  @Test
  public void testFilteredListenerBlocksAncestorProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "ancestorResized", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks JComponent properties.
   */
  @Test
  public void testFilteredListenerBlocksJsComponentProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "JComponent.something", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks Dialog properties.
   */
  @Test
  public void testFilteredListenerBlocksDialogProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "Dialog.disposed", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks dropTarget property.
   */
  @Test
  public void testFilteredListenerBlocksDropTarget() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "dropTarget", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks graphicsConfiguration.
   */
  @Test
  public void testFilteredListenerBlocksGraphicsConfiguration() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "graphicsConfiguration", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks focusOwner.
   */
  @Test
  public void testFilteredListenerBlocksFocusOwner() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "focusOwner", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks permanentFocusOwner.
   */
  @Test
  public void testFilteredListenerBlocksPermanentFocusOwner() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "permanentFocusOwner", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks activeWindow.
   */
  @Test
  public void testFilteredListenerBlocksActiveWindow() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "activeWindow", null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener blocks null property.
   */
  @Test
  public void testFilteredListenerBlocksNullProperty() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, null, null, "value"));

    assertEquals(0, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener with multiple events.
   */
  @Test
  public void testFilteredListenerMultipleEvents() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    filtered.propertyChange(new PropertyChangeEvent(this, "valid1", null, "v"));
    filtered.propertyChange(new PropertyChangeEvent(this, "Frame.x", null, "v"));
    filtered.propertyChange(new PropertyChangeEvent(this, "valid2", null, "v"));
    filtered.propertyChange(new PropertyChangeEvent(this, "ancestorMoved", null, "v"));
    filtered.propertyChange(new PropertyChangeEvent(this, "valid3", null, "v"));

    assertEquals(3, received.size());
  }

  /**
   * Tests FilteredPropertyChangeListener preserves event values.
   */
  @Test
  public void testFilteredListenerPreservesEventValues() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    PropertyChangeEvent evt = new PropertyChangeEvent(this, "testProperty", "oldValue", "newValue");
    filtered.propertyChange(evt);

    assertEquals(1, received.size());
    assertEquals("oldValue", received.get(0).getOldValue());
    assertEquals("newValue", received.get(0).getNewValue());
  }

  /**
   * Tests FilteredPropertyChangeListener preserves event source.
   */
  @Test
  public void testFilteredListenerPreservesEventSource() {
    List<PropertyChangeEvent> received = new ArrayList<>();
    PropertyChangeListener delegate = received::add;
    FilteredPropertyChangeListener filtered = new FilteredPropertyChangeListener(delegate);

    Object source = new Object();
    PropertyChangeEvent evt = new PropertyChangeEvent(source, "testProperty", null, "value");
    filtered.propertyChange(evt);

    assertEquals(1, received.size());
    assertSame(source, received.get(0).getSource());
  }

  /**
   * Tests EventDataParser parseCalendarData for CREATE_CALENDAR.
   */
  @Test
  public void testParseCalendarDataCreateCalendar() {
    Object[] data = new Object[] {"TestCalendar", ZoneId.of("UTC")};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "createCalendar", null, data);

    Object result = EventDataParser.parseCalendarData(PropertyType.CREATE_CALENDAR, evt);

    assertTrue(result instanceof CalendarEventData.CreateCalendar);
    CalendarEventData.CreateCalendar createData = (CalendarEventData.CreateCalendar) result;
    assertEquals("TestCalendar", createData.getName());
    assertEquals(ZoneId.of("UTC"), createData.getTimezone());
  }

  /**
   * Tests EventDataParser parseCalendarData for EDIT_CALENDAR.
   */
  @Test
  public void testParseCalendarDataEditCalendar() {
    Object[] oldData = new Object[] {"OldName"};
    Object[] newData = new Object[] {"NewName", ZoneId.of("America/Chicago")};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "editCalendar", oldData, newData);

    Object result = EventDataParser.parseCalendarData(PropertyType.EDIT_CALENDAR, evt);

    assertTrue(result instanceof CalendarEventData.EditCalendar);
    CalendarEventData.EditCalendar editData = (CalendarEventData.EditCalendar) result;
    assertEquals("OldName", editData.getOldName());
    assertEquals("NewName", editData.getNewName());
    assertEquals(ZoneId.of("America/Chicago"), editData.getNewTimezone());
  }

  /**
   * Tests EventDataParser parseCalendarData for EXPORT_CALENDAR.
   */
  @Test
  public void testParseCalendarDataExportCalendar() {
    Object[] data = new Object[] {"/export/path.csv", "csv"};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "exportCalendar", null, data);

    Object result = EventDataParser.parseCalendarData(PropertyType.EXPORT_CALENDAR, evt);

    assertTrue(result instanceof CalendarEventData.ExportCalendar);
    CalendarEventData.ExportCalendar exportData = (CalendarEventData.ExportCalendar) result;
    assertEquals("/export/path.csv", exportData.getFilename());
    assertEquals("csv", exportData.getFormat());
  }

  /**
   * Tests EventDataParser parseCalendarData for IMPORT_CALENDAR.
   */
  @Test
  public void testParseCalendarDataImportCalendar() {
    Object[] data = new Object[] {"/import/path.ics", "ical"};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "importCalendar", null, data);

    Object result = EventDataParser.parseCalendarData(PropertyType.IMPORT_CALENDAR, evt);

    assertTrue(result instanceof CalendarEventData.ImportCalendar);
    CalendarEventData.ImportCalendar importData = (CalendarEventData.ImportCalendar) result;
    assertEquals("/import/path.ics", importData.getFilename());
    assertEquals("ical", importData.getFormat());
  }

  /**
   * Tests EventDataParser parseCalendarData returns null for null type.
   */
  @Test
  public void testParseCalendarDataNullType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseCalendarData(null, evt));
  }

  /**
   * Tests EventDataParser parseCalendarData returns null for unhandled type.
   */
  @Test
  public void testParseCalendarDataUnhandledType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseCalendarData(PropertyType.VIEW_CHANGED, evt));
    assertNull(EventDataParser.parseCalendarData(PropertyType.CREATE_EVENT, evt));
    assertNull(EventDataParser.parseCalendarData(PropertyType.CALENDAR_SELECTED, evt));
  }

  /**
   * Tests EventDataParser parseEventData returns null for null type.
   */
  @Test
  public void testParseEventDataNullType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseEventData(null, evt));
  }

  /**
   * Tests EventDataParser parseEventData returns null for unhandled type.
   */
  @Test
  public void testParseEventDataUnhandledType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseEventData(PropertyType.CREATE_CALENDAR, evt));
    assertNull(EventDataParser.parseEventData(PropertyType.CREATE_EVENT, evt));
    assertNull(EventDataParser.parseEventData(PropertyType.VIEW_CHANGED, evt));
  }

  /**
   * Tests EventDataParser parseViewData for VIEW_CHANGED.
   */
  @Test
  public void testParseViewDataViewChanged() {
    Object[] data = new Object[] {SwingCalendarView.ViewMode.WEEK, LocalDate.of(2024, 7, 15)};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "viewChanged", null, data);

    Object result = EventDataParser.parseViewData(PropertyType.VIEW_CHANGED, evt);

    assertTrue(result instanceof ViewEventData.ViewChanged);
    ViewEventData.ViewChanged viewData = (ViewEventData.ViewChanged) result;
    assertEquals(SwingCalendarView.ViewMode.WEEK, viewData.getViewMode());
    assertEquals(LocalDate.of(2024, 7, 15), viewData.getDate());
  }

  /**
   * Tests EventDataParser parseViewData for VIEW_CHANGED with DAY mode.
   */
  @Test
  public void testParseViewDataViewChangedDayMode() {
    Object[] data = new Object[] {SwingCalendarView.ViewMode.DAY, LocalDate.of(2024, 3, 10)};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "viewChanged", null, data);

    Object result = EventDataParser.parseViewData(PropertyType.VIEW_CHANGED, evt);

    ViewEventData.ViewChanged viewData = (ViewEventData.ViewChanged) result;
    assertEquals(SwingCalendarView.ViewMode.DAY, viewData.getViewMode());
  }

  /**
   * Tests EventDataParser parseViewData for VIEW_CHANGED with MONTH mode.
   */
  @Test
  public void testParseViewDataViewChangedMonthMode() {
    Object[] data = new Object[] {SwingCalendarView.ViewMode.MONTH, LocalDate.of(2024, 12, 1)};
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "viewChanged", null, data);

    Object result = EventDataParser.parseViewData(PropertyType.VIEW_CHANGED, evt);

    ViewEventData.ViewChanged viewData = (ViewEventData.ViewChanged) result;
    assertEquals(SwingCalendarView.ViewMode.MONTH, viewData.getViewMode());
  }

  /**
   * Tests EventDataParser parseViewData returns null for null type.
   */
  @Test
  public void testParseViewDataNullType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseViewData(null, evt));
  }

  /**
   * Tests EventDataParser parseViewData returns null for unhandled type.
   */
  @Test
  public void testParseViewDataUnhandledType() {
    PropertyChangeEvent evt = new PropertyChangeEvent(this, "test", null, null);
    assertNull(EventDataParser.parseViewData(PropertyType.CREATE_EVENT, evt));
    assertNull(EventDataParser.parseViewData(PropertyType.DAY_SELECTED, evt));
    assertNull(EventDataParser.parseViewData(PropertyType.CREATE_CALENDAR, evt));
  }

  /**
   * Tests CalendarControllerHandler canHandle for CALENDAR_SELECTED.
   */
  @Test
  public void testCalendarHandlerCanHandleCalendarSelected() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.CALENDAR_SELECTED));
  }

  /**
   * Tests CalendarControllerHandler canHandle for CREATE_CALENDAR.
   */
  @Test
  public void testCalendarHandlerCanHandleCreateCalendar() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.CREATE_CALENDAR));
  }

  /**
   * Tests CalendarControllerHandler canHandle for EDIT_CALENDAR.
   */
  @Test
  public void testCalendarHandlerCanHandleEditCalendar() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.EDIT_CALENDAR));
  }

  /**
   * Tests CalendarControllerHandler canHandle for EXPORT_CALENDAR.
   */
  @Test
  public void testCalendarHandlerCanHandleExportCalendar() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.EXPORT_CALENDAR));
  }

  /**
   * Tests CalendarControllerHandler canHandle for IMPORT_CALENDAR.
   */
  @Test
  public void testCalendarHandlerCanHandleImportCalendar() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.IMPORT_CALENDAR));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for CREATE_EVENT.
   */
  @Test
  public void testCalendarHandlerCannotHandleCreateEvent() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.CREATE_EVENT));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for EDIT_EVENT.
   */
  @Test
  public void testCalendarHandlerCannotHandleEditEvent() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.EDIT_EVENT));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for EVENT_SELECTED.
   */
  @Test
  public void testCalendarHandlerCannotHandleEventSelected() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.EVENT_SELECTED));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for VIEW_CHANGED.
   */
  @Test
  public void testCalendarHandlerCannotHandleViewChanged() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.VIEW_CHANGED));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for DAY_SELECTED.
   */
  @Test
  public void testCalendarHandlerCannotHandleDaySelected() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.DAY_SELECTED));
  }

  /**
   * Tests CalendarControllerHandler canHandle returns false for null.
   */
  @Test
  public void testCalendarHandlerCannotHandleNull() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertFalse(handler.canHandle(null));
  }

  /**
   * Tests EventControllerHandler canHandle for CREATE_EVENT.
   */
  @Test
  public void testEventHandlerCanHandleCreateEvent() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.CREATE_EVENT));
  }

  /**
   * Tests EventControllerHandler canHandle for EDIT_EVENT.
   */
  @Test
  public void testEventHandlerCanHandleEditEvent() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.EDIT_EVENT));
  }

  /**
   * Tests EventControllerHandler canHandle for EVENT_SELECTED.
   */
  @Test
  public void testEventHandlerCanHandleEventSelected() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.EVENT_SELECTED));
  }

  /**
   * Tests EventControllerHandler canHandle returns false for CALENDAR_SELECTED.
   */
  @Test
  public void testEventHandlerCannotHandleCalendarSelected() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.CALENDAR_SELECTED));
  }

  /**
   * Tests EventControllerHandler canHandle returns false for CREATE_CALENDAR.
   */
  @Test
  public void testEventHandlerCannotHandleCreateCalendar() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.CREATE_CALENDAR));
  }

  /**
   * Tests EventControllerHandler canHandle returns false for VIEW_CHANGED.
   */
  @Test
  public void testEventHandlerCannotHandleViewChanged() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.VIEW_CHANGED));
  }

  /**
   * Tests EventControllerHandler canHandle returns false for null.
   */
  @Test
  public void testEventHandlerCannotHandleNull() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertFalse(handler.canHandle(null));
  }

  /**
   * Tests ViewControllerHandler canHandle for VIEW_CHANGED.
   */
  @Test
  public void testViewHandlerCanHandleViewChanged() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.VIEW_CHANGED));
  }

  /**
   * Tests ViewControllerHandler canHandle for DAY_SELECTED.
   */
  @Test
  public void testViewHandlerCanHandleDaySelected() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertTrue(handler.canHandle(PropertyType.DAY_SELECTED));
  }

  /**
   * Tests ViewControllerHandler canHandle returns false for CALENDAR_SELECTED.
   */
  @Test
  public void testViewHandlerCannotHandleCalendarSelected() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.CALENDAR_SELECTED));
  }

  /**
   * Tests ViewControllerHandler canHandle returns false for CREATE_EVENT.
   */
  @Test
  public void testViewHandlerCannotHandleCreateEvent() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertFalse(handler.canHandle(PropertyType.CREATE_EVENT));
  }

  /**
   * Tests ViewControllerHandler canHandle returns false for null.
   */
  @Test
  public void testViewHandlerCannotHandleNull() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertFalse(handler.canHandle(null));
  }

  /**
   * Tests CalendarControllerHandler implements PropertyChangeHandler.
   */
  @Test
  public void testCalendarHandlerImplementsInterface() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    assertTrue(handler instanceof PropertyChangeHandler);
  }

  /**
   * Tests EventControllerHandler implements PropertyChangeHandler.
   */
  @Test
  public void testEventHandlerImplementsInterface() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    assertTrue(handler instanceof PropertyChangeHandler);
  }

  /**
   * Tests ViewControllerHandler implements PropertyChangeHandler.
   */
  @Test
  public void testViewHandlerImplementsInterface() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    assertTrue(handler instanceof PropertyChangeHandler);
  }

  /**
   * Tests all PropertyTypes are handled by exactly one handler.
   */
  @Test
  public void testAllPropertyTypesHandledByOneHandler() {
    CalendarControllerHandler calHandler = new CalendarControllerHandler(null, null);
    EventControllerHandler eventHandler = new EventControllerHandler(null, null);
    ViewControllerHandler viewHandler = new ViewControllerHandler(null, null);

    for (PropertyType type : PropertyType.values()) {
      int count = 0;
      if (calHandler.canHandle(type)) {
        count++;
      }
      if (eventHandler.canHandle(type)) {
        count++;
      }
      if (viewHandler.canHandle(type)) {
        count++;
      }
      assertEquals(1, count);
    }
  }

  /**
   * Tests CalendarControllerHandler handles 5 property types.
   */
  @Test
  public void testCalendarHandlerHandles5Types() {
    CalendarControllerHandler handler = new CalendarControllerHandler(null, null);
    int count = 0;
    for (PropertyType type : PropertyType.values()) {
      if (handler.canHandle(type)) {
        count++;
      }
    }
    assertEquals(5, count);
  }

  /**
   * Tests EventControllerHandler handles 3 property types.
   */
  @Test
  public void testEventHandlerHandles3Types() {
    EventControllerHandler handler = new EventControllerHandler(null, null);
    int count = 0;
    for (PropertyType type : PropertyType.values()) {
      if (handler.canHandle(type)) {
        count++;
      }
    }
    assertEquals(3, count);
  }

  /**
   * Tests ViewControllerHandler handles 2 property types.
   */
  @Test
  public void testViewHandlerHandles2Types() {
    ViewControllerHandler handler = new ViewControllerHandler(null, null);
    int count = 0;
    for (PropertyType type : PropertyType.values()) {
      if (handler.canHandle(type)) {
        count++;
      }
    }
    assertEquals(2, count);
  }
}
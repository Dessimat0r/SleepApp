package sleepapp;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MutableDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.pf.settings.Settings;
import org.pf.settings.rw.IniReaderWriter;

/**
 * 
 */

/**
 * SleepApp - SleepApp
 * 
 * @version 1.0
 * @author DXM40HOURS
 */
public final class SleepApp {
	public LocalTime[] wakeTimes = null;
	public Period[] sleepPeriods = null;
	public LocalTime[] idealSleepStartTimes = null;
	
	final int[] currAmendedWakeTime = {0, 0};
	final int[] timeAsleep = {0, 0};
	
	private Display display;
	private boolean disposed = false, finished = false;
	private boolean pending = false;
	
	public Settings settings;
	
	//private Popup popup = null;
	
	private MutableDateTime currTime = new MutableDateTime(), tomorrowTime = new MutableDateTime();
	private DateTime sleepWarningStartTime;
	
	public int sleepDay, wakeDay;
	
	public LocalDate sleepDayLD, wakeDayLD;
	
	public LocalDate today, tomorrow;
	
	public DateTime currSleepDT, wakeTime;
	
	public Period sleepPeriod;
	
	public Shell controlShell, currShell;
	
    //-----------------------------------------------------------------------
    /**
     * Gets the default PeriodFormatter.
     * <p>
     * This currently returns a word based formatter using English only.
     * Hopefully future release will support localized period formatting.
     * 
     * @return the formatter
     */
    public static PeriodFormatter createDefaultFormatter() {
		final String[] variants = {" ", ",", ",and ", ", and "};
        final PeriodFormatter cEnglishWords = new PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix(" year", " years")
            .appendSeparator(", ", " and ", variants)
            .appendMonths()
            .appendSuffix(" month", " months")
            .appendSeparator(", ", " and ", variants)
            .appendWeeks()
            .appendSuffix(" week", " weeks")
            .appendSeparator(", ", " and ", variants)
            .appendDays()
            .appendSuffix(" day", " days")
            .appendSeparator(", ", " and ", variants)
            .appendHours()
            .appendSuffix(" hour", " hours")
            .appendSeparator(", ", " and ", variants)
            .appendMinutes()
            .appendSuffix(" minute", " minutes")
            .toFormatter();
        return cEnglishWords;
    }
	
	public static final PeriodFormatter PERIOD_FORMATTER = createDefaultFormatter();

	public long currUpdateTimeMS = 0, lastUpdateTimeMS = 0, lastCheckTimeMS = 0, lastShowTimeMS = 0;
	
	public static final long TIME_UPDATE_PERIOD_MS = 1000;
	public static final long TIME_WARN_PERIOD_MS = 300000; // 5 mins
	//public static final long TIME_WARN_PERIOD_MS = 30; // 5 mins
	public static final Period PRE_SLEEP_WARN_PERIOD = Period.hours(4);
	
	public final DateTimeFormatter timeFormatter;
	
	public Tray tray;
	
	public TrayItem trayItem;
	
	public IniReaderWriter settingsReader;
	
	protected ToolTip tt = null;
	
	protected static final Random RANDOM = new Random();
	
	enum Phrase {
		DF_PHRASE(
			0,
			"Stop bloody playing Dwarf Fortress and go to bed. " +
			"You need to get into University earlier."
		),
		KNACKERED_PHRASE(
			0,
			"You're going to be knackered in the morning, it's not worth it -- turn the PC off."
		),
		DUMP_PHRASE(
			0,
			"You still need to have a dump, right? Get to the shitter and then go to sleep."
		)
		;
		
		/**
		 * 
		 */
		private Phrase(int hoursleft, String phrase) {
			this.hoursLeft = hoursleft;
			this.phrase = phrase;
		}
		
		protected int hoursLeft;
		protected String phrase;
	}
	
	public static void main(String args[]) {
		SleepApp app = new SleepApp();
		app.init();
		app.start();
	}
	
	/**
	 * @return the disposed
	 */
	public boolean isDisposed() {
		return disposed;
	}
	
	/**
	 * @return the display
	 */
	public Display getDisplay() {
		return display;
	}

	public SleepApp() {
		settingsReader = new IniReaderWriter("settings.ini");
		settings = settingsReader.loadSettings();
		
		wakeTimes = new LocalTime[7];
		sleepPeriods = new Period[7];
		idealSleepStartTimes = new LocalTime[7];
		
		wakeTimes[0] = processTimeString(settings.getValueOf("wake_time_mon"));
		wakeTimes[1] = processTimeString(settings.getValueOf("wake_time_tues"));
		wakeTimes[2] = processTimeString(settings.getValueOf("wake_time_wed"));
		wakeTimes[3] = processTimeString(settings.getValueOf("wake_time_thurs"));
		wakeTimes[4] = processTimeString(settings.getValueOf("wake_time_fri"));
		wakeTimes[5] = processTimeString(settings.getValueOf("wake_time_sat"));
		wakeTimes[6] = processTimeString(settings.getValueOf("wake_time_sun"));
		
		sleepPeriods[0] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_mon"));
		sleepPeriods[1] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_tues"));
		sleepPeriods[2] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_wed"));
		sleepPeriods[3] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_thurs"));
		sleepPeriods[4] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_fri"));
		sleepPeriods[5] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_sat"));
		sleepPeriods[6] = processTimeStringPeriod(settings.getValueOf("ideal_sleep_period_sun"));
		
		for (int i = 0; i < 7; i++) {
			idealSleepStartTimes[i] = wakeTimes[i].minus(sleepPeriods[i]);
		}
		
		display = new Display();
		tray = display.getSystemTray();
		
		DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
		builder.appendClockhourOfHalfday(1);
		builder.appendLiteral(':');
		builder.appendMinuteOfHour(2);
		builder.appendLiteral(' ');
		builder.appendHalfdayOfDayText();
		timeFormatter = builder.toFormatter();
	}
	
	public LocalTime processTimeString(String timeStr) {
		int colonplace = timeStr.indexOf(':');
		int hour = Integer.parseInt(timeStr.substring(0, colonplace));
		int minute = Integer.parseInt(timeStr.substring(colonplace + 1));
		return new LocalTime(hour, minute);
	}
	
	public Period processTimeStringPeriod(String timeStr) {
		int colonplace = timeStr.indexOf(':');
		int hour = Integer.parseInt(timeStr.substring(0, colonplace));
		int minute = Integer.parseInt(timeStr.substring(colonplace + 1));
		return new Period(hour, minute, 0, 0);
	}
	
	public void dispose() {
		if (currShell != null && !currShell.isDisposed()) {
			currShell.close();
		}
		
		display.dispose();
	}
	
	public void openWindow() {		
		currShell = new Shell(display, SWT.ON_TOP | SWT.DIALOG_TRIM);
		currShell.setText("Sleep Warning!");
		GridLayout layout = new GridLayout(2, false);
		currShell.setLayout(layout);
		
		Label l = new Label(
			currShell, SWT.WRAP
		);
		l.setText(
			"You will now get " +
			PERIOD_FORMATTER.print(sleepPeriod) + " of sleep " +
			"if you choose to sleep now. This is based on a wake-up time of " +
			timeFormatter.print(wakeTimes[wakeDay - 1]) +
			" on " + wakeDayLD.dayOfWeek().getAsText() + ". It is currently " +
			timeFormatter.print(currTime) +
			". Make your decision. " + (needSleep() ? " " + getRandomPhrase() : "")
		);
		GridData d = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		d.widthHint = 250;
		l.setLayoutData(d);
		
		Button sleepButton = new Button(currShell, SWT.NONE);
		sleepButton.setText("Go to sleep");
		sleepButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
		
		Button stayAwakeButton = new Button(currShell, SWT.NONE);
		stayAwakeButton.setText("Stay awake");
		stayAwakeButton.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
		
		sleepButton.setFocus();
		
		currShell.setMinimumSize(250, 130);
		currShell.pack();
		currShell.open();
		currShell.setVisible(true);
		
		sleepButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				currShell.close();
				currShell = null;
			}
		});
		
		stayAwakeButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				currShell.close();
				currShell = null;
			}
		});
	}

	public void init() {
		controlShell = new Shell(display);
		controlShell.setVisible(false);		
		
		if (tray != null) {
		    Image image = new Image(display, 20, 20);
		    GC gc = new GC(image);
		    
		    gc.setBackground(new Color(display, 255, 100, 5));
		    gc.fillRectangle(0, 0, 19, 19);
		    
		    gc.setBackground(new Color(display, 0, 0, 0));
		    gc.setForeground(new Color(display, 0, 0, 0));
		    gc.fillRectangle(5, 5, 10, 10);
		    
		    gc.drawRectangle(0, 0, 19, 19);
		    
		    gc.dispose();
		    ImageData imageData = image.getImageData();
			
			trayItem = new TrayItem(tray, SWT.BALLOON | SWT.ICON_INFORMATION);
			tt = new ToolTip(controlShell, SWT.NONE);
			trayItem.setToolTip(tt);
			tt.setAutoHide(true);
			trayItem.setImage(image);
			
			trayItem.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					openWindow();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					
				}
			});
			
			tt.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					openWindow();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
				}
			});
			final Menu menu = new Menu(controlShell, SWT.POP_UP);
			
			MenuItem updateMenuItem = new MenuItem(menu, SWT.PUSH);
			updateMenuItem.setText("&Update");
			updateMenuItem.setAccelerator(SWT.CTRL + 'U');
			
			menu.setDefaultItem(updateMenuItem);
			
			MenuItem closeMenuItem = new MenuItem(menu, SWT.PUSH);
			
			closeMenuItem.setText("&Close");
			closeMenuItem.setAccelerator(0);
			
			trayItem.addListener (SWT.MenuDetect, new Listener() {
				public void handleEvent (Event event) {
					menu.setVisible(true);
				}
			});
			
			updateMenuItem.addSelectionListener(new SelectionAdapter() {
				/* (non-Javadoc)
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateTimes();
				}
			});
			
			closeMenuItem.addSelectionListener(new SelectionAdapter() {
				/* (non-Javadoc)
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					finished = true;
				}
			});
		}
		updateTimes();
	}

	public void start() {
		while (!finished) {
			if (!display.readAndDispatch()) {
				update();
				display.sleep();
			}
		}
		dispose();
	}
	
	public void updateTimes() {		
		currTime.setMillis(DateTimeUtils.currentTimeMillis());
		tomorrowTime.setMillis(currTime.getMillis());
		tomorrowTime.addDays(1);
		
		today = new LocalDate(currTime);
		tomorrow = today.plusDays(1);
		
		int hour = currTime.getHourOfDay();
		if (hour < 7) {
			sleepDayLD = today;
			wakeDayLD = today;
		} else {
			sleepDayLD = today;
			wakeDayLD = tomorrow;
		}
		
		//currSleepDT = IDEAL_SLEEP_START_TIMES[sleepDay - 1].toDateTimeToday();
		
		sleepDay = sleepDayLD.getDayOfWeek();
		wakeDay = wakeDayLD.getDayOfWeek();
		
		wakeTime = today.toDateTime(wakeTimes[wakeDay - 1]);
		
		// calculate sleep hours
		sleepPeriod = new Period(currTime, wakeTime);
	
		if (trayItem != null) trayItem.setToolTipText(
			"You will get " +
			PERIOD_FORMATTER.print(sleepPeriod) +
			" sleep, based on a wake-up time of " +
			timeFormatter.print(wakeTimes[wakeDay - 1]) + 
			"."
		);
	}
	
	public boolean needSleep() {		
		return (sleepPeriod.toStandardSeconds().isLessThan(sleepPeriods[sleepDay - 1].toStandardSeconds()));
	}
	
	public void update() {
		currUpdateTimeMS = System.currentTimeMillis();
		
		if ((lastCheckTimeMS + TIME_UPDATE_PERIOD_MS) <= currUpdateTimeMS) {
			updateTimes();
			lastCheckTimeMS = currUpdateTimeMS;
		}
		
		if (((lastShowTimeMS + TIME_WARN_PERIOD_MS) <= currUpdateTimeMS)) {
			if (needSleep()) {
				//if (currSleepDT.isAfterNow()) {
				pending = true;
			//}
			}
			
			lastShowTimeMS = currUpdateTimeMS;
		}
		
		if (pending) {
			showTooltip();
			pending = false;			
		}
		
		if (currShell != null && !currShell.isDisposed()) {
			currShell.update();
		}
		
		lastUpdateTimeMS = currUpdateTimeMS;
	}
	
	protected void showTooltip() {
		tt.setMessage("You will get " + PERIOD_FORMATTER.print(sleepPeriod) + " sleep" +
		", based on a wake-up time of " + timeFormatter.print(wakeTimes[wakeDay - 1]) +
		"." + (needSleep() ? " " + getRandomPhrase() : "")
		);
		
		tt.setVisible(true);
	}
	
	public String getRandomPhrase() {
		return Phrase.values()[RANDOM.nextInt(Phrase.values().length)].phrase;
	}
	
	/**
	 * @return the sleepDay
	 */
	public int getSleepDay() {
		return sleepDay;
	}
	
	/**
	 * @return the sleepDayLD
	 */
	public LocalDate getSleepDayLD() {
		return sleepDayLD;
	}
	
	/**
	 * @return the sleepPeriod
	 */
	public Period getSleepPeriod() {
		return sleepPeriod;
	}
	
	/**
	 * @return the wakeDay
	 */
	public int getWakeDay() {
		return wakeDay;
	}
	
	/**
	 * @return the wakeDayLD
	 */
	public LocalDate getWakeDayLD() {
		return wakeDayLD;
	}
	
	/**
	 * @return the wakeTime
	 */
	public DateTime getWakeTime() {
		return wakeTime;
	}
}

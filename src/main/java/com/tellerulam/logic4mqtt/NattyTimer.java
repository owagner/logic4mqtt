package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import com.joestelmach.natty.*;
import com.tellerulam.logic4mqtt.api.*;

public class NattyTimer extends LogicTimer
{
	/*
	 * current scheduling run
	 */
	private List<Date> dates;
	private int dateIndex;
	private boolean recurrence;
	private Date recursUntil;
	private boolean needToCheckSunpatterns=true;

	protected NattyTimer(String symbolicName, String timespec, TimerCallbackInterface callback, Object userdata)
	{
		super(symbolicName,timespec,callback,userdata);
		parseTime();
	}

	@Override
	public void start()
	{
		schedule(false);
	}
	//private static final Pattern sunpatterns=Pattern.compile("(official |nautical |nautic |astro |astronomical |civil )?(sunset|sunrise)(\\s?(\\+|\\-)\\s?([0-9]+)\\s?(s|m|h)\\w*)?");
	private static final Pattern sunpatterns=Pattern.compile("(official |nautical |nautic |astro |astronomical |civil )?(sunset|sunrise)");
	//private static final DateFormat hhmmss=new SimpleDateFormat("HH:mm:ss");

	/* Overridable for unit testing only */
	protected Time getTimeInstance()
	{
		return InstanceManager.getTimeInstance();
	}

	public static class ParsedSpec
	{
		public final List<Date> dates;
		public final boolean recurrence;
		public final Date recursUntil;
		public ParsedSpec(List<Date> dates,boolean recurrence,Date recursUntil)
		{
			this.dates=dates;
			this.recurrence=recurrence;
			this.recursUntil=recursUntil;
		}
	}

	public static ParsedSpec parseTimeSpec(String timespec)
	{
		NattyTimer t=new NattyTimer("",timespec,null,null);
		return new ParsedSpec(t.dates,t.recurrence,t.recursUntil);
	}

	private void parseTime()
	{
		/*
		 * Before we feed data to Natty, replace sunset/sundown accordingly
		 */
		String usedtimespec;
		if(needToCheckSunpatterns)
		{
			StringBuffer newtimespec=new StringBuffer();

			// Will be set to true again if a match occured
			needToCheckSunpatterns=false;
			Matcher m=sunpatterns.matcher(timespec);
			while(m.find())
			{
				String replacement=null;

				boolean rise=m.group(2).equals("sunrise");
				char type='d';
				if(m.group(1)!=null)
					type=m.group(1).charAt(0);
				Time ss=getTimeInstance();
				switch(type)
				{
					case 'd':
						replacement=(rise)?ss.getSunrise():ss.getSunset();
						break;
					case 'c':
						replacement=(rise)?ss.getCivilSunrise():ss.getCivilSunset();
						break;
					case 'o':
						replacement=(rise)?ss.getOfficialSunrise():ss.getOfficialSunset();
						break;
					case 'a':
						replacement=(rise)?ss.getAstronomicalSunrise():ss.getAstronomicalSunset();
						break;
					case 'n':
						replacement=(rise)?ss.getNauticalSunrise():ss.getNauticalSunset();
						break;
				}
				m.appendReplacement(newtimespec,replacement);
				needToCheckSunpatterns=true;
			}
			m.appendTail(newtimespec);
			usedtimespec=newtimespec.toString();
		}
		else
			usedtimespec=timespec;

		Parser p=new Parser();
		List<DateGroup> groups=p.parse(usedtimespec);
		if(groups.size()!=1)
			throw new IllegalArgumentException("Ambigious date specification: "+usedtimespec);
		DateGroup dg=groups.get(0);
		dates=dg.getDates();
		recurrence=dg.isRecurring();
		recursUntil=dg.getRecursUntil();
		dateIndex=0;
	}

	private boolean schedule(boolean rescheduling)
	{
		if(canceled)
			return false;

		Date now=new Date();
		// Schedule the next timer iteration
		if(dateIndex>=dates.size())
		{
			// No more dates after this parsing run
			if(recurrence)
			{
				if(recursUntil!=null && now.after(recursUntil))
					return false;
				// Reparse time
				parseTime();
				return schedule(true);
			}
			removeOneTimer(this);
			return false;
		}

		/*
		 * With a sunset/sunrise relative pattern, the re-scheduling
		 * may have ended up a few minutes in the future, which causes or
		 * simple retrigger-on-next-day check to fail. So we make sure or
		 * next run is at least 12 hours in the future.
		 */
		Date mustBeLaterThan=now;
		if(needToCheckSunpatterns && rescheduling)
		{
			Calendar cal=Calendar.getInstance();
			cal.add(Calendar.HOUR_OF_DAY,12);
			mustBeLaterThan=cal.getTime();
		}

		Date nextrun=dates.get(dateIndex++);
		if(mustBeLaterThan.after(nextrun))
		{
			// Reschedule on next day
			Calendar cal=Calendar.getInstance();
			cal.setTime(nextrun);
			cal.add(Calendar.DAY_OF_YEAR, 1);
			nextrun=cal.getTime();
		}

		t.schedule(currentTimerTask=new TimerTask(){
			@Override
			public void run()
			{
				if(canceled)
					return;
				runCallback();
				schedule(true);
			}
		},nextrun);
		L.info("Scheduled "+this+" for "+nextrun);
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder msg=new StringBuilder();
		msg.append("[NT:");
		msg.append(symbolicName);
		msg.append(":");
		msg.append(dates);
		if(recurrence)
		{
			msg.append("/R");
			if(recursUntil!=null)
			{
				msg.append("-");
				msg.append(recursUntil);
			}
		}
		msg.append("]");
		return msg.toString();
	}

	protected Date getFirstDateForTest()
	{
		return dates.get(0);
	}

	private static final Logger L=Logger.getLogger(NattyTimer.class.getName());

}

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
		schedule();
	}

	private static final Pattern sunpatterns=Pattern.compile("(official |nautical |nautic |astro |astronomical |official )?(sunset|sunrise)");

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
				char type='o';
				if(m.group(1)!=null)
					type=m.group(1).charAt(0);
				SunriseSunset ss=SunriseSunset.getInstance();
				switch(type)
				{
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

	private boolean schedule()
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
				return schedule();
			}
			removeOneTimer(this);
			return false;
		}

		Date nextrun=dates.get(dateIndex++);
		if(now.after(nextrun))
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
				schedule();
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

	private static final Logger L=Logger.getLogger(NattyTimer.class.getName());
}

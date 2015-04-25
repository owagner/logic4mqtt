/*
 * This object contains utility functions to deal with Sunset/Sunrise calculations
 *
 */

package com.tellerulam.logic4mqtt.api;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import com.tellerulam.logic4mqtt.*;

public class Time
{
	static final Time instance=new Time();

	protected Time()
	{
		/* Keep private */
	}

	/**
	 * Set the latitude and longitude used for all subsequent calculations.
	 *
	 * @param latitude
	 * @param longitude
	 */
	public synchronized void setLocation(double latitude,double longitude)
	{
		SunCalc.setLatLng(latitude, longitude);
		L.config("Set location for sunset calculations to "+latitude+" "+longitude);
	}


	/**
	 * Get the Sunrise time (format hh:mm) for the given phase
	 *
	 * @param phase DAYLIGHT (or null), OFFICIAL / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunrise time
	 */
	public String getSunrise(String phase)
	{
		SunCalc.DayInfo di=SunCalc.getDayInfo();
		int m;

		if(phase==null || "DAYLIGHT".equals(phase))
			m=di.sunrise[4];
		else if("OFFICIAL".equals(phase))
			m=di.sunrise[3];
		else if("CIVIL".equals(phase))
			m=di.sunrise[2];
		else if("NAUTICAL".equals(phase))
			m=di.sunrise[1];
		else if("ASTRONOMICAL".equals(phase))
			m=di.sunrise[0];
		else
			throw new IllegalArgumentException("Unknown phase "+phase);

		return SunCalc.makeTimeString(m);
	}
	/**
	 * Get the Sunset time (format hh:mm) for the given phase
	 *
	 * @param phase DAYLIGHT (or null), OFFICIAL / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunset time
	 */
	public String getSunset(String phase)
	{
		SunCalc.DayInfo di=SunCalc.getDayInfo();
		int m;

		if(phase==null || "DAYLIGHT".equals(phase))
			m=di.sunset[0];
		else if("OFFICIAL".equals(phase))
			m=di.sunset[1];
		else if("CIVIL".equals(phase))
			m=di.sunset[2];
		else if("NAUTICAL".equals(phase))
			m=di.sunset[3];
		else if("ASTRONOMICAL".equals(phase))
			m=di.sunset[4];
		else
			throw new IllegalArgumentException("Unknown phase "+phase);

		return SunCalc.makeTimeString(m);
	}

	/**
	 * Shortcut for getSunrise() with the given phase
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getAstronomicalSunrise()
	{
		return getSunrise("ASTRONOMICAL");
	}
	/**
	 * Shortcut for getSunset() with the given phase
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getAstronomicalSunset()
	{
		return getSunset("ASTRONOMICAL");
	}

	/**
	 * Shortcut for getSunrise() with the given phase
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getNauticalSunrise()
	{
		return getSunrise("NAUTICAL");
	}
	/**
	 * Shortcut for getSunset() with the given phase
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getNauticalSunset()
	{
		return getSunset("NAUTICAL");
	}

	/**
	 * Shortcut for getSunrise() with the given phase
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getCivilSunrise()
	{
		return getSunrise("CIVIL");
	}
	/**
	 * Shortcut for getSunset() with the given phase
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getCivilSunset()
	{
		return getSunset("CIVIL");
	}

	/**
	 * Shortcut for getSunrise() with the given phase
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getOfficialSunrise()
	{
		return getSunrise(null);
	}
	/**
	 * Shortcut for getSunset() with the given phase
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getOfficialSunset()
	{
		return getSunset(null);
	}

	/**
	 * Shortcut for getSunrise() for beginning of sunset
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getSunrise()
	{
		return getSunrise(null);
	}
	/**
	 * Shortcut for getSunset() for beginning of sunset
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getSunset()
	{
		return getSunset(null);
	}


	/**
	 * Determine whether we're currently having daylight according to the given horizon
	 *
	 * @param horizon
	 * @return whether it is currently daylight
	 */

	public boolean isDaylight(String horizon)
	{
		return isBetween(getSunrise(horizon),getSunset(horizon));
	}

	/**
	 * Shortcut to isDaylight() for full daylight
	 * @return whether it is currently daylight
	 */
	public boolean isDaylight()
	{
		return isDaylight(null);
	}

	/**
	 * Shortcut to isDaylight() with the given phase
	 * @return whether it is currently daylight
	 */
	public boolean isCivilDaylight()
	{
		return isDaylight("CIVIL");
	}

	/**
	 * Shortcut to isDaylight() with the given phase
	 * @return whether it is currently daylight
	 */
	public boolean isNauticalDaylight()
	{
		return isDaylight("NAUTICAL");
	}

	/**
	 * Shortcut to isDaylight() with the given phase
	 * @return whether it is currently daylight
	 */
	public boolean isAstronomicalDaylight()
	{
		return isDaylight("ASTRONOMICAL");
	}

	/**
	 * Shortcut to isDaylight() with the given phase
	 * @return whether it is currently daylight
	 */
	public boolean isOfficialDaylight()
	{
		return isDaylight(null);
	}

	/**
	 * Calculate the Sun's Azimuth angle at the defined location
	 * and the current moment
	 *
	 * @return The azimuth angle (degrees)
	 */
	public double getSunAzimuth()
	{
		return SunCalc.getPositionInfo().azimuth;
	}

	/**
	 * Calculate the Sun's Altitude at the defined location
	 * and the current moment.
	 *
	 * @return The altitude angle (degress). May be negative if the sun has set.
	 */
	public double getSunAltitude()
	{
		return SunCalc.getPositionInfo().altitude;
	}


	private final Pattern timeSpecPattern=Pattern.compile("([0-9]{1,2}):([0-9]{1,2})(?:\\:([0-9]{1,2}))?");
	private Calendar parseTimeSpec(String timespec)
	{
		Calendar cal=Calendar.getInstance();
		Matcher m=timeSpecPattern.matcher(timespec);
		if(!m.matches())
			throw new IllegalArgumentException("Invalid time specification "+timespec);
		cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(m.group(1)));
		cal.set(Calendar.MINUTE,Integer.parseInt(m.group(2)));
		if(m.group(3)!=null)
			cal.set(Calendar.SECOND,Integer.parseInt(m.group(3)));
		else
			cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		return cal;
	}

	protected static Calendar fixedNow; // Test only!
	private Calendar getNow()
	{
		if(fixedNow!=null)
			return (Calendar)fixedNow.clone();
		return Calendar.getInstance();
	}

	/**
	 * Check whether current time is before the specified time.
	 *
	 * @param timespec A time in "hh:mm" or "hh:mm:ss" format
	 * @return whether current time is before the specified time
	 */
	public boolean isBefore(String timespec)
	{
		Calendar cal=parseTimeSpec(timespec);
		return getNow().before(cal);
	}

	/**
	 * Check whether current time is past the specified time.
	 *
	 * @param timespec A time in "hh:mm" or "hh:mm:ss" format
	 * @return whether current time is past the specified time
	 */
	public boolean isAfter(String timespec)
	{
		Calendar cal=parseTimeSpec(timespec);
		return getNow().after(cal);
	}

	/**
	 * Check whether current time is between the specified times.
	 *
	 * @param timespec1 A start time in "hh:mm" or "hh:mm:ss" format
	 * @param timespec2 A end time in "hh:mm" or "hh:mm:ss" format
	 * @return whether current time is between the specified times
	 */
	public boolean isBetween(String timespec1,String timespec2)
	{
		Calendar cal_start=parseTimeSpec(timespec1);
		Calendar cal_end=parseTimeSpec(timespec2);
		if(cal_start.after(cal_end))
			throw new IllegalArgumentException("End time "+timespec2+" before start time "+timespec1);
		Calendar now=getNow();
		return cal_start.before(now) && cal_end.after(now);
	}

	private static Logger L=Logger.getLogger(Time.class.getName());
}

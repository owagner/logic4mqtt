/*
 * This object contains utility functions to deal with Sunset/Sunrise calculations
 *
 * It is effectivly a very thin wrapper around Mike Reedell's https://github.com/mikereedell/sunrisesunsetlib-java/
 * library, which in turn implements http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 *
 */

package com.tellerulam.logic4mqtt.api;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import net.sourceforge.novaforjava.JulianDay;
import net.sourceforge.novaforjava.api.LnDate;
import net.sourceforge.novaforjava.api.LnLnlatPosn;
import net.sourceforge.novaforjava.api.LnRstTime;
import net.sourceforge.novaforjava.solarsystem.Solar;

public class Time
{
	private static final Time instance=new Time();

	private LnLnlatPosn location=new LnLnlatPosn();
	{
		location.lat=51.358813;
		location.lng=7.241483;
	}

	/**
	 * Set the latitude and longitude used for all subsequent calculations.
	 *
	 * @param latitude
	 * @param longitude
	 */
	public synchronized void setLocation(String latitude,String longitude)
	{
		LnLnlatPosn newLocation=new LnLnlatPosn();
		newLocation.lat=Double.parseDouble(latitude);
		newLocation.lng=Double.parseDouble(longitude);
		location=newLocation;
		L.config("Set location for sunset calculations to "+location);
	}

	private double getZenith(String name)
	{
		if(name==null || "OFFICIAL".equalsIgnoreCase(name))
			return Solar.LN_SOLAR_STANDART_HORIZON;
		else if("ASTRONOMICAL".equalsIgnoreCase(name))
			return -18;
		else if("NAUTICAL".equalsIgnoreCase(name))
			return -12;
		else if("CIVIL".equalsIgnoreCase(name))
			return -6;
		throw new IllegalArgumentException("Unknown zenith "+name);
	}

	private LnRstTime calcSSTimes(String zenith)
	{
		double jd=JulianDay.ln_get_julian_from_sys();
		LnRstTime result=new LnRstTime();
		Solar.ln_get_solar_rst_horizon(jd, location, getZenith(zenith), result);
		return result;
	}

	private String jdToTimeString(double jd)
	{
		 LnDate date=new LnDate();
		 JulianDay.ln_get_date(jd, date);
		 Calendar utcCal=Calendar.getInstance();
		 utcCal.setTimeZone(TimeZone.getTimeZone("UTC"));
		 utcCal.set(date.years,date.months-1,date.days,date.hours,date.minutes,(int)date.seconds);
		 Calendar cal=Calendar.getInstance();
		 cal.setTimeInMillis(utcCal.getTimeInMillis());
		 return String.format("%02d:%02d",cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE));
	}

	/**
	 * Get the Sunrise time (format hh:mm) for the given Zentih
	 *
	 * @param zenith OFFICIAL (or null) / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunrise time
	 */
	public String getSunrise(String zenith)
	{
		LnRstTime res=calcSSTimes(zenith);
		return jdToTimeString(res.rise);
	}
	/**
	 * Get the Sunset time (format hh:mm) for the given Zentih
	 *
	 * @param zenith OFFICIAL (or null) / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunset time
	 */
	public String getSunset(String zenith)
	{
		LnRstTime res=calcSSTimes(zenith);
		return jdToTimeString(res.set);
	}

	/**
	 * Shortcut for getSunrise() with the given zenith
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getAstronomicalSunrise()
	{
		return getSunrise("ASTRONOMICAL");
	}
	/**
	 * Shortcut for getSunset() with the given zenith
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getAstronomicalSunset()
	{
		return getSunset("ASTRONOMICAL");
	}

	/**
	 * Shortcut for getSunrise() with the given zenith
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getNauticalSunrise()
	{
		return getSunrise("NAUTICAL");
	}
	/**
	 * Shortcut for getSunset() with the given zenith
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getNauticalSunset()
	{
		return getSunset("NAUTICAL");
	}

	/**
	 * Shortcut for getSunrise() with the given zenith
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getCivilSunrise()
	{
		return getSunrise("CIVIL");
	}
	/**
	 * Shortcut for getSunset() with the given zenith
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getCivilSunset()
	{
		return getSunset("CIVIL");
	}

	/**
	 * Shortcut for getSunrise() with the given zenith
	 * @return the sunrise time
	 *
	 * @see getSunrise
	 */
	public String getOfficialSunrise()
	{
		return getSunrise(null);
	}
	/**
	 * Shortcut for getSunset() with the given zenith
	 * @return the sunset time
	 *
	 * @see getSunset
	 */
	public String getOfficialSunset()
	{
		return getSunset(null);
	}

	/**
	 * Determine whether we're currently having daylight according to the given zenith
	 *
	 * @param zenith
	 * @return whether it is currently daylight
	 */

	public boolean isDaylight(String zenith)
	{
		LnRstTime res=calcSSTimes(zenith);
		double jd=JulianDay.ln_get_julian_from_sys();
		return jd>=res.rise && jd<=res.set;
	}

	/**
	 * Shortcut to isDaylight() with the given zenith
	 * @return whether it is currently daylight
	 */
	public boolean isCivilDaylight()
	{
		return isDaylight("CIVIL");
	}

	/**
	 * Shortcut to isDaylight() with the given zenith
	 * @return whether it is currently daylight
	 */
	public boolean isNauticalDaylight()
	{
		return isDaylight("NAUTICAL");
	}

	/**
	 * Shortcut to isDaylight() with the given zenith
	 * @return whether it is currently daylight
	 */
	public boolean isAstronomicalDaylight()
	{
		return isDaylight("ASTRONOMICAL");
	}

	/**
	 * Shortcut to isDaylight() with the given zenith
	 * @return whether it is currently daylight
	 */
	public boolean isOfficialDaylight()
	{
		return isDaylight(null);
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

	public static Time getInstance()
	{
		return instance;
	}

	private static Logger L=Logger.getLogger(Time.class.getName());
}

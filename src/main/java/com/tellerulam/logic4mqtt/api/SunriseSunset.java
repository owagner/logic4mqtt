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

import com.luckycatlabs.sunrisesunset.*;
import com.luckycatlabs.sunrisesunset.calculator.*;
import com.luckycatlabs.sunrisesunset.dto.*;

public class SunriseSunset
{
	private static final SunriseSunset instance=new SunriseSunset();

	private Location location;
	private SolarEventCalculator sscalc;

	private synchronized SolarEventCalculator getCalculator()
	{
		if(location==null)
			location=new Location("51.358813","7.241483");
		if(sscalc==null)
			sscalc=new SolarEventCalculator(location,TimeZone.getDefault());
		return sscalc;
	}

	/**
	 * Set the latitude and longitude used for all subsequent calculations.
	 *
	 * @param latitude
	 * @param longitude
	 */
	public synchronized void setLocation(String latitude,String longitude)
	{
		location=new Location(latitude,longitude);
		sscalc=null;
	}

	private Zenith getZenith(String name)
	{
		if(name==null || "OFFICIAL".equalsIgnoreCase(name))
			return Zenith.OFFICIAL;
		else if("ASTRONOMICAL".equalsIgnoreCase(name))
			return Zenith.ASTRONOMICAL;
		else if("NAUTICAL".equalsIgnoreCase(name))
			return Zenith.NAUTICAL;
		else if("CIVIL".equalsIgnoreCase(name))
			return Zenith.CIVIL;
		throw new IllegalArgumentException("Unknown zenith "+name);
	}

	/**
	 * Get the Sunrise time (format hh:mm) for the given Zentih
	 *
	 * @param zenith OFFICIAL (or null) / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunrise time
	 */
	public String getSunrise(String zenith)
	{
		return getCalculator().computeSunriseTime(getZenith(zenith), Calendar.getInstance());
	}
	/**
	 * Get the Sunset time (format hh:mm) for the given Zentih
	 *
	 * @param zenith OFFICIAL (or null) / ASTRONOMICAL / NAUTICAL / CIVIL
	 * @return the sunrise time
	 */
	public String getSunset(String zenith)
	{
		return getCalculator().computeSunsetTime(getZenith(zenith), Calendar.getInstance());
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
		Calendar now=Calendar.getInstance();
		Zenith z=getZenith(zenith);
		Calendar sunrise=getCalculator().computeSunriseCalendar(z, now);
		Calendar sunset=getCalculator().computeSunsetCalendar(z, now);
		return(now.compareTo(sunrise)>0 && now.compareTo(sunset)<0);
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
	public boolean isOfficial()
	{
		return isDaylight(null);
	}

	public static SunriseSunset getInstance()
	{
		return instance;
	}

	private static Logger L=Logger.getLogger(SunriseSunset.class.getName());
}

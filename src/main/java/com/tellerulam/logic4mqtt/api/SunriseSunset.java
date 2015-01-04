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
import com.luckycatlabs.sunrisesunset.dto.*;

public class SunriseSunset
{
	private static final SunriseSunset instance=new SunriseSunset();

	private Location location;
	private SunriseSunsetCalculator sscalc;

	private synchronized SunriseSunsetCalculator getCalculator()
	{
		if(location==null)
			location=new Location("51.358813","7.241483");
		if(sscalc==null)
			sscalc=new SunriseSunsetCalculator(location,TimeZone.getDefault());
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

	public String getAstronomicalSunrise()
	{
		return getCalculator().getAstronomicalSunriseForDate(Calendar.getInstance());
	}
	public String getAstronomicalSunset()
	{
		return getCalculator().getAstronomicalSunsetForDate(Calendar.getInstance());
	}
	public String getNauticalSunrise()
	{
		return getCalculator().getNauticalSunriseForDate(Calendar.getInstance());
	}
	public String getNauticalSunset()
	{
		return getCalculator().getNauticalSunsetForDate(Calendar.getInstance());
	}
	public String getCivilSunrise()
	{
		return getCalculator().getCivilSunriseForDate(Calendar.getInstance());
	}
	public String getCivilSunset()
	{
		return getCalculator().getCivilSunsetForDate(Calendar.getInstance());
	}
	public String getOfficialSunrise()
	{
		return getCalculator().getOfficialSunriseForDate(Calendar.getInstance());
	}
	public String getOfficialSunset()
	{
		return getCalculator().getOfficialSunsetForDate(Calendar.getInstance());
	}

	public static SunriseSunset getInstance()
	{
		return instance;
	}

	private static Logger L=Logger.getLogger(SunriseSunset.class.getName());
}

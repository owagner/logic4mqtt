package com.tellerulam.logic4mqtt.api;

import static org.junit.Assert.*;

import java.util.*;


import org.junit.*;

public class TimeTest
{
	@Test
	public void testSunset()
	{
		Time t=InstanceManager.getTimeInstance();

		String hor[]={"DAYLIGHT","OFFICIAL","CIVIL","NAUTICAL","ASTRONOMICAL"};
		for(String h:hor)
		{
			System.out.println(h+" SUNRISE: "+t.getSunrise(h));
			System.out.println(h+" SUNSET: "+t.getSunset(h));
		}
		System.out.println("AZIMUTH:"+t.getSunAzimuth());
		System.out.println("ALTITUDE:"+t.getSunAltitude());
	}

	private Time prepareTimeForTest()
	{
		Calendar tst=Calendar.getInstance();
		tst.set(Calendar.HOUR_OF_DAY, 14);
		tst.set(Calendar.MINUTE, 23);
		Time.fixedNow=tst;
		return InstanceManager.getTimeInstance();
	}

	@Test
	public void testBeforePastBetween()
	{
		Time t=prepareTimeForTest();

		assertTrue(t.isBefore("15:00"));
		assertFalse(t.isBefore("14:00"));
		assertTrue(t.isAfter("5:00"));
		assertFalse(t.isAfter("14:25:11"));
		assertTrue(t.isBetween("12:00:55", "20:00"));
		assertFalse(t.isBetween("12:00","14:11:11"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testTimeParser()
	{
		Time t=prepareTimeForTest();
		assertTrue(t.isBetween("12:00:55", "12:00:54"));
	}
}

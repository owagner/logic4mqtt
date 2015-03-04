package com.tellerulam.logic4mqtt;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.Test;

import com.tellerulam.logic4mqtt.api.Time;

public class NattyTimerTest
{
	@Test
	public void testSunstuff()
	{
		String cases[]={
			"civil sunset", "18:17:00",
			"civil sunrise", "07:29:00",

			"1 minute after civil sunset", "18:18:00",
			"1 hour before civil sunrise", "06:29:00",

			"1 min after civil sunset", "18:18:00",
			"1 hour before civil sunrise", "06:29:00",

			"61 minutes after civil sunset", "19:18:00",
			"1 seconds before civil sunrise", "07:28:59",
			"every one second before civil sunrise", "07:28:59",
		};
		DateFormat df=new SimpleDateFormat("HH:mm:ss");
		for(int ix=0;ix<cases.length;ix+=2)
		{
			NattyTimer t=new NattyTimer("test",cases[ix],null,null){
				@Override
				protected Time getTimeInstance()
				{
					return new Time(){
						@Override
						public String getSunset(String zenith)
						{
							return "18:"+(14+(zenith.hashCode()%12));
						}
						@Override
						public String getSunrise(String zenith)
						{
							return "07:"+(32-(zenith.hashCode()%12));
						}
					};
				}
			};
			assertEquals(cases[ix+1],df.format(t.getFirstDateForTest()));
		}
	}
}

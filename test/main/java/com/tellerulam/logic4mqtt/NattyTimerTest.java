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

			"civil sunset+1m", "18:18:00",
			"civil sunrise-1hour", "06:29:00",

			"civil sunset +1 m", "18:18:00",
			"civil sunrise -1 hour", "06:29:00",

			"civil sunset +61 m", "19:18:00",
			"civil sunrise -1 seconds", "07:28:59",
			"every civil sunrise -1 s", "07:28:59",
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

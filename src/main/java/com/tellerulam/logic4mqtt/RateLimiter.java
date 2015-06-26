package com.tellerulam.logic4mqtt;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RateLimiter
{
	private static final Map<String,Calendar> rateLimits=new HashMap<>();

	public static boolean rateLimit(String symbolicName,int intervalInSeconds)
	{
		Calendar cal=Calendar.getInstance();
		synchronized(rateLimits)
		{
			Calendar nextExecutionAfter=rateLimits.get(symbolicName);
			if(nextExecutionAfter!=null)
				if(cal.before(nextExecutionAfter))
					return false;
			cal.add(Calendar.SECOND, intervalInSeconds);
			rateLimits.put(symbolicName,cal);
		}
		return true;
	}
}

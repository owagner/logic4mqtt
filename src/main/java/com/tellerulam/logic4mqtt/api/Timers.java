package com.tellerulam.logic4mqtt.api;

//import java.util.logging.*;

import com.tellerulam.logic4mqtt.*;

/**
 * This class manages event timers.
 *
 */
public class Timers
{
	static final Timers instance=new Timers();
	private Timers()
	{
		/* Keep private */
	}

	public void addTimer(String symbolicName,String timespec,TimerCallbackInterface callback,Object userdata)
	{
		LogicTimer.addTimer(symbolicName, timespec, callback, userdata);
	}
	public void addTimer(String symbolicName,String timespec,TimerCallbackInterface callback)
	{
		LogicTimer.addTimer(symbolicName, timespec, callback, null);
	}
	public int remTimer(String symbolicName)
	{
		return LogicTimer.remTimer(symbolicName);
	}

	/**
	 * Utility method to rate-limit execution. Called with a symbolic name and a minimum interval,
	 * will return a boolean whether to execute now or not.
	 *
	 * Example: React to a PIR movement detection, but only sent email once every 300 seconds
	 *
	 * Events.onUpdate("device//pir/MOTION",function(topic,val){
	 * 	if(Timers.rateLimit("PIR_REPORT",300))
	 *  {
	 *    Mail.sendMail("ALARM!","Movement detected");
	 *  }
	 * });
	 *
	 *
	 * @param symbolicName
	 * @param intervalInSeconds
	 * @return whether to execute now
	 */
	public boolean rateLimit(String symbolicName,int intervalInSeconds)
	{
		return RateLimiter.rateLimit(symbolicName,intervalInSeconds);
	}

	//private static Logger L=Logger.getLogger(Timers.class.getName());
}

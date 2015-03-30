package com.tellerulam.logic4mqtt.api;

//import java.util.logging.*;

import com.tellerulam.logic4mqtt.*;

public class Timers
{
	private static final Timers instance=new Timers();

	public static Timers getInstance()
	{
		return instance;
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

	//private static Logger L=Logger.getLogger(Timers.class.getName());
}

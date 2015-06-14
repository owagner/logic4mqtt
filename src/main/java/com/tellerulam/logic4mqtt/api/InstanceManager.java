package com.tellerulam.logic4mqtt.api;

public class InstanceManager
{
	public static Events getEventsInstance()
	{
		return Events.instance;
	}
	public static Time getTimeInstance()
	{
		return Time.instance;
	}
	public static Timers getTimersInstance()
	{
		return Timers.instance;
	}
	public static Utilities getUtilitiesInstance()
	{
		return Utilities.instance;
	}
	public static Mail getMailInstance()
	{
		return Mail.instance;
	}
}

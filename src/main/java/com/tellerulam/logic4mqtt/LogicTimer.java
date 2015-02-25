package com.tellerulam.logic4mqtt;

import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.quartz.*;

public abstract class LogicTimer
{
	public static void addTimer(String symbolicName,String timespec,TimerCallbackInterface callback,Object userdata)
	{
		LogicTimer t;
		/*
		 * We first try to parse the timespec as a CronExpression. If this fails,
		 * we try the Natty natural language parser. If that also fails, this function
		 * will throw an Exception
		 */
		try
		{
			CronExpression cex=new CronExpression(timespec);
			t=new CronTimer(symbolicName,timespec,callback,userdata,cex);
		}
		catch(ParseException e)
		{
			/* Try Natty instead */
			t=new NattyTimer(symbolicName,timespec,callback,userdata);
		}
		synchronized(allTimers)
		{
			List<LogicTimer> timers=allTimers.get(symbolicName);
			if(timers==null)
				allTimers.put(symbolicName,timers=new ArrayList<>());
			timers.add(t);
		}
		t.start();
	}
	public static int remTimer(String symbolicName)
	{
		synchronized(allTimers)
		{
			List<LogicTimer> timers=allTimers.remove(symbolicName);
			if(timers!=null)
			{
				for(LogicTimer t:timers)
					t.cancel();
				return timers.size();
			}
		}
		return 0;
	}

	/*
	 * Used by subclasses to remove themselves when they're expired
	 */
	protected void removeOneTimer(LogicTimer t)
	{
		synchronized(allTimers)
		{
			List<LogicTimer> timers=allTimers.get(symbolicName);
			if(timers!=null)
				timers.remove(t);
		}
	}

	static final Map<String,List<LogicTimer>> allTimers=new HashMap<>();

	static public Collection<LogicTimer> getAllTimers()
	{
		Collection<LogicTimer> l=new ArrayList<>();
		for(List<LogicTimer> tl:allTimers.values())
			l.addAll(tl);
		return l;
	}

	protected final String symbolicName;
	protected final String timespec;
	protected final TimerCallbackInterface callback;
	protected final Object userdata;

	protected boolean canceled;
	protected TimerTask currentTimerTask;

	abstract void start();

	private static DateFormat summaryFmt=DateFormat.getDateTimeInstance();

	public synchronized String getCmdlineSummary()
	{
		StringBuilder msg=new StringBuilder();
		msg.append(symbolicName);
		msg.append('\t');
		msg.append(summaryFmt.format(new Date(currentTimerTask.scheduledExecutionTime())));
		msg.append('\t');
		msg.append(timespec);
		msg.append('\t');
		msg.append(callback);
		return msg.toString();
	}


	void cancel()
	{
		this.canceled=true;
		if(currentTimerTask!=null)
			currentTimerTask.cancel();
	}

	protected LogicTimer(String symbolicName,String timespec,TimerCallbackInterface callback,Object userdata)
	{
		this.symbolicName=symbolicName;
		this.timespec=timespec;
		this.callback=callback;
		this.userdata=userdata;
	}

	static final Timer t=new Timer(true);

	private static final Logger L=Logger.getLogger(LogicTimer.class.getName());

	void runCallback()
	{
		try
		{
			callback.run(userdata);
		}
		catch(Throwable t)
		{
			L.log(Level.WARNING,"Error when executing timer callback",t);
		}
	}
}

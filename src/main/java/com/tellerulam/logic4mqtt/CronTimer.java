package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.logging.*;

import org.quartz.*;

public class CronTimer extends LogicTimer
{
	private final CronExpression cex;

	protected CronTimer(String symbolicName, String timespec, TimerCallbackInterface callback, Object userdata, CronExpression cex)
	{
		super(symbolicName,timespec,callback,userdata);
		this.cex=cex;
	}

	@Override
	public void start()
	{
		schedule();
	}

	private void schedule()
	{
		if(canceled)
			return;

		Date next=cex.getNextValidTimeAfter(new Date());
		currentTimerTask=new TimerTask()
		{
			@Override
			public void run()
			{
				runCallback();
				schedule();
			}
		};
		t.schedule(currentTimerTask, next);
		L.info("Scheduled "+this+" for "+next);
	}

	@Override
	public String toString()
	{
		StringBuilder msg=new StringBuilder();
		msg.append("[CT:");
		msg.append(symbolicName);
		msg.append(":");
		msg.append(cex);
		msg.append("]");
		return msg.toString();
	}

	private static final Logger L=Logger.getLogger(CronTimer.class.getName());

}

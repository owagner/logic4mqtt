package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;
import java.util.*;

import com.tellerulam.logic4mqtt.*;

public class CmdTimers extends Cmd
{
	CmdTimers()
	{
		super("TIMERS","Show list of active timers");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		Collection<LogicTimer> timers=LogicTimer.getAllTimers();
		for(LogicTimer t:timers)
		{
			w.println(t.getCmdlineSummary());
		}
		w.println(".");
	}

}

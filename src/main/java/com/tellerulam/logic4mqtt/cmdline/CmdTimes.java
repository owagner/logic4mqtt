package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;

import com.tellerulam.logic4mqtt.api.*;

public class CmdTimes extends Cmd
{
	CmdTimes()
	{
		super("TIMES","Get current sunrise/sunset times for the various zeniths");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		Time t=InstanceManager.getTimeInstance();
		final String zeniths[]={"ASTRONOMICAL","NAUTICAL","CIVIL","OFFICIAL"};
		for(String zenith:zeniths)
		{
			w.print(zenith);
			w.print(' ');
			w.print(t.getSunrise(zenith));
			w.print(' ');
			w.print(t.getSunset(zenith));
			w.print(' ');
			w.println(t.isDaylight(zenith)?"DL":"");
		}
		w.println(".");
	}

}

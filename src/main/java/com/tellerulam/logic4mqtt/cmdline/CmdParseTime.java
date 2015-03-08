package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;

import com.tellerulam.logic4mqtt.*;
import com.tellerulam.logic4mqtt.api.*;

public class CmdParseTime extends Cmd
{
	CmdParseTime()
	{
		super("PARSETIME","Parse a natural language time description");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		try
		{
			NattyTimer.ParsedSpec spec=NattyTimer.parseTimeSpec(args.args[1]);
			w.print(spec.dates);
			w.print('\t');
			if(spec.recurrence)
			{
				w.print("REC");
				if(spec.recursUntil!=null)
				{
					w.print(" UNTIL ");
					w.print(spec.recursUntil);
				}
				w.println();
			}
			else
			{
				w.println("ONCE");
			}
		}
		catch(IllegalArgumentException ie)
		{
			w.println("Invalid time specification: "+ie.getMessage());
		}
	}

}

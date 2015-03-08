package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;
import java.util.*;

import com.tellerulam.logic4mqtt.*;

public class CmdEvents extends Cmd
{
	CmdEvents()
	{
		super("EVENTS","Show list of active event handlers");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		Collection<EventHandler> handlers=EventHandler.getAllHandlers();
		for(EventHandler h:handlers)
		{
			w.println(h.getCmdlineSummary());
		}
		w.println(".");
	}

}

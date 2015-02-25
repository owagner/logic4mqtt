package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;

public class CmdHelp extends Cmd
{
	CmdHelp()
	{
		super("HELP","Show command reference");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		for(Cmd cmd:getAllCommands())
		{
			w.println(cmd.cmd+"\t"+cmd.desc);
		}
		w.println(".");
	}

}

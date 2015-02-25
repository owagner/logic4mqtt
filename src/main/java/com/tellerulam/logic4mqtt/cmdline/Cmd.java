package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;
import java.util.*;

public abstract class Cmd
{
	private static final Map<String,Cmd> commands=new HashMap<>();

	static void initCommands()
	{
		add(new CmdHelp());
		add(new CmdQuit());
		add(new CmdTimers());
		add(new CmdTimes());
	}
	private static void add(Cmd cmd)
	{
		commands.put(cmd.cmd,cmd);
	}


	protected static Collection<Cmd> getAllCommands()
	{
		return commands.values();
	}

	static Cmd get(String name)
	{
		return commands.get(name);
	}

	protected final String cmd;
	protected final String desc;

	protected Cmd(String cmd,String desc)
	{
		this.cmd=cmd;
		this.desc=desc;
	}

	public abstract void exec(PrintWriter w,ArgSplitter args) throws Exception;

}

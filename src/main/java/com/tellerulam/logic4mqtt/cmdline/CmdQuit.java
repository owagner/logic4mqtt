package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;

public class CmdQuit extends Cmd
{
	CmdQuit()
	{
		super("QUIT","Close connection");
	}

	@Override
	public void exec(PrintWriter w, ArgSplitter args) throws Exception
	{
		throw new EOFException();
	}

}

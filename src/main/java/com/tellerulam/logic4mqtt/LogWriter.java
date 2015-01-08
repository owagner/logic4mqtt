package com.tellerulam.logic4mqtt;

import java.io.*;
import java.util.logging.*;

public class LogWriter extends Writer
{
	private final Level level;
	private final String logPrefix;
	private final StringBuilder buffer=new StringBuilder();
	private final Logger L=Logger.getLogger(LogWriter.class.getName());

	LogWriter(Level loglevel,String logPrefix)
	{
		this.level=loglevel;
		this.logPrefix="["+logPrefix+"] ";
	}

	@Override
	public void close() throws IOException
	{
		/* Ignore */
	}

	@Override
	public void flush() throws IOException
	{
		/* Ignore */
	}

	@Override
	public void write(char[] a, int offs, int len) throws IOException
	{
		for(;len--!=0;offs++)
		{
			if(a[offs]=='\r')
				continue;
			else if(a[offs]=='\n')
			{
				L.log(level,logPrefix+buffer);
				buffer.setLength(0);
			}
			else
				buffer.append(a[offs]);
		}
	}

}

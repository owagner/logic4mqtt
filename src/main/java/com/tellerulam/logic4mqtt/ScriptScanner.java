package com.tellerulam.logic4mqtt;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.script.*;

import com.tellerulam.logic4mqtt.api.*;

public class ScriptScanner
{
	private static class LScript implements Comparable<LScript>
	{
		final File f;
		final ScriptEngine scriptEngine;
		private LScript(File f, ScriptEngine scriptEngine)
		{
			this.f = f;
			this.scriptEngine = scriptEngine;
		}
		@Override
		public int compareTo(LScript l)
		{
			return f.getPath().compareTo(l.f.getPath());
		}
	}

	private static List<LScript> scripts=new ArrayList<>();
	private static ScriptEngineManager sem=new ScriptEngineManager();

	private static final Map<String,ScriptEngine> engines=new HashMap<>();
	static private ScriptEngine getEngine(String ext)
	{
		ScriptEngine se=engines.get(ext);
		if(se==null)
		{
			se=sem.getEngineByExtension(ext);
			if(se!=null)
			{
				// Configure the engine
				se.put("Events", Events.getInstance());
				se.put("Timers", Timers.getInstance());
				se.put("Utilities", Utilities.getInstance());
				se.put("SunriseSunset", SunriseSunset.getInstance());
				engines.put(ext,se);
			}
		}
		return se;
	}

	static void scanDirectory(File dir)
	{
		File[] content=dir.listFiles();
		for(File f:content)
		{
			if(f.isDirectory())
				scanDirectory(f);
			else
			{
				// Extract the extensions
				String name=f.getName();
				int eix=name.lastIndexOf('.');
				if(eix>0)
				{
					String ext=name.substring(eix+1);
					ScriptEngine se=getEngine(ext);
					if(se==null)
					{
						L.log(Level.WARNING, "Unable to find a matching script engine for script "+f);
					}
					else
					{
						scripts.add(new LScript(f,se));
					}
				}
			}
		}
	}

	static void doScan(String dirName)
	{
		// Gather scripts
		File dir=new File(dirName);
		L.info("Scanning script directory "+dir.getAbsolutePath());
		scanDirectory(dir);
		Collections.sort(scripts);
		if(scripts.size()==0)
		{
			L.severe("No scripts found, exiting");
			System.exit(0);
		}
		L.info("Found "+scripts.size()+" runnable scripts, now executing");
		// ...and run them
		for(LScript ls:scripts)
		{
			L.info("Executing "+ls.f+" with "+ls.scriptEngine.getFactory().getEngineName());
			try
			{
				String logPrefix=ls.f.getName();
				ls.scriptEngine.getContext().setWriter(new LogWriter(Level.INFO,logPrefix));
				ls.scriptEngine.getContext().setErrorWriter(new LogWriter(Level.WARNING,logPrefix));
				ls.scriptEngine.eval(new FileReader(ls.f));
			}
			catch(FileNotFoundException e)
			{
				L.log(Level.SEVERE, "Script "+ls.f+" no longer found?!?",e);
			}
			catch(ScriptException e)
			{
				L.log(Level.SEVERE, "Error executing script "+ls.f,e);
				System.exit(1);
			}
		}
		/*
		 * Since we no longer have individual contexts when callbacks are being used, we should reset this
		 */
	}

	private static final Logger L=Logger.getLogger(ScriptScanner.class.getName());

}

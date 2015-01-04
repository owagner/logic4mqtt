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
					ScriptEngine se=sem.getEngineByExtension(ext);
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
				ls.scriptEngine.put("Events", Events.getInstance());
				ls.scriptEngine.put("Timers", Timers.getInstance());
				ls.scriptEngine.put("Utilities", Utilities.getInstance());
				ls.scriptEngine.put("SunriseSunset", SunriseSunset.getInstance());
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
	}

	private static final Logger L=Logger.getLogger(ScriptScanner.class.getName());

}

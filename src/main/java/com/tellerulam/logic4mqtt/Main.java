package com.tellerulam.logic4mqtt;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import org.eclipse.paho.client.mqttv3.*;

import com.tellerulam.logic4mqtt.cmdline.*;

public class Main
{
	static final Timer t=new Timer(true);

	public static String getVersion()
	{
		// First, try the manifest tag
		String version=Main.class.getPackage().getImplementationVersion();
		if(version==null)
		{
			// Read from build.gradle instead
			try
			{
				List<String> buildFile=Files.readAllLines(Paths.get("build.gradle"),StandardCharsets.UTF_8);
				Pattern p=Pattern.compile("version.*=.*'([^']+)");
				for(String l:buildFile)
				{
					Matcher m=p.matcher(l);
					if(m.find())
						return m.group(1);
				}
			}
			catch(IOException e)
			{
				/* Ignore, no version */
			}
		}
		return version;
	}

	public static void main(String[] args) throws MqttException, SecurityException, IOException
	{
		/*
		 * Interpret all command line arguments as property definitions (without the hm2mqtt prefix)
		 */
		for(String s:args)
		{
			String sp[]=s.split("=",2);
			if(sp.length!=2)
			{
				System.out.println("Invalid argument (no '='): "+s);
				System.exit(1);
			}
			System.setProperty("logic4mqtt."+sp[0],sp[1]);
		}
		SyslogHandler.readConfig();
		Logger.getLogger(Main.class.getName()).info("logic4mqtt V"+getVersion()+" (C) 2015 Oliver Wagner <owagner@tellerulam.com>");
		CmdlineHandler.init();
		MQTTHandler.init();
		String scriptDirs=System.getProperty("logic4mqtt.scripts.dir","scripts");
		for(String scriptDir:scriptDirs.split(":"))
			ScriptScanner.doScan(scriptDir);
	}
}

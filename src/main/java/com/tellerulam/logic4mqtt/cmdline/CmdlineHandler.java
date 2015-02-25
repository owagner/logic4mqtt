package com.tellerulam.logic4mqtt.cmdline;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import com.tellerulam.logic4mqtt.*;

public class CmdlineHandler extends Thread
{
	static public void init()
	{
		int cmdLinePort=Integer.getInteger("logic4mqtt.cmdline.port",-1).intValue();
		if(cmdLinePort<=0)
			return;
		Cmd.initCommands();
		new CmdlineListener(cmdLinePort).start();
		L.config("Command line handler listening on port "+cmdLinePort);
	}

	/* Here be dragons */

	private static class CmdlineListener extends Thread
	{
		private final int port;
		protected CmdlineListener(int port)
		{
			super("CmdlineListener on port "+port);
			this.port=port;
			setDaemon(true);
		}

		@Override
		public void run()
		{
			try(ServerSocket ss=new ServerSocket(port))
			{
				Socket s;
				while((s=ss.accept())!=null)
				{
					new CmdlineHandler(s).start();
				}
			}
			catch(IOException e)
			{
				L.log(Level.WARNING,"Failed to start CmdlineListener on port "+port,e);
			}
		}
	}

	protected CmdlineHandler(Socket s)
	{
		super("CmdlineHandler for "+s);
		this.s=s;
		setDaemon(true);
	}
	private final Socket s;
	private BufferedReader br;
	private PrintWriter w;

	private void handleLine(String cmdline) throws Exception
	{
		ArgSplitter args=new ArgSplitter(cmdline);
		Cmd cmd=Cmd.get(args.args[0]);
		if(cmd==null)
			w.println("-Unknown command "+args.args[0]);
		else
			cmd.exec(w, args);
	}

	@Override
	public void run()
	{
		String cmdline=null;
		try
		{
			br=new BufferedReader(new InputStreamReader(s.getInputStream()));
			w=new PrintWriter(s.getOutputStream(),true);
			w.println("logic4mqtt V"+Main.getVersion()+" - use HELP for list of commands");
			w.flush();
			while((cmdline=br.readLine())!=null)
			{
				handleLine(cmdline);
				cmdline=null;
			}
		}
		catch(EOFException eof)
		{
			/* Ignore silently */
		}
		catch(Exception e)
		{
			L.log(Level.INFO,"Error while processing line "+cmdline);
		}
		finally
		{
			try
			{
				s.close();
			}
			catch(IOException e)
			{
				/* Ignore */
			}
		}
	}

	private static final Logger L=Logger.getLogger(CmdlineHandler.class.getName());

}

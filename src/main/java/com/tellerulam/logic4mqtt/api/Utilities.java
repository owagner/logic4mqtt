/*
 * This object contains utility functions to be used from scripts.
 *
 */

package com.tellerulam.logic4mqtt.api;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

/**
 * Various utility functions.
 *
 */
public class Utilities
{
	static final Utilities instance=new Utilities();
	private Utilities()
	{
		/* Keep private */
	}

	/**
	 *
	 * Send a Wake On Lan "Magic Packet"
	 *
	 * @param macSpec The MAC address to wake, in the form aa:bb:cc:dd:ee:ff or aa-bb-cc-dd-ee-ff
	 * @param interfacename optional name of the network interface to send the packet with. If null,
	 *                      will send to all interfaces which have broadcast addresses
	 */
	public void sendWOL(String macSpec,String interfacename)
	{
        byte[] mac = new byte[6];
        try
        {
        	String[] specParts = macSpec.split("(\\:|\\-)");
        	for(int i=0;i<6;i++)
        		mac[i]=(byte)Integer.parseInt(specParts[i],16);
        }
        catch(Exception e)
        {
        	throw new IllegalArgumentException("Invalid MAC address "+macSpec);
        }
        /* Prepare the payload */
        byte packet[]=new byte[6+16*6];
        Arrays.fill(packet,0,6,(byte)0xff);
        for(int i=6;i<packet.length;i+=6)
        	System.arraycopy(mac,0,packet,i,6);

        /* Make it easy - send to all interfaces */
        try(DatagramSocket ds=new DatagramSocket())
        {
	        Enumeration<NetworkInterface> interfaces=NetworkInterface.getNetworkInterfaces();
	        while(interfaces.hasMoreElements())
	        {
	        	NetworkInterface ni=interfaces.nextElement();
        		if(ni.isLoopback())
        			continue;
        		if(interfacename!=null && !interfacename.equals(ni.getName()))
        			continue;
	        	for(InterfaceAddress ifaddr:ni.getInterfaceAddresses())
	        	{
	        		InetAddress bcaddr=ifaddr.getBroadcast();
	        		if(bcaddr==null)
	        			continue;
	                DatagramPacket dp=new DatagramPacket(packet,packet.length,bcaddr,9);
	                ds.send(dp);
	                L.info("Successfully sent WOL packet to "+macSpec+" with "+ni+"/"+bcaddr);
	        	}
	        }
        }
        catch(Exception e)
        {
        	L.log(Level.WARNING,"Error sending WOL packet to "+macSpec,e);
        }
	}
	/**
	 *
	 * Send a Wake On Lan "Magic Packet"
	 *
	 * @param macSpec The MAC address to wake, in the form aa:bb:cc:dd:ee:ff or aa-bb-cc-dd-ee-ff
	 */
	public void sendWOL(String macSpec)
	{
		sendWOL(macSpec,null);
	}

	/**
	 * Send a simple, raw network message via TCP or UDP to a given host and port, and
	 * optionally read a single-line response.
	 *
	 * This is intended for simple automation protocols. If you need more flexibility,
	 * look into the libraries of your scripting language.
	 *
	 * Note: The complete script engine will block while waiting timeoutMillis. It is
	 * recommended to keep the value as low as possible. Do not set it to a value
	 * higher than zero unless you really need the response.
	 *
	 * @param host the host to send the message to. Either an IP address or a hostname.
	 * @param port the port number (1-65535)
	 * @param message the payload of the message
	 * @param udp if true, UDP will be used, TCP otherwise
	 * @param timeoutMillis amount of time to wait for a response. If 0, we don't wait
	 * @return the received response, or null if anything went wrong or a timeout occured
	 */
	public String sendNetMessage(String host,int port,String message,boolean udp,int timeoutMillis)
	{
		String response=null;
		try
		{
			if(udp)
			{
				DatagramSocket s=new DatagramSocket();
				byte payload[]=message.getBytes(StandardCharsets.UTF_8);
				DatagramPacket p=new DatagramPacket(payload,payload.length);
				s.connect(InetAddress.getByName(host), port);
				s.send(p);
				if(timeoutMillis!=0)
				{
					s.setSoTimeout(timeoutMillis);
					s.receive(p);
					response=new String(p.getData(),StandardCharsets.UTF_8);
				}
				s.close();
			}
			else
			{
				Socket s=new Socket(host,port);
				s.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
				if(timeoutMillis!=0)
				{
					s.setSoTimeout(timeoutMillis);
					BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8));
					response=br.readLine();
				}
				s.close();
			}
		}
		catch(Exception e)
		{
			L.log(Level.WARNING,"sendNetMessage("+host+","+port+","+message+","+udp+","+timeoutMillis+") failed",e);
		}
		return response;
	}

	/**
	 * Send a simple, raw network message via TCP or UDP to a given host and port.
	 *
	 * This is intended for simple automation protocols. If you need more flexibility,
	 * look into the libraries of your scripting language.
	 *
	 * @param host the host to send the message to. Either an IP address or a hostname.
	 * @param port the port number (1-65535)
	 * @param message the payload of the message
	 * @param udp if true, UDP will be used, TCP otherwise
	 */
	public void sendNetMessage(String host,int port,String message,boolean udp)
	{
		sendNetMessage(host,port,message,udp,0);
	}

	/**
	 * Send a simple, raw network message via TCP to a given host and port.
	 *
	 * This is intended for simple automation protocols. If you need more flexibility,
	 * look into the libraries of your scripting language.
	 *
	 * @param host the host to send the message to. Either an IP address or a hostname.
	 * @param port the port number (1-65535)
	 * @param message the payload of the message
	 */
	public void sendNetMessage(String host,int port,String message)
	{
		sendNetMessage(host,port,message,false,0);
	}

	/**
	 * Execute a command with the system's command interpreter.
	 *
	 * Returns the command's output, or null in case execution failed.
	 * If the output contains multiple lines, they are seperated by newlines.
	 * The last (or only) line will not have a newline appended.
	 *
	 * @param cmd The command to execute @see Process.exec
	 * @return the output of the command as a string, or null.
	 */
	public String executeCommand(String cmd)
	{
		L.log(Level.INFO, "Executing external command "+cmd);
		try
		{
			StringBuilder result=new StringBuilder();
			Process proc=Runtime.getRuntime().exec(cmd);
			BufferedReader br=new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String l;
			while((l=br.readLine())!=null)
			{
				if(result.length()!=0)
					result.append('\n');
				result.append(l);
			}
			return result.toString();
		}
		catch(Exception e)
		{
			L.log(Level.INFO,"Execution of external command "+cmd+" failed",e);
			return null;
		}
	}

	private static Logger L=Logger.getLogger(Utilities.class.getName());
}

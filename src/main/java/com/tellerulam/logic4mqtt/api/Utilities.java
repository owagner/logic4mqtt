/*
 * This object contains utility functions to be used from scripts.
 *
 */

package com.tellerulam.logic4mqtt.api;

import java.net.*;
import java.util.*;
import java.util.logging.*;

public class Utilities
{
	private static final Utilities instance=new Utilities();

	public static Utilities getInstance()
	{
		return instance;
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

	private static Logger L=Logger.getLogger(Utilities.class.getName());
}

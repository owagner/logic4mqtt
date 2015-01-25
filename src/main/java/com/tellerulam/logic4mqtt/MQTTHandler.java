package com.tellerulam.logic4mqtt;

import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.*;

import com.eclipsesource.json.*;

public class MQTTHandler
{
	private final Logger L=Logger.getLogger(getClass().getName());

	public static void init() throws MqttException
	{
		instance=new MQTTHandler();
		instance.doInit();
	}

	private static MQTTHandler instance;

	private final String topicPrefix;
	private MQTTHandler()
	{
		String tp=System.getProperty("logic4mqtt.mqtt.topic","logic");
		if(!tp.endsWith("/"))
			tp+="/";
		topicPrefix=tp;
	}

	private MqttClient mqttc;

	private void queueConnect()
	{
		shouldBeConnected=false;
		Main.t.schedule(new TimerTask(){
			@Override
			public void run()
			{
				doConnect();
			}
		},10*1000);
	}

	private class StateChecker extends TimerTask
	{
		@Override
		public void run()
		{
			if(!mqttc.isConnected() && shouldBeConnected)
			{
				L.warning("Should be connected but aren't, reconnecting");
				queueConnect();
			}
		}
	}

	private boolean shouldBeConnected;

	private Object convertStringToObject(String val)
	{
		// Try double?
		if(val.indexOf('.')>=0)
		{
			try
			{
				return Double.valueOf(val);
			}
			catch(NumberFormatException nfe)
			{
				// Ok, not a double, can't be an Integer, can't be true/false, treat as String
				return val;
			}
		}
		// TODO: do we really want to do that?
		if("true".equalsIgnoreCase(val))
			return Boolean.TRUE;
		if("false".equalsIgnoreCase(val))
			return Boolean.FALSE;
		// Try Integer
		try
		{
			return Integer.valueOf(val);
		}
		catch(NumberFormatException nfe2)
		{
			// Not an Integer; maybe a Double in scientific notation without a decimal point?
			try
			{
				return Double.valueOf(val);
			}
			catch(NumberFormatException nfe3)
			{
				// String it is, then
				return val;
			}
		}
	}

	void processMessage(String topic,MqttMessage msg)
	{
		L.fine("Received "+msg+" to "+topic);

		// Determine whether the payload is JSON encoded or not
		String payload=new String(msg.getPayload(),StandardCharsets.UTF_8);
		Object transformedVal;

		if(payload.trim().startsWith("{"))
		{
			JsonObject data=JsonObject.readFrom(payload);
			JsonValue val=data.get("val");
			if(val.isNumber())
				transformedVal=Double.valueOf(val.asDouble());
			else if(val.isString())
				transformedVal=val.asString();
			else if(val.isBoolean())
				transformedVal=Integer.valueOf(val.asBoolean()?1:0);
			else if(val.isNull())
				transformedVal=null;
			else
				transformedVal=val.toString();
		}
		else
			transformedVal=convertStringToObject(payload);

		TopicCache t=TopicCache.storeTopic(topic,transformedVal);
		// If this is a retained message, do not dispatch an event
		if(msg.isRetained())
			return;
		EventHandler.dispatchEvent(topic,t);
	}

	private void doConnect()
	{
		L.info("Connecting to MQTT broker "+mqttc.getServerURI()+" with CLIENTID="+mqttc.getClientId()+" and status TOPIC PREFIX="+topicPrefix);

		MqttConnectOptions copts=new MqttConnectOptions();
		copts.setWill(topicPrefix+"connected", "0".getBytes(), 2, true);
		copts.setCleanSession(true);
		try
		{
			mqttc.connect(copts);
			mqttc.publish(topicPrefix+"connected", "2".getBytes(), 1, true);
			L.info("Successfully connected to broker, subscribing to #");
			try
			{
				mqttc.subscribe("#",0);
				shouldBeConnected=true;
			}
			catch(MqttException mqe)
			{
				L.log(Level.WARNING,"Error subscribing to topic hierarchy, check your configuration",mqe);
				throw mqe;
			}
		}
		catch(MqttException mqe)
		{
			L.log(Level.WARNING,"Error while connecting to MQTT broker, will retry: "+mqe.getMessage(),mqe);
			queueConnect(); // Attempt reconnect
		}
	}

	private void doInit() throws MqttException
	{
		String server=System.getProperty("logic4mqtt.mqtt.server","tcp://localhost:1883");
		String clientID=System.getProperty("logic4mqtt.mqtt.clientid","logic4mqtt");
		mqttc=new MqttClient(server,clientID,new MemoryPersistence());
		mqttc.setCallback(new MqttCallback() {
			@Override
			public void messageArrived(String topic, MqttMessage msg) throws Exception
			{
				try
				{
					processMessage(topic,msg);
				}
				catch(Exception e)
				{
					L.log(Level.WARNING,"Error when processing message "+msg+" for "+topic,e);
				}
			}
			@Override
			public void deliveryComplete(IMqttDeliveryToken token)
			{
				/* Intentionally ignored */
			}
			@Override
			public void connectionLost(Throwable t)
			{
				L.log(Level.WARNING,"Connection to MQTT broker lost",t);
				queueConnect();
			}
		});
		doConnect();
		Main.t.schedule(new StateChecker(),30*1000,30*1000);
	}

	/**
	 * Publish a plain message (not JSON encoded)
	 * @param name
	 * @param val
	 * @param retain
	 */
	static public void doPublish(String name, Object val,boolean retain)
	{
		String valstr=convertValue(val);
		MqttMessage msg=new MqttMessage(valstr.getBytes(StandardCharsets.UTF_8));
		msg.setQos(0);
		msg.setRetained(retain);
		try
		{
			instance.mqttc.publish(name, msg);
			instance.L.info("Published "+valstr+" to "+name);
		}
		catch(MqttException e)
		{
			instance.L.log(Level.WARNING,"Error when publishing message",e);
		}
	}

	/*
	static public void doPublishJSON(String name, Object val,boolean retain)
	{
		String txtmsg=new JsonObject().add("val",val).toString();
		MqttMessage msg=new MqttMessage(txtmsg.getBytes(StandardCharsets.UTF_8));
		msg.setQos(0);
		msg.setRetained(retain);
		try
		{
			instance.mqttc.publish(name, msg);
			instance.L.info("Published "+txtmsg+" to "+name);
		}
		catch(MqttException e)
		{
			instance.L.log(Level.WARNING,"Error when publishing message",e);
		}
	}
	*/

	/**
	 *
	 * If the script engine used is Javascript, we end up with all numbers being Doubles.
	 * We really only want to produce a decimal point on the MQTT bus if it's really necessary,
	 * though.
	 *
	 * @param val
	 * @return
	 */
	private static final DecimalFormat df_mqtt_bus=new DecimalFormat("0.########",new DecimalFormatSymbols(Locale.US));
	public static String convertValue(Object val)
	{
		if(val instanceof Number)
		{
			synchronized(df_mqtt_bus)
			{
				return df_mqtt_bus.format(val);
			}
		}
		else if(val instanceof Boolean)
		{
			// Convert Boolean into 0/1
			if(((Boolean)val).booleanValue())
				return "1";
			else
				return "0";
		}
		return val.toString();
	}

}

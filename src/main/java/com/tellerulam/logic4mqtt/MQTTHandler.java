package com.tellerulam.logic4mqtt;

import java.nio.charset.*;
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

	void processMessage(String topic,MqttMessage msg)
	{
		JsonObject data=JsonObject.readFrom(new String(msg.getPayload(),Charset.forName("UTF-8")));
		L.info("Received "+msg+" to "+topic);
		JsonValue val=data.get("val");
		Object transformedVal;
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
		copts.setWill(topicPrefix+"connected", "{ \"val\": false, \"ack\": true }".getBytes(), 1, true);
		copts.setCleanSession(true);
		try
		{
			mqttc.connect(copts);
			mqttc.publish(topicPrefix+"connected", "{ \"val\": true, \"ack\": true }".getBytes(), 1, true);
			L.info("Successfully connected to broker, subscribing to #");
			try
			{
				mqttc.subscribe("x",0);
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

	static public void doPublish(String name, String val, boolean retain,boolean ack)
	{
		String txtmsg=new JsonObject().add("val",val).add("ack",ack).toString();
		MqttMessage msg=new MqttMessage(txtmsg.getBytes(Charset.forName("UTF-8")));
		// Default QoS is 1, which is what we want
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


}

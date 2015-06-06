/*
 * This class handles the cache of topics and a history of the last known values
 */

package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

public class TopicCache
{
	static final Map<String,TopicCache> topics=new HashMap<>();

	/**
	 * Replaces the // notation into /status/ and /set/ respectivly
	 * @param inputTopic
	 * @return new topic, with possible // replaced
	 */

	public static String convertSetTopic(String inputTopic)
	{
		return convertTopic(inputTopic,"set");
	}

	public static String convertStatusTopic(String inputTopic)
	{
		return convertTopic(inputTopic,"status");
	}

	public static String convertGetTopic(String inputTopic)
	{
		return convertTopic(inputTopic,"get");
	}

	private static String convertTopic(String inputTopic,String replacement)
	{
		if(inputTopic.startsWith("$"))
		{
			return MQTTHandler.getTopicPrefix()+"status/"+inputTopic.substring(1);
		}

		int slashIx=inputTopic.indexOf('/');
		if(slashIx<0)
			return inputTopic;
		if(inputTopic.length()==slashIx+1)
			return inputTopic;
		if(inputTopic.charAt(slashIx+1)=='/')
		{
			// Found // notation
			return inputTopic.substring(0,slashIx+1)+replacement+inputTopic.substring(slashIx+1);
		}
		return inputTopic;
	}

	private static final Pattern replaceStatusPattern=Pattern.compile("[^/]*/status/.*");

	/**
	 * Replaces a "/status/" in a topic with "//"
	 *
	 * @param topic
	 * @return
	 */
	public static String removeStatusFunction(String topic)
	{
		// Replace /status/ in topic with //
		Matcher m=replaceStatusPattern.matcher(topic);
		if(m.matches())
			return topic.replaceFirst("/status/","//");
		return topic;
	}

	static TopicCache storeTopic(String topic,Object newValue)
	{
		synchronized(topics)
		{
			TopicCache t=topics.get(topic);
			if(t==null)
			{
				t=new TopicCache(topic);
				topics.put(t.topic,t);
			}
			t.storeValue(newValue);
			return t;
		}
	}

	public static TopicValue getTopicValue(String topic,int which)
	{
		synchronized(topics)
		{
			TopicCache t=topics.get(topic);
			if(t!=null && which<t.values.length)
				return t.values[which];
		}
		return null;
	}

	public static Map<String, Object> getTopicValues(String topicPattern)
	{
		Map<String,Object> values=new HashMap<>();
		Pattern p=Pattern.compile(convertStatusTopic(topicPattern));
		synchronized(topics)
		{
			for(TopicCache t:topics.values())
				if(p.matcher(t.topic).matches())
					values.put(t.topic,t.values[0].value);
		}
		return values;
	}

	public Object getValue()
	{
		return changedValues[0].value;
	}
	public Object getPreviousValue()
	{
		if(changedValues[1]!=null)
			return changedValues[1].value;
		return null;
	}
	public Date getPreviousTimestamp()
	{
		if(changedValues[1]!=null)
			return changedValues[1].ts;
		return null;
	}
	/* Whether the last store was a refresh of the previus value */
	public boolean wasRefreshed()
	{
		return lastStoreWasRefresh;
	}
	private void storeValue(Object newValue)
	{
		System.arraycopy(values,0,values,1,values.length-1);
		values[0]=new TopicValue(newValue,new Date());
		if(changedValues[0]!=null && newValue.equals(changedValues[0].value))
		{
			changedValues[0].markAsRefreshed();
			lastStoreWasRefresh=true;
			return;
		}
		System.arraycopy(changedValues,0,changedValues,1,changedValues.length-1);
		changedValues[0]=new TopicValue(newValue,new Date());
		lastStoreWasRefresh=false;
	}

	private TopicCache(String topic)
	{
		this.topic=topic;
	}

	public final String topic;

	static public class TopicValue
	{
		private TopicValue(Object value, Date ts)
		{
			this.value=value;
			this.ts=ts;
			this.lastRefresh=ts;
		}
		public final Object value;
		public final Date ts;
		public Date lastRefresh;
		void markAsRefreshed()
		{
			lastRefresh=new Date();
		}
	}
	private final TopicValue values[]=new TopicValue[10];
	private final TopicValue changedValues[]=new TopicValue[10];
	private boolean lastStoreWasRefresh;

	static {
		Main.t.schedule(new TimerTask(){
			final Logger L=Logger.getLogger(TopicCache.class.getName());
			@Override
			public void run()
			{
				L.info("TopicCache contains "+topics.size()+" entries");
			}
		},5000,5*60*1000);
	}
}

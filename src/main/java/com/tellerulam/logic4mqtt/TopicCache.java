/*
 * This class handles the cache of topics and a history of the last known values
 */

package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.logging.*;

public class TopicCache
{
	static final Map<String,TopicCache> topics=new HashMap<>();

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

	public Object getValue()
	{
		return values[0].value;
	}
	public Object getPreviousValue()
	{
		if(values[1]!=null)
			return values[1].value;
		return null;
	}
	public Date getPreviousTimestamp()
	{
		if(values[1]!=null)
			return values[1].ts;
		return null;
	}

	private void storeValue(Object newValue)
	{
		if(values[0]!=null && newValue.equals(values[0].value))
		{
			values[0].markAsRefreshed();
			return;
		}
		System.arraycopy(values,0,values,1,values.length-1);
		values[0]=new TopicValue(newValue,new Date());
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
	final TopicValue values[]=new TopicValue[10];

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

package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class EventHandler
{
	static final Map<Integer,EventHandler> handlers=new HashMap<>();
	static int idCounter;

	static public boolean removeByID(int id)
	{
		synchronized(handlers)
		{
			return handlers.remove(Integer.valueOf(id))!=null;
		}
	}

	static private Number convertToNumberIfPossible(Object possibleNumber)
	{
		if(possibleNumber instanceof Number)
			return (Number)possibleNumber;
		try
		{
			return new Double(possibleNumber.toString());
		}
		catch(NumberFormatException e)
		{
			/* Not convertable */
			return null;
		}
	}

	static public int createNewHandler(String topicPattern, Object[] destvalues, boolean changeOnly, EventCallbackInterface callback)
	{
		// Replace any possible destvalue with a Number instance, if possible
		if(destvalues!=null)
		{
			for(int i=0;i<destvalues.length;i++)
			{
				Number n=convertToNumberIfPossible(destvalues[i]);
				if(n!=null)
					destvalues[i]=n;
			}
		}
		synchronized(handlers)
		{
			EventHandler handler=new EventHandler(++idCounter,topicPattern,destvalues,changeOnly,callback);
			handlers.put(Integer.valueOf(handler.id),handler);
			if(L.isLoggable(Level.INFO))
				L.info("Created new Event handler "+handler);
			return handler.id;
		}
	}

	static void dispatchEvent(String topic,TopicCache t)
	{
		Object value=t.getValue();
		synchronized(handlers)
		{
			for(EventHandler h:handlers.values())
			{
				if(h.handles(topic) && h.hasDestValue(value))
				{
					if(!h.changeOnly || !(value.equals(t.getPreviousValue())))
						h.queueExecution(topic, value, t.getPreviousValue(), t.getPreviousTimestamp());
				}
			}
		}
	}

	private boolean hasDestValue(Object value)
	{
		if(destvalues==null)
			return true;
		Number valueAsNumber=convertToNumberIfPossible(value);
		String valueAsString=null;
		L.info("value is "+value+" "+value.getClass());
		for(Object dv:destvalues)
		{
			L.info("dv is "+dv+" "+dv.getClass());
			if(valueAsNumber!=null && (dv instanceof Number))
			{
				// Numerical comparision
				if(valueAsNumber.doubleValue()==((Number)dv).doubleValue())
					return true;
			}
			else
			{
				// Textual comparision
				if(valueAsString==null)
					valueAsString=value.toString();
				if(valueAsString.equals(dv.toString()))
					return true;
			}
		}
		return false;
	}

	private boolean handles(String topic)
	{
		return topicPattern.equals(topic);
	}

	private void queueExecution(String topic,Object value,Object previousValue,Date previousTimestamp)
	{
		eventExecutor.execute(new EventRunner(topic,value,previousValue,previousTimestamp));
	}

	private class EventRunner implements Runnable
	{
		final String topic;
		final Object value;
		final Object previousValue;
		final Date previousTimestamp;
		EventRunner(String topic, Object value, Object previousValue, Date previousTimestamp)
		{
			this.topic=topic;
			this.value=value;
			this.previousValue=previousValue;
			this.previousTimestamp=previousTimestamp;
		}
		@Override
		public void run()
		{
			try
			{
				callback.run(topic,value,previousValue,previousTimestamp);
			}
			catch(Exception e)
			{
				L.log(Level.WARNING, "Error when executing event callback for "+topic+"="+value,e);
			}
		}
	}

	static final Executor eventExecutor=Executors.newCachedThreadPool();

	static final Logger L=Logger.getLogger(EventHandler.class.getName());

	private EventHandler(int id, String topicPattern, Object[] destvalues, boolean changeOnly, EventCallbackInterface callback)
	{
		this.id=id;
		this.topicPattern=topicPattern;
		this.destvalues=destvalues;
		this.changeOnly=changeOnly;
		this.callback=callback;
	}

	@Override
	public String toString()
	{
		return "["+id+":"+topicPattern+"="+(destvalues==null?"*":Arrays.asList(destvalues))+"/"+(changeOnly?"CH":"UP")+"]";
	}

	private final int id;
	private final String topicPattern;
	private final Object destvalues[];
	private final boolean changeOnly;
	private final EventCallbackInterface callback;
}

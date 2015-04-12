package com.tellerulam.logic4mqtt;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

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

	private static class AutoExpirer implements TimerCallbackInterface
	{
		private final EventHandler eh;

		public AutoExpirer(EventHandler handler)
		{
			this.eh=handler;
		}

		@Override
		public String toString()
		{
			return "{AEx:"+eh+"}";
		}

		@Override
		public void run(Object userdata)
		{
			synchronized(handlers)
			{
				handlers.remove(Integer.valueOf(eh.id));
			}
		}
	}

	static public int createNewHandler(
		String topicPattern,
		Object[] destvalues,
		boolean changeOnly,
		EventCallbackInterface callback,
		boolean oneShot,
		String expires
	)
	{
		// Replace any possible destvalue with a Number instance, if possible
		if(destvalues!=null)
		{
			for(int i=0;i<destvalues.length;i++)
			{
				Number n=ScriptEngineTools.convertToNumberIfPossible(destvalues[i]);
				if(n!=null)
					destvalues[i]=n;
			}
		}
		synchronized(handlers)
		{
			EventHandler handler=new EventHandler(++idCounter,topicPattern,destvalues,changeOnly,callback,oneShot,expires);
			handlers.put(Integer.valueOf(handler.id),handler);
			/* Queue an timer to expire the event handler, if "expires" was set */
			if(expires!=null)
				LogicTimer.addTimer("_EVENT_EXPIRER_"+topicPattern,expires,new AutoExpirer(handler),null);
			if(L.isLoggable(Level.INFO))
				L.info("Created new Event handler "+handler);
			return handler.id;
		}
	}

	public static Collection<EventHandler> getAllHandlers()
	{
		synchronized(handlers)
		{
			return new ArrayList<>(handlers.values());
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
					if(!h.changeOnly || t.wasRefreshed())
						h.queueExecution(topic, value, t.getPreviousValue(), t.getPreviousTimestamp());
				}
			}
		}
	}

	private boolean hasDestValue(Object value)
	{
		if(destvalues==null)
			return true;
		Number valueAsNumber=ScriptEngineTools.convertToNumberIfPossible(value);
		String valueAsString=null;
		//L.info("value is "+value+" "+value.getClass());
		for(Object dv:destvalues)
		{
			//L.info("dv is "+dv+" "+dv.getClass());
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
		Matcher m=topicPattern.matcher(topic);
		return m.matches();
	}

	private void queueExecution(String topic,Object value,Object previousValue,Date previousTimestamp)
	{
		topic=TopicCache.removeStatusFunction(topic);
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
			if(EventHandler.this.oneShot)
				EventHandler.removeByID(EventHandler.this.id);
		}
	}

	/* Currently, Nashorn is singlethreaded */
	static final Executor eventExecutor=Executors.newFixedThreadPool(1);

	static final Logger L=Logger.getLogger(EventHandler.class.getName());

	private EventHandler(int id, String topicPattern, Object[] destvalues, boolean changeOnly, EventCallbackInterface callback,boolean oneShot,String expires)
	{
		this.id=id;
		this.topicPattern=Pattern.compile(topicPattern);
		this.destvalues=destvalues;
		this.changeOnly=changeOnly;
		this.callback=callback;
		this.oneShot=oneShot;
		this.expires=expires;
	}

	@Override
	public String toString()
	{
		return "{"+id+":"+topicPattern+"="+(destvalues==null?"*":Arrays.asList(destvalues))+"/"+(changeOnly?"CH":"UP")+(oneShot?"/OS":"")+(expires!=null?expires:"")+"}";
	}

	public String getCmdlineSummary()
	{
		StringBuilder s=new StringBuilder();
		s.append(id);
		s.append('\t');
		s.append(topicPattern);
		s.append('\t');
		if(destvalues==null)
			s.append('*');
		else
			s.append(Arrays.asList(destvalues));
		s.append('\t');
		s.append(changeOnly?"CH":"UP");
		if(oneShot)
			s.append("/OS");
		s.append('\t');
		if(expires==null)
			s.append("CONT");
		else
			s.append(expires);
		s.append('\t');
		s.append(callback);

		return s.toString();
	}

	private final int id;
	private final Pattern topicPattern;
	private final Object destvalues[];
	private final boolean changeOnly, oneShot;
	private final EventCallbackInterface callback;
	private final String expires;


}

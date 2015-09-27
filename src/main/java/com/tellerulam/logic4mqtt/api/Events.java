/**
 * This object contains the interface from scripts to the Event handling system.
 *
 * Event callbacks receive the following arguments:
 *
 * 	public void run(String topic,Object newValue,Object previousValue,Date previousTimestamp,Object fullValue);
 *  <ul>
 *   <li>topic - the topic on which the event occured (with the function replaced by "//")
 *   <li>newValue - the new simple value of the topic (the "val" field in case of a received JSON encoded message)
 *   <li>previousValue - the previous simple value of the topic
 *   <li>previousTimestamp - the time when the previous value was set
 *   <li>fullValue - the complete MQTT message as received and JSON-parsed, with all meta data
 *  </ul>
 *
 *
 * <b>Type conversion</b>: If you use the Nashorn (Javascript) engine and the simple value of a topic is
 * an array, you must manually convert the received object into a Javascript array using the Java.from()
 * Nashorn special method. On the other hand, it is <u>not</u> necessary to do this when passing
 * Javascript arrays to the Java utility functions.<b>
 *
 * Example:
 *
 *   var testArray=["Hello","World"];
 *   Events.setValue("$TEST",testArray);
 *   var res=Events.getValue("$TEST");
 *   // res now holds an Object, not an Array. Let's convert it:
 *   var resArray=Java.from(res);
 *   // resArray is now an array
 *
 */

package com.tellerulam.logic4mqtt.api;

import java.util.*;

import com.tellerulam.logic4mqtt.*;
import com.tellerulam.logic4mqtt.TopicCache.TopicValue;

/**
 * This class manages event handlers.
 *
 */
public class Events
{
	static final Events instance=new Events();

	private Events()
	{
		/* Keep private */
	}

	/**
	 *
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes to the specified value.
	 *
	 * @param topicPattern a RegEx matching topics
	 * @param value value which will trigger the callback
	 * @param callback function to call
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChangeTo(String topicPattern,Object value,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, new Object[]{value}, true, callback, false, null, false);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes to any of the the specified values.
	 *
	 * @param topicPattern a RegEx matching topics
	 * @param values values which will trigger the callback
	 * @param callback function to call
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChangeTo(String topicPattern,Object values[],EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, values, true, callback, false, null, false);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes from the previous value.
	 *
	 * @param topicPattern a RegEx matching topics
	 * @param callback function to call
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChange(String topicPattern,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, null, true, callback, false, null, false);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic is being published to, regardless of the value.
	 *
	 * @param topicPattern a RegEx matching topics
	 * @param callback function to call
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onUpdate(String topicPattern,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, null, false, callback, false, null, false);
	}

	/**
	 * Add an Event Handler using a description object (e.g. JSON object from Javascript)
	 *
	 * @param topicPattern a RegEx matching topics
	 * @param callback function to call
	 * @param params Map with parameters:<ul>
	 *   <li>values - value or array of values which trigger the event
	 *   <li>change - bool: whether to run only on changes of the value
	 *   <li>oneshot - bool: whether to remove the event handler after it run once
	 *   <li>initial - bool: whether to check calling the callback once with the initial value when added
	 *   <li>expires - string: timespec when handler expires
	 *  </ul>
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	@SuppressWarnings("unchecked")
	public int add(String topicPattern,EventCallbackInterface callback,Map<String,Object> params)
	{
		Object values=params.get("values");
		Object vals[];
		if(values!=null)
		{
			if(values instanceof Map)
			{
				vals=((Map<String,Object>)values).values().toArray();
			}
			else
				vals=new Object[]{values};
		}
		else
			vals=null;

		boolean oneShot=ScriptEngineTools.interpretAsBoolean(params.get("oneshot"));
		boolean change=ScriptEngineTools.interpretAsBoolean(params.get("change"));
		boolean initial=ScriptEngineTools.interpretAsBoolean(params.get("initial"));

		String expires=(String)params.get("expires");

		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		int eid=EventHandler.createNewHandler(topicPattern, vals, change, callback, oneShot, expires, initial);

		return eid;
	}

	/**
	 * Removes an event handler which was added with one of the on...() methods.
	 *
	 * @param id of the handler to remove
	 * @return boolean whether an event handler with the given id was found and removed
	 */
	public boolean remove(int id)
	{
		return EventHandler.removeByID(id);
	}

	/**
	 *
	 * Publish an update to the specified topic with the given value. It will not be retained.
	 *
	 * @param topic to publish on
	 * @param value to publish
	 */
	public void setValue(String topic,Object value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, value, false);
	}

	public void setValue(String topic,Map<String,Object> value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, ScriptEngineTools.encodeAsJSON(value), false);
	}

	/*
	public void setValue(String topic,Object values[])
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, ScriptEngineTools.encodeAsJSON(values), false);
	}
	*/

	/**
	 * Publish an update to the specified topic with the given value. It will be retained.
	 *
	 * @param topic to publish to
	 * @param value to publish
	 */
	public void storeValue(String topic,Object value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, value, true);
	}

	public void storeValue(String topic,Map<String,Object> value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, ScriptEngineTools.encodeAsJSON(value), true);
	}

	public void storeValue(String topic,List<?> values)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, ScriptEngineTools.encodeAsJSON(values), true);
	}

	private static class QueuedSet implements TimerCallbackInterface
	{
		final String setTopic;
		final Object value;
		final boolean retain;
		@Override
		public void run(Object userdata)
		{
			MQTTHandler.doPublish(setTopic, value, retain);
		}
		@Override
		public String toString()
		{
			return "{QuSet:="+value+(retain?"/R":""+"}");
		}
		protected QueuedSet(String setTopic, Object value, boolean retain)
		{
			this.setTopic = setTopic;
			this.value = value;
			this.retain = retain;
		}
	}

	private void internalQueueSet(String timespec,String topic,final Object value,final boolean retain)
	{
		final String setTopic=TopicCache.convertSetTopic(topic);
		LogicTimer.addTimer("_SET_"+setTopic, timespec, new QueuedSet(setTopic,value,retain),null);
	}
	/**
	 * Queue an update to the specified topic with the given value at timespec.
	 *
	 * This is effectivly a shortcut for adding a timer which sets the value,
	 * with a symbolic name of _SET_topic
	 *
	 * @param timespec the timer spec
	 * @param topic the topic to publish to
	 * @param value the value to publish
	 *
	 * @see Timers
	 */
	public void queueValue(String timespec,String topic,Object value)
	{
		internalQueueSet(timespec,topic,value,false);
	}

	public void queueValue(String timespec,String topic,Map<String,Object> value)
	{
		internalQueueSet(timespec,topic,ScriptEngineTools.encodeAsJSON(value),false);
	}


	/**
	 * Queue an update to the specified topic with the given value at timespec,
	 * with the retain flag set to true.
	 *
	 * This is effectivly a shortcut for adding a timer which sets the value,
	 * with a symbolic name of _SET_topic
	 *
	 * @param timespec the timer spec
	 * @param topic the topic to publish to
	 * @param value the value to publish
	 *
	 * @see Timers
	 */
	public void queueStore(String timespec,String topic,Object value)
	{
		internalQueueSet(timespec,topic,value,true);
	}

	public void queueStore(String timespec,String topic,Map<String,Object> value)
	{
		internalQueueSet(timespec,topic,ScriptEngineTools.encodeAsJSON(value),true);
	}

	/**
	 * Clears possibly queued value sets for the given topic
	 *
	 * @param topic the topic to clear the queue for
	 * @return number of removed queued set
	 */
	public int clearQueue(String topic)
	{
		return LogicTimer.remTimer("_SET_"+TopicCache.convertSetTopic(topic));
	}

	/**
	 *
	 * Get a cached last value of a topic, or null if the topic or the specified
	 * generation is not known.
	 *
	 * @param topic
	 * @param generation (0 = current, 1 = previous, ...)
	 * @return the value, or null
	 */
	public Object getValue(String topic,int generation)
	{
		topic=TopicCache.convertStatusTopic(topic);
		TopicValue tv=TopicCache.getTopicValue(topic, generation);
		if(tv!=null)
			return tv.value;
		return null;
	}

	/*
	public Collection<?> getValueAsArray(String topic,int generation)
	{
		topic=TopicCache.convertStatusTopic(topic);
		TopicValue tv=TopicCache.getTopicValue(topic, generation);
		if(tv!=null)
		{
			if(tv.value instanceof List<?>)
				return ((List<?>)tv.value);
			else if(tv.value instanceof Map<?,?>)
				return ((Map<?,?>)tv.value).values();
			else
				return Collections.singleton(tv.value);

		}
		return null;
	}*/

	/**
	 * An array of instances of his helper class is returned by {@link Events#getValues(String)}
	 */
	static public class GetValuesEntry
	{
		GetValuesEntry(String topic, Object value)
		{
			this.topic=topic;
			this.value=value;
		}
		public final String topic;
		public final Object value;
	}

	/**
	 *
	 * Get the cached values of all topics matching "topicPattern"
	 *
	 * @param topicPattern
	 * @return an array of objects with members "topic" and "value"
	 */
	public GetValuesEntry[] getValues(String topicPattern)
	{
		Map<String,Object> values=TopicCache.getTopicValues(topicPattern);
		GetValuesEntry le[]=new GetValuesEntry[values.size()];
		int lex=0;
		for(Map.Entry<String,Object> me:values.entrySet())
		{
			GetValuesEntry te=new GetValuesEntry(me.getKey(),me.getValue());
			le[lex++]=te;
		}
		return le;
	}

	/**
	 * Get the current last value of a topic, or null if the topic is not known.
	 * @param topic
	 * @return the value, or null
	 */
	public Object getValue(String topic)
	{
		return getValue(topic,0);
	}

	/**
	 *
	 * Get the timestamp of the value of a topic, or null if the topic or the specified
	 * generation is not known.
	 *
	 * @param topic
	 * @param generation (0 = current, 1 = previous, ...)
	 * @return the timestamp, or null
	 */
	public Date getTimestamp(String topic,int generation)
	{
		topic=TopicCache.convertStatusTopic(topic);
		TopicValue tv=TopicCache.getTopicValue(topic, generation);
		if(tv!=null)
			return tv.ts;
		return null;
	}

	/**
	 * Get the timestamp of the value of a topic, or null if the topic is not known.
	 * @param topic
	 * @return the timestamp, or null
	 */
	public Date getTimestamp(String topic)
	{
		return getTimestamp(topic,0);
	}

	/**
	 * Request a value via a MQTT /get/ publish
	 * @param topic
	 */
	public void requestValue(String topic)
	{
		topic=TopicCache.convertGetTopic(topic);
		MQTTHandler.doPublish(topic, "?", false);
	}

	public int test(String srcTopic,String destTopic,Map<String,Object> params)
	{
		Object v=params.get("mapper");
		System.out.println(v);
		System.out.println(v.getClass());
		return 0;
	}
	/**
	 *
	 * Link one topic to another. Only the "val" part of the source topic will be set
	 * on the destination topic.
	 *
	 * @param srcTopic Source topic
	 * @param destTopic Destination topic
	 * @param params Map with parameters:<ul>
	 *   <li>change - bool: whether to link only on changes of the source value
	 *   <li>oneshot - bool: whether to remove the event handler after it run once
	 *   <li>initial - bool: whether to link once with the initial value when added
	 *   <li>retain - bool: whether the setting occurs with the retain flag set
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int linkValue(String srcTopic,String destTopic,Map<String,Object> params)
	{
		final boolean oneShot=ScriptEngineTools.interpretAsBoolean(params.get("oneshot"));
		final boolean change=ScriptEngineTools.interpretAsBoolean(params.get("change"));
		final boolean initial=ScriptEngineTools.interpretAsBoolean(params.get("initial"));
		final boolean retain=ScriptEngineTools.interpretAsBoolean(params.get("retain"));

		final String expires=(String)params.get("expires");

		srcTopic=TopicCache.convertStatusTopic(srcTopic);
		final String finalDestTopic=TopicCache.convertSetTopic(destTopic);

		EventCallbackInterface linker=new EventCallbackInterface()
		{
			@Override
			public void run(String topic, Object newValue, Object previousValue, Date previousTimestamp, Object fullValue)
			{
				MQTTHandler.doPublish(finalDestTopic, newValue, retain);
			}
		};

		int eid=EventHandler.createNewHandler(srcTopic, null, change, linker, oneShot, expires, initial);
		return eid;
	}

	/**
	 *
	 * Link one topic to another. Each update on the source topic will be set on the
	 * destination topic. No initial setting occurs.
	 *
	 * @param srcTopic
	 * @param destTopic
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int linkValue(String srcTopic,String destTopic)
	{
		return linkValue(srcTopic,destTopic,Collections.emptyMap());
	}

}

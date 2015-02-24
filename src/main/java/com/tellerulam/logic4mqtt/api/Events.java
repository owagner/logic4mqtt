/*
 * This object contains the interface from scripts to the Event handling system
 */

package com.tellerulam.logic4mqtt.api;

import java.util.*;

import com.tellerulam.logic4mqtt.*;
import com.tellerulam.logic4mqtt.TopicCache.TopicValue;

public class Events
{
	private static final Events instance=new Events();

	public static Events getInstance()
	{
		return instance;
	}

	/**
	 *
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes to the specified value.
	 *
	 * @param topicPattern
	 * @param value
	 * @param callback
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChangeTo(String topicPattern,Object value,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, new Object[]{value}, true, callback);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes to any of the the specified values.
	 *
	 * @param topicPattern
	 * @param values
	 * @param callback
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChangeTo(String topicPattern,Object values[],EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, values, true, callback);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic changes from the previous value.
	 *
	 * @param topicPattern
	 * @param callback
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onChange(String topicPattern,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, null, true, callback);
	}
	/**
	 * Add an Event Handler on the specified topic pattern. It is triggered when the
	 * topic is being published, regardless of the value.
	 *
	 * @param topicPattern
	 * @param callback
	 * @return id of the new handler (to be used with e.g. remove)
	 */
	public int onUpdate(String topicPattern,EventCallbackInterface callback)
	{
		topicPattern=TopicCache.convertStatusTopic(topicPattern);
		return EventHandler.createNewHandler(topicPattern, null, false, callback);
	}
	/**
	 * Removes an event handler which was added with one of the on...() methods.
	 *
	 * @param id
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
	 * @param topic
	 * @param value
	 */
	public void setValue(String topic,Object value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, value, false);
	}

	/**
	 * Publish an update to the specified topic with the given value. It will be retained.
	 *
	 * @param topic
	 * @param value
	 */
	public void storeValue(String topic,Object value)
	{
		topic=TopicCache.convertSetTopic(topic);
		MQTTHandler.doPublish(topic, value, true);
	}

	public void internalQueueSet(String timespec,String topic,final Object value,final boolean retain)
	{
		final String setTopic=TopicCache.convertSetTopic(topic);
		LogicTimer.addTimer("_SET_"+setTopic, timespec, new TimerCallbackInterface(){
			@Override
			public void run(Object userdata)
			{
				MQTTHandler.doPublish(setTopic, value, retain);
			}
		},null);
	}
	/**
	 * Queue an update to the specified topic with the given value at timespec.
	 *
	 * This is effectivly a shortcut for adding a timer which sets the value,
	 * with a symbolic name of _SET_<topic>
	 *
	 * @param timespec the timer spec
	 * @param topic the topic to publish to
	 * @param value the value to publish
	 *
	 * @see Timers
	 */
	public void queueValue(String timespec,String topic,final Object value)
	{
		internalQueueSet(timespec,topic,value,false);
	}

	/**
	 * Queue an update to the specified topic with the given value at timespec,
	 * with the retain flag set to true.
	 *
	 * This is effectivly a shortcut for adding a timer which sets the value,
	 * with a symbolic name of _SET_<topic>
	 *
	 * @param timespec the timer spec
	 * @param topic the topic to publish to
	 * @param value the value to publish
	 *
	 * @see Timers
	 */
	public void queueStore(String timespec,String topic,final Object value)
	{
		internalQueueSet(timespec,topic,value,true);
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
}

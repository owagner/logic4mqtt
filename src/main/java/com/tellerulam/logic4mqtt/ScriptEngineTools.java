package com.tellerulam.logic4mqtt;

import java.lang.reflect.*;
import java.util.*;

import javax.script.*;

import com.eclipsesource.json.*;

public class ScriptEngineTools
{
	static public boolean interpretAsBoolean(Object possibleBoolean)
	{
		if(possibleBoolean==null)
			return false;

		if(possibleBoolean instanceof Boolean)
			return ((Boolean)possibleBoolean).booleanValue();
		Number bn=convertToNumberIfPossible(possibleBoolean);
		if(bn!=null)
			return bn.intValue()!=0;
		return false;
	}

	static public Number convertToNumberIfPossible(Object possibleNumber)
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

	/*
	 * Special hack for Nashorn: Unlike Rhino, passed Javascript arrays will not be wrapped
	 * in an object wrapped with a List implementation. We therefore do some reflection
	 * magic to call isArray() and values() on the ScriptObjectMirror
	 */
	private static Class<?> scriptObjectMirrorClass;
	private static Method scriptObjectMirrorClass_isArray,scriptObjectMirrorClass_values;
	static {
		try
		{
			scriptObjectMirrorClass=Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
			scriptObjectMirrorClass_isArray=scriptObjectMirrorClass.getMethod("isArray");
			scriptObjectMirrorClass_values=scriptObjectMirrorClass.getMethod("values");
		}
		catch(ClassNotFoundException | NoSuchMethodException | SecurityException e)
		{
			/* Ignore, we're probably simply not running Nashorn */
			scriptObjectMirrorClass=null;
		}
	}

	@SuppressWarnings("unchecked")
	private static JsonObject mapToJSO(Map<String,Object> map)
	{
		JsonObject jso=new JsonObject();
		for(Map.Entry<String,Object> me:map.entrySet())
		{
			Object v=me.getValue();
			if(scriptObjectMirrorClass!=null && (v instanceof Bindings))
			{
				// Check whether we this is a Nashorn Array
				if(scriptObjectMirrorClass.isAssignableFrom(v.getClass()))
				{
					try
					{
						if(scriptObjectMirrorClass_isArray.invoke(v).equals(Boolean.TRUE))
							v=scriptObjectMirrorClass_values.invoke(v);
					}
					catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
					{
						/* Conversion failed, continue as normal */
					}
				}
			}
			if(v instanceof Bindings)
			{
				jso.add(me.getKey(),mapToJSO((Bindings)me.getValue()));
			}
			else if(v instanceof Map)
			{
				jso.add(me.getKey(),mapToJSO((Map<String,Object>)me.getValue()));
			}
			else if(v instanceof Collection)
			{
				JsonArray arr=new JsonArray();
				Collection<Object> col=(Collection<Object>)v;
				for(Object colv:col)
					arr.add(objectToJsonValue(colv));
				jso.add(me.getKey(),arr);
			}
			else
				jso.add(me.getKey(),objectToJsonValue(v));
		}
		return jso;
	}

	@SuppressWarnings("unchecked")
	private static JsonValue objectToJsonValue(Object v)
	{
		if(scriptObjectMirrorClass!=null && (v instanceof Bindings))
		{
			// Check whether we this is a Nashorn Array
			// and convert it into a Java array, then
			if(scriptObjectMirrorClass.isAssignableFrom(v.getClass()))
			{
				try
				{
					if(scriptObjectMirrorClass_isArray.invoke(v).equals(Boolean.TRUE))
						v=scriptObjectMirrorClass_values.invoke(v);
				}
				catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
				{
					/* Conversion failed, continue as normal */
				}
			}
		}

		if(v==null)
			return JsonValue.NULL;
		if(v instanceof Integer)
			return JsonValue.valueOf(((Integer)v).intValue());
		else if(v instanceof Number)
			return JsonValue.valueOf(((Number)v).doubleValue());
		else if(v instanceof Boolean)
			return ((Boolean)v).booleanValue()?JsonValue.TRUE:JsonValue.FALSE;
		else if(v instanceof Map) // Will also cover Bindings
		{
			JsonObject jso=new JsonObject();
			for(Map.Entry<String,? extends Object> me:((Map<String,? extends Object>)v).entrySet())
			{
				jso.add(me.getKey(),objectToJsonValue(me.getValue()));
			}
			return jso;
		}
		else if(v instanceof Collection<?>)
		{
			JsonArray jsa=new JsonArray();
			Iterator<?> i=((Collection<?>)v).iterator();
			while(i.hasNext())
				jsa.add(objectToJsonValue(i.next()));
			return jsa;
		}
		else if(v instanceof Object[])
		{
			Object vo[]=(Object[])v;
			JsonArray jsa=new JsonArray();
			for(Object o:vo)
				jsa.add(objectToJsonValue(o));
			return jsa;
		}
		else
			return JsonValue.valueOf(v.toString());
	}
	static public String encodeAsJSON(Object v)
	{
		JsonValue jso=objectToJsonValue(v);
		return jso.toString();
	}


}

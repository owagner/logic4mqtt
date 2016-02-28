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
			return Json.NULL;
		if(v instanceof Integer)
			return Json.value(((Integer)v).intValue());
		else if(v instanceof Number)
			return Json.value(((Number)v).doubleValue());
		else if(v instanceof Boolean)
			return ((Boolean)v).booleanValue()?Json.TRUE:Json.FALSE;
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
			return Json.value(v.toString());
	}
	static public String encodeAsJSON(Object v)
	{
		JsonValue jso=objectToJsonValue(v);
		return jso.toString();
	}

}

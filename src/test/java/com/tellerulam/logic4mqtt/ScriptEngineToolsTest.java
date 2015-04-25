package com.tellerulam.logic4mqtt;

import static org.junit.Assert.*;

import java.util.*;

import javax.script.*;

import org.junit.*;

public class ScriptEngineToolsTest
{
	public class TestReceiver
	{
		public String dotest(Bindings json)
		{
			return ScriptEngineTools.encodeAsJSON(json);
		}
		public String dotest(Map<String,Object> val)
		{
			return ScriptEngineTools.encodeAsJSON(val);
		}
		@SuppressWarnings("boxing")
		public String dotest(double d)
		{
			if(d == (long) d)
		        return String.format("%d",(long)d);
		    else
		        return String.format("%s",d);		}
	}

	@Test
	public void callbackFormatTest() throws ScriptException
	{
		ScriptEngineManager sem=new ScriptEngineManager();
		ScriptEngine se=sem.getEngineByExtension("js");
		se.put("TestFix",new TestReceiver());

		String testcases[]={
			"23", "23",
			"{\"blurb\":42}", "{\"blurb\": 42 }",
			"{\"blurb\":[19,23]}", "{\"blurb\" : [ 19, 23 ]}",
			"{\"blurb\":{\"quark\":19.1}}", "{\"blurb\" : { \"quark\": 19.1 } }",
		};

		for(int ix=0;ix<testcases.length;ix+=2)
			assertEquals(testcases[ix],se.eval("TestFix.dotest("+testcases[ix+1]+")"));
	}
}

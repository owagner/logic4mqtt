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
		        return String.format("%s",d);
		}
		public String lastStore;
		public void storeJSON(Map<String,Object> val)
		{
			lastStore=ScriptEngineTools.encodeAsJSON(val);
		}
		public void storeJSON(List<?> val)
		{
			lastStore=ScriptEngineTools.encodeAsJSON(val);
		}
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

	@Test
	public void encodeJSONtest() throws ScriptException
	{
		ScriptEngineManager sem=new ScriptEngineManager();
		ScriptEngine se=sem.getEngineByExtension("js");
		TestReceiver tr=new TestReceiver();
		se.put("TestFix",tr);
		/* JS representation, JSON text */
		String testcases[]={
			"[[1,2],[3,4]]", "[[1,2],[3,4]]",
			"[12,13]",	"[12,13]",
			"{dummy:15}", "{\"dummy\":15}",
			"{dummy:[15,16]}", "{\"dummy\":[15,16]}",
			"{dummy:{a:2,b:3}}", "{\"dummy\":{\"a\":2,\"b\":3}}",
			"{dummy:{a:2,b:[3,1,4]}}", "{\"dummy\":{\"a\":2,\"b\":[3,1,4]}}",
			"[{o:1},{o:2}]", "[{\"o\":1},{\"o\":2}]",
		};
		for(int ix=0;ix<testcases.length;ix+=2)
		{
			se.eval("TestFix.storeJSON("+testcases[ix]+")");
			System.out.println(testcases[ix]+" yields "+tr.lastStore);
			assertEquals(testcases[ix+1],tr.lastStore);
		}
	}
}

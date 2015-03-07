package com.tellerulam.logic4mqtt;

import static org.junit.Assert.*;

import org.junit.*;

public class TopicCacheTest
{
	@Test
	public void testConvertTopic()
	{
		assertEquals("test/set/blah",TopicCache.convertSetTopic("test//blah"));
		assertEquals("test/status/blah",TopicCache.convertStatusTopic("test//blah"));

		assertEquals("test/set/blah//blubb",TopicCache.convertSetTopic("test//blah//blubb"));

		assertEquals("test",TopicCache.convertSetTopic("test"));
		assertEquals("test/",TopicCache.convertSetTopic("test/"));
		assertEquals("test/set/",TopicCache.convertSetTopic("test//"));

		assertEquals("logic/status/test",TopicCache.convertSetTopic("$test"));
	}

	@Test
	public void testStatus()
	{
		assertEquals("test//blah",TopicCache.removeStatusFunction("test/status/blah"));
		assertEquals("test/set/blah",TopicCache.removeStatusFunction("test/set/blah"));
		assertEquals("test/gurgh/status/blah",TopicCache.removeStatusFunction("test/gurgh/status/blah"));
	}

}

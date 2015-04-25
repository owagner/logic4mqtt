package com.tellerulam.logic4mqtt;

import static org.junit.Assert.*;

import org.junit.*;

public class MQTTHandlerTest
{
	@Test
	public void testConvertValue()
	{
		final Object testset[]={
			"0", new Double(0),
			"0.1", new Double(0.1),
			"0.11111111", new Double(0.11111111),
			"0.11111111", new Double(0.111111111), // Rounding
			"0.55555556", new Double(0.555555559), // Rounding
		};

		for(int ix=0;ix<testset.length;ix+=2)
			assertEquals(testset[ix].toString(),MQTTHandler.convertValue(testset[ix+1]));
	}

}

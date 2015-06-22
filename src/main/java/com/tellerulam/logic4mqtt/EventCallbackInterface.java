package com.tellerulam.logic4mqtt;

import java.util.*;

public interface EventCallbackInterface
{
	public void run(String topic,Object newValue,Object previousValue,Date previousTimestamp,Object fullValue);
	//public void run(String topic,List<?> newValue,List<?> previousValue,Date previousTimestamp,Object fullValue);
}

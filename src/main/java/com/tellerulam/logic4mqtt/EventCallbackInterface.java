package com.tellerulam.logic4mqtt;

import java.util.*;

public interface EventCallbackInterface
{
	public void run(String topic,Object newValue,Object previousValue,Date previousTimestamp);
}

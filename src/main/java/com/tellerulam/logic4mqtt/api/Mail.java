package com.tellerulam.logic4mqtt.api;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import com.tellerulam.logic4mqtt.*;

/**
 * The Mail class supports sending E-Mail.
 *
 * It utilizies the Java Mail subsystem: https://java.net/projects/javamail/pages/Home
 *
 *
 */
public class Mail
{
	static final Mail instance=new Mail();

	protected Mail()
	{
		/* Keep private */
	}

	private static Logger L=Logger.getLogger(Mail.class.getName());
}

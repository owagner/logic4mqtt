package com.tellerulam.logic4mqtt;

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


}

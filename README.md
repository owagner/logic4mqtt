logic4mqtt
==========

  Written and (C) 2015 Oliver Wagner <owagner@tellerulam.com> 
  
  Provided under the terms of the MIT license.

Overview
--------
logic4mqtt is a logic and scripting engine for SmartHome automation, based around MQTT as a central
message bus.

It uses Java's generalized scripting interface (JSR-223) so scripts can be implemented in any
script language supported by this interface. By default, the JVM ships with a Javascript scripting
engine (Rhino with Java 7, Nashorn with Java 8), but a variety of other interfaces is available
for languages like Groovy, Jython etc.


Dependencies
------------
* Java 1.7 (or higher) SE Runtime Environment: https://www.java.com/
* Eclipse Paho: https://www.eclipse.org/paho/clients/java/ (used for MQTT communication)
* Minimal-JSON: https://github.com/ralfstx/minimal-json (used for JSON creation and parsing)
* natty: http://natty.joestelmach.com/ (used for natural language time parsing)
* sunrisesunsetlib-java: https://github.com/mikereedell/sunrisesunsetlib-java (for sunrise/sunset calculations)
* Quartz Scheduler: http://www.quartz-scheduler.org/ (used for cron-alike timer parsing)

[![Build Status](https://travis-ci.org/mqtt-smarthome/logic4mqtt.svg)](https://travis-ci.org/mqtt-smarthome/logic4mqtt)


Events
------
Everything in logic4mqtt revolves around the core concept of an _Event_


Timers
------
logic4mqtt timers can be specified in one of two ways:

1. cron-alike syntax
2. natural language

### Cron-alike syntax ###

Cron syntax is parsed using the Quartz Scheduler CronExpression handling, which is described in 
detail at http://quartz-scheduler.org/api/2.2.0/org/quartz/CronExpression.html

Two important differences to UNIX-like cron specifications:

- the first field is a 'seconds' field, so the total number of fields is at least 6. UNIX-like crons
often only have minute resolution, and thus only 5 fields.
- it's not possible to specify both Day-Of-Month and Day-Of-Week as "*". Specify the Day-Of-Week 
or Day-Of-Month field as "?" if you don't care for that specific field.

Examples:

    Timers.addTimer("every_5_minutes","0 */5 * * * ?",callback);

### Natural language syntax ###

The natural language syntax utilizies the Natty library http://natty.joestelmach.com/ for parsing time 
specifications, with additional support for specifying sunset/sundown as a reference point.

Examples:

    Timers.addTimer("test1","in 5 minutes",callback);
    Timers.addTimer("test2","10 minutes before sunset",callback);
    Timers.addTimer("test3","1 hour after civil sunrise",callback);
    
Repeating timers are also specified using this syntax:

    Timers.addTimer("test4","every 10 minutes before sunset",callback);

The natty homepage has a test functionality at http://natty.joestelmach.com/try.jsp where you can
try possible specifications. The sunset/sundown keywords are simply replaced with the currently calculated
times before the string is passed to natty for parsing. For example, 

    1 hour after civil sunrise
    
becomes

    1 hour after 08:40
    
before it's passed to natty. The specifications are reparsed everytime the timer is run, to make sure
relative specifications always refer to the current time. This also applies to the sunset/sundown
replacement keywords. The sunset/sundown keywords may optionally be prefixed with the keywords "civil", "nautical", 
"astronomical" or "official", to refer to variants of the sunset/sundown times. The latitude and longitude
of the currently location need to be set with a call to

    SunriseSunset.setLocation(latitude,longitude)
    
prior to use.


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
for languages like Groovy, Jython and others.


Dependencies
------------
* Java 1.7 (or higher) SE Runtime Environment: https://www.java.com/
* Eclipse Paho: https://www.eclipse.org/paho/clients/java/ (used for MQTT communication)
* Minimal-JSON: https://github.com/ralfstx/minimal-json (used for JSON creation and parsing)
* natty: http://natty.joestelmach.com/ (used for natural language time parsing)
* sunrisesunsetlib-java: https://github.com/mikereedell/sunrisesunsetlib-java (for sunrise/sunset calculations)
* Quartz Scheduler: http://www.quartz-scheduler.org/ (used for cron-alike timer parsing)

[![Build Status](https://travis-ci.org/mqtt-smarthome/logic4mqtt.svg)](https://travis-ci.org/mqtt-smarthome/logic4mqtt) Automatically built jars can be downloaded from the release page on GitHub at https://github.com/mqtt-smarthome/logic4mqtt/releases


Topic notation
--------------
Various logic4mqtt functions deal with MQTT topics. To simplify topic usage in accordance with the MQTT-smarthome specification, 
there are some special constructs which are replaced depending on contexts:

  - When setting values, a "//" gets replaced with "/set/"
  - When getting values, a "//" gets replaced with "/status/"
  - A "$" prefix gets replaced with the configured logic4mqtt own topic prefix, with "/status/" added.
    This is intended to faciliate global state variables. 

Example:

	Events.setValue("knx//Floor/Livingroom/Light One",true)
	
publishes the value "1" to `knx/set/Floor/Livingroom/Light One`
whereas 

	Events.getValue("knx//Floor/Livingroom/Light One")
	
returns the value of the topic `knx/status/Floor/Livingroom/Light One`

	Events.storeValue("$Status Lighting",1)

and

	Events.getValue("$Status Lighting")
	
however both reference `logic/status/Stauts Lighting`


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

    Time.setLocation(latitude,longitude)
    
prior to use.

Changelog
---------
* 0.7 - 2015/02/23 - owagner
  - API: renamed the SunriseSunset object to "Time"
  - API: added Time.isBefore(), Time.isAfter() and Time.isBetween()
  - API: added Utilities.executeCommand(cmd)
  - API: added Events.storeValue() and Events.queueStore(), which complement .setValue() and .queueValue()
    respectivly, but cause the "retain" flag of the published message to be set. This is mainly intended to
    be used with global variables.
  - API: A "$" topic prefix gets replaced with the configured logic4mqtt own topic prefix, with "/status/" added.
    This is intended to faciliate global state variables. 
  
* 0.6 - 2015/01/25 - owagner
  - adapted to new mqtt-smarthome topic hierarchy scheme: /status/ for reports, /set/ for setting values.
    A special notation is supported -- if the first topic part is suffixed with "//", it gets
    replaced with /set/ and /status/ depending on context. For example, an event handler set
    to "knx//kitchen/light" gets to be actually set to "knx/status/kitchen/light", whereas
    a Event.setValue("knx//kitchen/light"...) will set "knx/set/kitchen/light"
  - now skips common backup suffixes when looking for scripts to execute (right now, "~" and ".bak")
  - when publishing to MQTT, round numbers to 8 decimal digits, and try to avoid a decimal point
    alltogether for integers. Also, convert Booleans to 0/1.
  - API: SunriseSunset now has getSunrise() and getSunset() methods which accept a string denoting
    the required Zenith (OFFICIAL, ASTRONOMICAL, NAUTICAL and CIVIL)
  - API: added SunriseSunset.isDaylight() and variants to quickly check whether it's currently
    between sunrise and sunset for the given Zenith

* 0.5 - 2015/01/18 - owagner
  - API: added Utilites.sendNetMessage() function for doing very simple network communication
  - when running Rhino, explicitely set setJavaPrimitiveWrap(false). Otherwise, primitive types returned
    from Java methods like Events.getValue() end up being Javascript Object instead of primitive
    instances
  - reset log prefix to [callback] when initial script runs have finished

* 0.4 - 2015/01/08 - owagner
  - use one ScriptEngine instance per file suffix, so the complete context is shared among scripts
  - read version number from jar manifest or build.gradle
  - redirect script output to java logging, with the log messages being [prefixed] with the script
    name
  


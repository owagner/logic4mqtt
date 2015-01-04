/*
 * Simple event: when a PIR presence detector detects presence, turn on a control tablet using WOL 
 */

Events.onChangeTo("knxkueche/Küche/Sensoren/Präsenz",1,function(topic,val)
{
	Utilities.sendWOL("80:ee:73:5d:43:e8","eth0");
});


/*
 * Solar calculations. Based on http://aa.quae.nl/en/reken/zonpositie.html
 */

package com.tellerulam.logic4mqtt;

import java.util.*;

public class SunCalc
{
	public static class DayInfo
	{
		public final int sunrise[] = new int[5];
		public final int sunset[] = new int[5];
	}
	public static class PositionInfo
	{
		public double azimuth;
		public double altitude;
	}

	public static DayInfo getDayInfo()
	{
		long now = System.currentTimeMillis();
		if(now > dayInfoValidUntil)
			calcDayInfo();
		return dayInfo;
	}

	public static PositionInfo getPositionInfo()
	{
		long now = System.currentTimeMillis();
		if(now > positionValidUntil)
			calcPositionInfo();
		return positionInfo;
	}

	public static void setLatLng(double lat, double lng)
	{
		lw = -lng * deg2rad;
		phi = lat * deg2rad;
		dayInfoValidUntil = 0;
		positionValidUntil = 0;
	}

	@SuppressWarnings("boxing")
	public static String makeTimeString(int msm)
	{
		return String.format("%02d:%02d", msm/60,msm%60);
	}

	static
	{
		setLatLng(51.358813, 7.241483);
	}

	/*
	 * Here be dragons
	 */

	private static final int J1970 = 2440588;
	private static final int J2000 = 2451545;
	private static final double deg2rad = Math.PI / 180;
	private static final double M0 = 357.5291 * deg2rad;
	private static final double M1 = 0.98560028 * deg2rad;
	private static final double J0 = 0.0009;
	private static final double J1 = 0.0053;
	private static final double J2 = -0.0069;
	private static final double C1 = 1.9148 * deg2rad;
	private static final double C2 = 0.0200 * deg2rad;
	private static final double C3 = 0.0003 * deg2rad;
	private static final double P = 102.9372 * deg2rad;
	private static final double e = 23.45 * deg2rad;
	private static final double th0 = 280.1600 * deg2rad;
	private static final double th1 = 360.9856235 * deg2rad;
	private static final double h0 = -0.83 * deg2rad; // sunset angle
	private static final double d0 = 0.53 * deg2rad; // sun diameter
	private static final double h1 = -6 * deg2rad; // nautical twilight angle
	private static final double h2 = -12 * deg2rad; // astronomical twilight
													// angle
	private static final double h3 = -18 * deg2rad; // darkness angle
	private static final double msInDay = 1000 * 60 * 60 * 24;

	private static double dateToJulianDate(Date d)
	{
		return d.getTime() / msInDay - 0.5 + J1970;
	}

	/*
	private static Date julianDateToDate(double j)
	{
		return new Date((long)((j + 0.5 - J1970) * msInDay));
	}
	*/

	private static int julianDateToMinutesSinceMidnight(double j)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis((long)((j + 0.5 - J1970) * msInDay));
		return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
	}

	private static double getJulianCycle(double J, double lw)
	{
		return Math.round(J - J2000 - J0 - lw / (2 * Math.PI));
	}

	private static double getApproxSolarTransit(double Ht, double lw, double n)
	{
		return J2000 + J0 + (Ht + lw) / (2 * Math.PI) + n;
	}

	private static double getSolarMeanAnomaly(double Js)
	{
		return M0 + M1 * (Js - J2000);
	}

	private static double getEquationOfCenter(double M)
	{
		return C1 * Math.sin(M) + C2 * Math.sin(2 * M) + C3 * Math.sin(3 * M);
	}

	private static double getEclipticLongitude(double M, double C)
	{
		return M + P + C + Math.PI;
	}

	private static double getSolarTransit(double Js, double M, double Lsun)
	{
		return Js + (J1 * Math.sin(M)) + (J2 * Math.sin(2 * Lsun));
	}

	private static double getSunDeclination(double Lsun)
	{
		return Math.asin(Math.sin(Lsun) * Math.sin(e));
	}

	private static double getRightAscension(double Lsun)
	{
		return Math.atan2(Math.sin(Lsun) * Math.cos(e), Math.cos(Lsun));
	}

	private static double getSiderealTime(double J, double lw)
	{
		return th0 + th1 * (J - J2000) - lw;
	}

	private static double getAzimuth(double H, double th, double a, double phi, double d)
	{
		return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(d) * Math.cos(phi));
	}

	private static double getAltitude(double H, double th, double a, double phi, double d)
	{
		return Math.asin(Math.sin(phi) * Math.sin(d) + Math.cos(phi) * Math.cos(d) * Math.cos(H));
	}

	private static double getHourAngle(double h, double phi, double d)
	{
		return Math.acos((Math.sin(h) - Math.sin(phi) * Math.sin(d)) / (Math.cos(phi) * Math.cos(d)));
	}

	private static double getSunsetJulianDate(double w0, double M, double Lsun, double lw, double n)
	{
		return getSolarTransit(getApproxSolarTransit(w0, lw, n), M, Lsun);
	}

	private static double getSunriseJulianDate(double Jtransit, double Jset)
	{
		return Jtransit - (Jset - Jtransit);
	}

	/*
	public static void aztest()
	{
		double J = dateToJulianDate(new Date());
		for(double j=J-0.5;j<J+0.5;j+=0.01)
		{
			double M = getSolarMeanAnomaly(j), C = getEquationOfCenter(M), Lsun = getEclipticLongitude(M, C), d = getSunDeclination(Lsun), a = getRightAscension(Lsun), th = getSiderealTime(j, lw), H = th - a;
			System.out.println(makeTimeString(julianDateToMinutesSinceMidnight(j))+" "+(Math.PI+getAzimuth(H, th, a, phi, d))/deg2rad+" "+H);
		}
	}
	*/

	private static void calcPositionInfo()
	{
		Calendar cal = Calendar.getInstance();
		double J = dateToJulianDate(cal.getTime());
		double M = getSolarMeanAnomaly(J), C = getEquationOfCenter(M), Lsun = getEclipticLongitude(M, C), d = getSunDeclination(Lsun), a = getRightAscension(Lsun), th = getSiderealTime(J, lw), H = th - a;
		PositionInfo pi = new PositionInfo();
		pi.azimuth = (Math.PI+getAzimuth(H, th, a, phi, d))/deg2rad;
		pi.altitude = getAltitude(H, th, a, phi, d)/deg2rad;
		positionInfo = pi;
		// Valid for 1 minute
		positionValidUntil = cal.getTimeInMillis() + 60 * 1000;
	}

	private static void calcDayInfo()
	{
		Calendar cal = Calendar.getInstance();

		double n = getJulianCycle(dateToJulianDate(cal.getTime()), lw), Js = getApproxSolarTransit(0, lw, n), M = getSolarMeanAnomaly(Js), C = getEquationOfCenter(M), Lsun = getEclipticLongitude(M, C), d = getSunDeclination(Lsun), Jtransit = getSolarTransit(Js, M, Lsun), w0 = getHourAngle(h0, phi, d), w1 = getHourAngle(h0 + d0, phi, d), Jset = getSunsetJulianDate(w0, M, Lsun, lw, n), Jsetstart = getSunsetJulianDate(w1, M, Lsun, lw, n), Jrise = getSunriseJulianDate(Jtransit, Jset), Jriseend = getSunriseJulianDate(
			Jtransit, Jsetstart), w2 = getHourAngle(h1, phi, d), Jnau = getSunsetJulianDate(w2, M, Lsun, lw, n), Jciv2 = getSunriseJulianDate(Jtransit, Jnau), w3 = getHourAngle(h2, phi, d), w4 = getHourAngle(h3, phi, d), Jastro = getSunsetJulianDate(w3, M, Lsun, lw, n), Jdark = getSunsetJulianDate(w4, M, Lsun, lw, n), Jnau2 = getSunriseJulianDate(Jtransit, Jastro), Jastro2 = getSunriseJulianDate(Jtransit, Jdark);

		DayInfo di = new DayInfo();

		di.sunrise[0] = julianDateToMinutesSinceMidnight(Jastro2);
		di.sunrise[1] = julianDateToMinutesSinceMidnight(Jnau2);
		di.sunrise[2] = julianDateToMinutesSinceMidnight(Jciv2);
		di.sunrise[3] = julianDateToMinutesSinceMidnight(Jrise);
		di.sunrise[4] = julianDateToMinutesSinceMidnight(Jriseend);

		di.sunset[0] = julianDateToMinutesSinceMidnight(Jsetstart);
		di.sunset[1] = julianDateToMinutesSinceMidnight(Jset);
		di.sunset[2] = julianDateToMinutesSinceMidnight(Jnau);
		di.sunset[3] = julianDateToMinutesSinceMidnight(Jastro);
		di.sunset[4] = julianDateToMinutesSinceMidnight(Jdark);

		// Valid until ~midnight
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_YEAR, 1);

		dayInfo = di;
		dayInfoValidUntil = cal.getTimeInMillis();
	}

	private static long dayInfoValidUntil, positionValidUntil;
	private static DayInfo dayInfo;
	private static PositionInfo positionInfo;
	private static double lw, phi;

}

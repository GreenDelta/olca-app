package org.openlca.app.collaboration.util;

import java.util.Calendar;

import org.openlca.app.M;

public class Format {

	public static String commitDate(long value) {
		var today = Calendar.getInstance();
		var cal = Calendar.getInstance();
		cal.setTimeInMillis(value);
		if (cal.after(today))
			return M.InFuture;
		var seconds = getDifference(today, cal, Calendar.SECOND, 60);
		if (seconds < 60)
			return timeText(seconds, M.SecondsAgo);
		var minutes = getDifference(today, cal, Calendar.MINUTE, 60);
		if (minutes < 60)
			return timeText(minutes, M.MinutesAgo);
		var hours = getDifference(today, cal, Calendar.HOUR_OF_DAY, 24);
		if (hours < 24)
			return timeText(hours, M.HoursAgo);
		var days = getDifference(today, cal, Calendar.DAY_OF_MONTH, 365);
		if (days < 7)
			return timeText(days, M.DaysAgo);
		if (days < 31)
			return timeText(days / 7, M.WeeksAgo);
		var months = getDifference(today, cal, Calendar.MONTH, 12);
		if (days < 365 && months > 0)
			return timeText(months, M.MonthsAgo);
		var years = Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR);
		return timeText(years, M.YearsAgo);
	}

	private static int getDifference(Calendar c1, Calendar c2, int type, int max) {
		var tmp = Calendar.getInstance();
		tmp.setTime(c1.getTime());
		var days = -1;
		while (c2.before(tmp)) {
			tmp.add(type, -1);
			days++;
			// more is not of interest here
			if (days == max)
				break;
		}
		return days;
	}

	private static String timeText(int value, String timeUnit) {
		return value + " " + timeUnit;
	}

}

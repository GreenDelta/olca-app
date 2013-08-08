package org.openlca.core.application;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formatter for dates in different locales
 * 
 * @author Sebastian Greve
 * 
 */
public class DateFormatter {

	/**
	 * Formats the date with the locale and returns it as string
	 * 
	 * @param date
	 *            The date to format
	 * @return The locale formatted date
	 */
	public static String formatShort(final Date date) {
		return DateFormat
				.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(
						date);
	}

	/**
	 * Formats the date with the locale and returns it as string
	 * 
	 * @param date
	 *            The date to format
	 * @return The locale formatted date
	 */
	public static String formatLong(final Date date) {
		return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
				.format(date);
	}

}

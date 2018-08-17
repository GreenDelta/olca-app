package org.openlca.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.openlca.app.Preferences;

/**
 * Provides methods for number formatting.
 */
public class Numbers {

	private static DecimalFormat percentFormat = new DecimalFormat("#00.00%",
			new DecimalFormatSymbols(Locale.US));
	private static DecimalFormat simpleFormat = getFormat("0.000");
	private static DecimalFormat scienceFormat = getFormat("0.000E0");
	private static double lowerBound = 0.001;
	private static double upperBound = 1000;

	public static void setDefaultAccuracy(int decimalPlaces) {
		int acc = decimalPlaces < 1 || decimalPlaces > 50 ? 4 : decimalPlaces;
		String pattern = createPattern(acc);
		simpleFormat = getFormat(pattern);
		scienceFormat = getFormat(pattern.concat("E0"));
		lowerBound = 1 / Math.pow(10, acc - 1);
		upperBound = Math.pow(10, acc - 1);
	}

	public static String percent(double number) {
		return apply(percentFormat, number);
	}

	public static String format(double number) {
		if (!Preferences.is(Preferences.FORMAT_INPUT_VALUES))
			return Double.toString(number);
		if (applySimpleFormat(number, lowerBound, upperBound))
			return apply(simpleFormat, number);
		return apply(scienceFormat, number);
	}

	public static String decimalFormat(double number, int decimals) {
		String pattern = "0";
		if (decimals > 0)
			pattern += ".";
		for (int i = 1; i <= decimals; i++)
			pattern += "0";
		return apply(getFormat(pattern), number);
	}

	public static String format(double number, int accuracy) {
		int acc = accuracy < 1 || accuracy > 50 ? 4 : accuracy;
		String pattern = createPattern(acc);
		double lower = 1 / Math.pow(10, acc - 1);
		double upper = Math.pow(10, acc - 1);
		if (applySimpleFormat(number, lower, upper))
			return apply(getFormat(pattern), number);
		return apply(getFormat(pattern.concat("E0")), number);
	}

	private static DecimalFormat getFormat(String pattern) {
		NumberFormat f = NumberFormat.getNumberInstance(Locale.ENGLISH);
		if (f instanceof DecimalFormat) {
			DecimalFormat format = (DecimalFormat) f;
			format.applyPattern(pattern);
			return format;
		}
		return null;
	}

	private static String createPattern(int acc) {
		String pattern = "0.";
		for (int i = 0; i < acc; i++)
			pattern = pattern.concat("0");
		return pattern;
	}

	private static String apply(DecimalFormat format, double number) {
		if (format == null)
			return NumberFormat.getNumberInstance(Locale.ENGLISH)
					.format(number);
		return format.format(number);
	}

	private static boolean applySimpleFormat(double number, double lower,
			double upper) {
		return number == 0
				|| (number >= -upper && number <= -lower)
				|| (number >= lower && number <= upper);
	}

}

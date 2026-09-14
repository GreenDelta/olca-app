package org.openlca.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

import org.openlca.app.preferences.Preferences;
import org.openlca.commons.Strings;

/**
 * Provides methods for number formatting.
 */
public class Numbers {

	private static final DecimalFormat percentFormat = new DecimalFormat(
		"#00.00%", new DecimalFormatSymbols(Locale.US));
	private static DecimalFormat simpleFormat = getFormat("0.000");
	private static DecimalFormat scienceFormat = getFormat("0.000E0");
	private static double lowerBound = 0.001;
	private static double upperBound = 1000;
	private static final SimpleDateFormat timestampFormat = new SimpleDateFormat(
		"yyyy-MM-dd HH:mm:ss");

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
		if (!Preferences.getBool(Preferences.FORMAT_INPUT_VALUES))
			return Double.toString(number);
		if (applySimpleFormat(number, lowerBound, upperBound))
			return apply(simpleFormat, number);
		return apply(scienceFormat, number);
	}

	public static String decimalFormat(double number, int decimals) {
		var pattern = "0";
		if (decimals > 0) {
			pattern += "." + "0".repeat(decimals);
		}
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
		var f = NumberFormat.getNumberInstance(Locale.ENGLISH);
		if (f instanceof DecimalFormat format) {
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

	private static boolean applySimpleFormat(
		double number, double lower, double upper
	) {
		return number == 0
			|| (number >= -upper && number <= -lower)
			|| (number >= lower && number <= upper);
	}

	public static String asTimestamp(long time) {
		return time <= 0
			? "---"
			: timestampFormat.format(new Date(time));
	}

	/// Tries to parse the given input with different number formats.
	public static OptionalDouble tryParseAnyFormat(String input) {
		var num = NumberParser.parseAny(input);
		if (num == null)
			return OptionalDouble.empty();
		return num.isNaN() || num.isInfinite()
			? OptionalDouble.empty()
			: OptionalDouble.of(num);
	}

	private static class NumberParser {

		// major distinct global format families
		private static final List<Locale> LOCALES = List.of(
			Locale.US,                // 1,234,567.89 (Dot decimal)
			Locale.GERMANY,           // 1.234.567,89 (Comma decimal, dot grouping)
			Locale.FRANCE,            // 1 234 567,89 (Comma decimal, space grouping)
			Locale.of("de", "CH"),    // 1'234'567.89 (Apostrophe grouping)
			Locale.of("en", "IN")     // 12,34,567.89 (Indian grouping)
		);

		private static Double parseAny(String input) {
			if (Strings.isBlank(input))
				return null;

			var s = input.trim();

			// try to parse the default format
			try {
				return Double.parseDouble(s);
			} catch (Exception _) {
			}

			// try to parse with comma as decimal separator
			if (s.contains(",") && !s.contains(".")) {
				try {
					return Double.parseDouble(s.replace(',', '.'));
				} catch (Exception _) {
				}
			}

			// try with common number formats
			for (var locale : LOCALES) {
				var nf = NumberFormat.getInstance(locale);
				nf.setStrict(true);
				var pos = new ParsePosition(0);
				var num = nf.parse(s, pos);
				// CRITICAL: verify the parser consumed the entire string!
				if (num != null && pos.getIndex() == s.length()) {
					return num.doubleValue();
				}
			}
			return null;
		}
	}

}

package org.openlca.app.util;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.preferences.Theme;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQSystem;
import org.openlca.util.Strings;

public class DQUI {

	public static final int MIN_COL_WIDTH = 20;

	public static String[] appendTableHeaders(String[] headers, DQSystem system) {
		var newHeaders = new String[headers.length + system.indicators.size()];
		System.arraycopy(headers, 0, newHeaders, 0, headers.length);
		for (int i = headers.length; i < newHeaders.length; i++) {
			int pos = i - headers.length + 1;
			var indicator = system.getIndicator(pos);
			newHeaders[i] = Strings.nullOrEmpty(indicator.name)
					? Integer.toString(indicator.position)
					: Character.toString(indicator.name.charAt(0));
		}
		return newHeaders;
	}

	public static double[] adjustTableWidths(double[] original, DQSystem system) {
		double[] adjusted = new double[original.length + system.indicators.size()];
		double maxValue = 0;
		int maxIndex = 0;
		for (int i = 0; i < original.length; i++) {
			adjusted[i] = original[i];
			if (adjusted[i] > maxValue) {
				maxValue = adjusted[i];
				maxIndex = i;
			}
		}
		adjusted[maxIndex] = adjusted[maxIndex] - .1;
		for (int i = original.length; i < adjusted.length; i++) {
			adjusted[i] = .1 / system.indicators.size();
		}
		return adjusted;
	}

	/**
	 * Return the corresponding color for the given data quality value. If the
	 * value is 0, the default background color is returned.
	 */
	public static Color getColor(int value, int total) {
		if (value <= 0)
			return Colors.background();
		if (value == 1)
			return green();
		if (value >= total)
			return red();
		int median = total / 2 + 1;
		if (value == median)
			return yellow();

		int divisor = median - 1;
		if (value < median) {
			int num = value - 1;
			return moreGreen((double) num / divisor);
		}
		int num = value - median;
		return moreRed((double) num / divisor);
	}

	private static Color green() {
		return Theme.isDark()
				? Colors.get(83, 167, 83)
				: Colors.get(125, 250, 125);
	}

	private static Color red() {
		return Theme.isDark()
				? Colors.get(167, 83, 83)
				: Colors.get(250, 125, 125);
	}

	private static Color yellow() {
		return Theme.isDark()
				? Colors.get(167, 167, 83)
				: Colors.get(250, 250, 125);
	}

	private static Color moreGreen(double factor) {
		return Theme.isDark()
				? Colors.get((int) (83 + (83 * factor)), 167, 83)
				: Colors.get((int) (125 + (125 * factor)), 250, 125);
	}

	private static Color moreRed(double factor) {
		return Theme.isDark()
				? Colors.get(167, (int) (167 - (83 * factor)), 83)
				: Colors.get(250, (int) (250 - (125 * factor)), 125);
	}

	public static boolean displayProcessQuality(DQResult result) {
		if (result == null)
			return false;
		if (result.setup.processSystem == null)
			return false;
		if (result.setup.processSystem.indicators.isEmpty())
			return false;
		return result.setup.processSystem.getScoreCount() != 0;
	}

	public static boolean displayExchangeQuality(DQResult result) {
		if (result == null)
			return false;
		if (result.setup.exchangeSystem == null)
			return false;
		if (result.setup.exchangeSystem.indicators.isEmpty())
			return false;
		if (result.setup.exchangeSystem.getScoreCount() == 0)
			return false;
		return result.setup.aggregationType != AggregationType.NONE;
	}

}

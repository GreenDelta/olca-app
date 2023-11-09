package org.openlca.app.util;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.preferences.Theme;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQSystem;
import org.python.google.common.base.Strings;

public class DQUI {

	public static final int MIN_COL_WIDTH = 20;

	public static String[] appendTableHeaders(String[] headers, DQSystem system) {
		String[] newHeaders = new String[headers.length + system.indicators.size()];
		for (int i = 0; i < headers.length; i++) {
			newHeaders[i] = headers[i];
		}
		for (int i = headers.length; i < newHeaders.length; i++) {
			int pos = i - headers.length + 1;
			DQIndicator indicator = system.getIndicator(pos);
			if (Strings.isNullOrEmpty(indicator.name))
				newHeaders[i] = Integer.toString(indicator.position);
			else
				newHeaders[i] = Character.toString(indicator.name.charAt(0));
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
	 * Return the corresponding color if the value is more than 0, otherwise null.
	 * The default color should be managed by the caller.
	 */
	public static Color getColor(int value, int total) {
		if (value <= 0)
			return Colors.background();
		if (value == 1)
			return green();
		if (value == total)
			return red();
		int median = total / 2 + 1;
		if (value == median)
			return yellow();
		if (value < median) {
			int divisor = median - 1;
			int factor = value - 1;
			return colorInferior((double) factor / divisor);
		}
		int divisor = median - 1;
		int factor = value - median;
		return colorSuperior((double) factor / divisor);
	}

	private static Color green() {
		if (Theme.isDark()) {
			return Colors.get(83, 167, 83);
		} else return Colors.get(125, 250, 125);
	}

	private static Color red() {
		if (Theme.isDark()) {
			return Colors.get(167, 83, 83);
		} else return Colors.get(250, 125, 125);
	}

	private static Color yellow() {
		if (Theme.isDark()) {
			return Colors.get(167, 167, 83);
		} else return Colors.get(250, 250, 125);
	}

	private static Color colorInferior(double factor) {
		if (Theme.isDark()) {
			return Colors.get((int) (83 + (83 * factor)), 167, 83);
		} else return Colors.get((int) (125 + (125 * factor)), 250, 125);
	}

	private static Color colorSuperior(double factor) {
		if (Theme.isDark()) {
			return Colors.get(167, (int) (167 - (83 * factor)), 83);
		} else return Colors.get(250, (int) (250 - (125 * factor)), 125);
	}

	public static boolean displayProcessQuality(DQResult result) {
		if (result == null)
			return false;
		if (result.setup.processSystem == null)
			return false;
		if (result.setup.processSystem.indicators.isEmpty())
			return false;
		if (result.setup.processSystem.getScoreCount() == 0)
			return false;
		return true;
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
		if (result.setup.aggregationType == AggregationType.NONE)
			return false;
		return true;
	}

}

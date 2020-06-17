package org.openlca.app.util;

import org.eclipse.swt.graphics.Color;
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

	public static Color getColor(int value, int total) {
		if (value <= 0)
			return Colors.white();
		if (value == 1)
			return Colors.get(125, 250, 125);
		if (value == total)
			return Colors.get(250, 125, 125);
		int median = total / 2 + 1;
		if (value == median)
			return Colors.get(250, 250, 125);
		if (value < median) {
			int divisor = median - 1;
			int factor = value - 1;
			return Colors.get(125 + (125 * factor / divisor), 250, 125);
		}
		int divisor = median - 1;
		int factor = value - median;
		return Colors.get(250, 250 - (125 * factor / divisor), 125);
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

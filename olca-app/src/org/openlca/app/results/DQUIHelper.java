package org.openlca.app.results;

import org.openlca.core.model.DQSystem;

class DQUIHelper {

	public static String[] appendTableHeaders(String[] headers, DQSystem system) {
		String[] newHeaders = new String[headers.length + system.indicators.size()];
		for (int i = 0; i < headers.length; i++) {
			newHeaders[i] = headers[i];
		}
		for (int i = headers.length; i < newHeaders.length; i++) {
			int pos = i - headers.length + 1;
			newHeaders[i] = system.getIndicator(pos).name;
		}
		return newHeaders;
	}

	public static double[] adjustTableWidths(double[] original, DQSystem system) {
		double[] adjusted = new double[original.length + system.indicators.size()];
		for (int i = 0; i < original.length; i++) {
			adjusted[i] = original[i];
		}
		adjusted[0] = adjusted[0] - .1;
		for (int i = original.length; i < adjusted.length; i++) {
			adjusted[i] = .1 / system.indicators.size();
		}
		return adjusted;
	}

}

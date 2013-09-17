package org.openlca.app.util;

import org.openlca.core.model.Uncertainty;

/**
 * Converts uncertainty information to a single label.
 */
public final class UncertaintyLabel {

	private UncertaintyLabel() {
	}

	public static String get(Uncertainty uncertainty) {
		if (uncertainty == null || uncertainty.getDistributionType() == null)
			return "none";
		switch (uncertainty.getDistributionType()) {
		case NONE:
			return "none";
		case LOG_NORMAL:
			return lognormal(uncertainty);
		case NORMAL:
			return normal(uncertainty);
		case UNIFORM:
			return uniform(uncertainty);
		case TRIANGLE:
			return triangular(uncertainty);
		default:
			return "none";
		}
	}

	private static String lognormal(Uncertainty u) {
		String template = "lognormal: gmean=%s gsigma=%s";
		String gmean = format(u.getParameter1Value(), u.getParameter1Formula());
		String gsigma = format(u.getParameter2Value(), u.getParameter2Formula());
		return String.format(template, gmean, gsigma);
	}

	private static String normal(Uncertainty u) {
		String template = "normal: mean=%s sigma=%s";
		String mean = format(u.getParameter1Value(), u.getParameter1Formula());
		String sigma = format(u.getParameter2Value(), u.getParameter2Formula());
		return String.format(template, mean, sigma);
	}

	private static String uniform(Uncertainty u) {
		String template = "uniform: min=%s max=%s";
		String min = format(u.getParameter1Value(), u.getParameter1Formula());
		String max = format(u.getParameter2Value(), u.getParameter2Formula());
		return String.format(template, min, max);
	}

	private static String triangular(Uncertainty u) {
		String template = "triangular: min=%s mode=%s max=%s";
		String min = format(u.getParameter1Value(), u.getParameter1Formula());
		String mode = format(u.getParameter2Value(), u.getParameter2Formula());
		String max = format(u.getParameter3Value(), u.getParameter3Formula());
		return String.format(template, min, mode, max);
	}

	private static String format(double number, String formula) {
		String val = Numbers.format(number, 2);
		if (formula != null)
			return val + " (" + formula + ")";
		return val;
	}

}

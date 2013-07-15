package org.openlca.core.editors.lciamethod;

import org.openlca.core.application.Numbers;
import org.openlca.core.model.ImpactFactor;

/**
 * Converts the uncertainty information of an impact assessment factor to a
 * single label.
 */
final class UncertaintyLabel {

	private UncertaintyLabel() {
	}

	static String get(ImpactFactor factor) {
		if (factor == null || factor.getUncertaintyType() == null)
			return "none";
		switch (factor.getUncertaintyType()) {
		case NONE:
			return "none";
		case LOG_NORMAL:
			return lognormal(factor);
		case NORMAL:
			return normal(factor);
		case UNIFORM:
			return uniform(factor);
		case TRIANGLE:
			return triangular(factor);
		default:
			return "none";
		}
	}

	private static String lognormal(ImpactFactor factor) {
		String template = "lognormal: gmean=%s gsigma=%s";
		String gmean = format(factor.getUncertaintyParameter1());
		String gsigma = format(factor.getUncertaintyParameter2());
		return String.format(template, gmean, gsigma);
	}

	private static String normal(ImpactFactor factor) {
		String template = "normal: mean=%s sigma=%s";
		String mean = format(factor.getUncertaintyParameter1());
		String sigma = format(factor.getUncertaintyParameter2());
		return String.format(template, mean, sigma);
	}

	private static String uniform(ImpactFactor factor) {
		String template = "uniform: min=%s max=%s";
		String min = format(factor.getUncertaintyParameter1());
		String max = format(factor.getUncertaintyParameter2());
		return String.format(template, min, max);
	}

	private static String triangular(ImpactFactor factor) {
		String template = "triangular: min=%s mode=%s max=%s";
		String min = format(factor.getUncertaintyParameter1());
		String mode = format(factor.getUncertaintyParameter2());
		String max = format(factor.getUncertaintyParameter3());
		return String.format(template, min, mode, max);
	}

	private static String format(double number) {
		return Numbers.format(number, 2);
	}

}

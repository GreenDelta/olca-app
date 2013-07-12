package org.openlca.core.editors.lciamethod;

import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.UncertaintyDistributionType;

/**
 * A helper class which sets the uncertainty values to an impact assessment
 * factor and updates the respective factor value.
 */
final class UncertaintySetter {

	private UncertaintySetter() {
	}

	static void setValues(ImpactFactor factor,
			UncertaintyDistributionType type, double... parameters) {
		if (factor == null)
			return;
		if (type == null || type == UncertaintyDistributionType.NONE) {
			clear(factor);
			return;
		}
		switch (type) {
		case NORMAL:
			setNormal(factor, parameters);
			break;
		case LOG_NORMAL:
			setLognormal(factor, parameters);
			break;
		case UNIFORM:
			setUniform(factor, parameters);
			break;
		case TRIANGLE:
			setTriangular(factor, parameters);
			break;
		default:
			clear(factor);
		}
	}

	private static void clear(ImpactFactor factor) {
		factor.setUncertaintyType(UncertaintyDistributionType.NONE);
		factor.setUncertaintyParameter1(0);
		factor.setUncertaintyParameter2(0);
		factor.setUncertaintyParameter3(0);
	}

	private static void setNormal(ImpactFactor factor, double[] params) {
		factor.setUncertaintyType(UncertaintyDistributionType.NORMAL);
		factor.setValue(params[0]);
		factor.setUncertaintyParameter1(params[0]);
		factor.setUncertaintyParameter2(params[1]);
		factor.setUncertaintyParameter3(0);
	}

	private static void setLognormal(ImpactFactor factor, double[] params) {
		factor.setUncertaintyType(UncertaintyDistributionType.LOG_NORMAL);
		factor.setValue(params[0]);
		factor.setUncertaintyParameter1(params[0]);
		factor.setUncertaintyParameter2(params[1]);
		factor.setUncertaintyParameter3(0);
	}

	private static void setUniform(ImpactFactor factor, double[] params) {
		factor.setUncertaintyType(UncertaintyDistributionType.UNIFORM);
		double min = params[0];
		double max = params[1];
		double mean = (min + max) / 2;
		factor.setValue(mean);
		factor.setUncertaintyParameter1(min);
		factor.setUncertaintyParameter2(max);
		factor.setUncertaintyParameter3(0);
	}

	private static void setTriangular(ImpactFactor factor, double[] params) {
		factor.setUncertaintyType(UncertaintyDistributionType.TRIANGLE);
		double min = params[0];
		double mode = params[1];
		double max = params[2];
		double mean = (min + mode + max) / 3;
		factor.setValue(mean);
		factor.setUncertaintyParameter1(min);
		factor.setUncertaintyParameter2(mode);
		factor.setUncertaintyParameter3(max);
	}

}

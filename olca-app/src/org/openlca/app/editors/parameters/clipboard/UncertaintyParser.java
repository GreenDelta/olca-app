package org.openlca.app.editors.parameters.clipboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;

class UncertaintyParser {

	private UncertaintyType[] uncertaintyTypes = {
			UncertaintyType.LOG_NORMAL,
			UncertaintyType.NORMAL,
			UncertaintyType.TRIANGLE,
			UncertaintyType.UNIFORM
	};

	private String[] uncertaintyPatterns = {
			"\\s*lognormal:\\s+gmean=([0-9,\\.]+)\\s+gsigma=([0-9,\\.]+)\\s*",
			"\\s*normal:\\s+mean=([0-9,\\.]+)\\s+sigma=([0-9,\\.]+)\\s*",
			"\\s*triangular:\\s+min=([0-9,\\.]+)\\s+mode=([0-9,\\.]+)\\s+max=([0-9,\\.]+)\\s*",
			"\\s*uniform:\\s+min=([0-9,\\.]+)\\s+max=([0-9,\\.]+)\\s*"
	};

	Uncertainty read(String field) {
		if (field == null)
			return null;
		for (int i = 0; i < uncertaintyPatterns.length; i++) {
			Pattern p = Pattern.compile(uncertaintyPatterns[i]);
			Matcher m = p.matcher(field);
			if (m.find())
				return getUncertainty(m, uncertaintyTypes[i]);
		}
		return null;
	}

	private Uncertainty getUncertainty(Matcher m, UncertaintyType type) {
		if (m == null || type == null)
			return null;
		switch (type) {
		case LOG_NORMAL:
			return matchLogNormal(m);
		case NORMAL:
			return matchNormal(m);
		case TRIANGLE:
			return matchTriangle(m);
		case UNIFORM:
			return matchUniform(m);
		default:
			return null;
		}
	}

	private Uncertainty matchUniform(Matcher m) {
		try {
			double min = Clipboard.readDouble(m.group(1));
			double max = Clipboard.readDouble(m.group(2));
			return Uncertainty.uniform(min, max);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchTriangle(Matcher m) {
		try {
			double min = Clipboard.readDouble(m.group(1));
			double mode = Clipboard.readDouble(m.group(2));
			double max = Clipboard.readDouble(m.group(3));
			return Uncertainty.triangle(min, mode, max);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchNormal(Matcher m) {
		try {
			double mean = Clipboard.readDouble(m.group(1));
			double sigma = Clipboard.readDouble(m.group(2));
			return Uncertainty.normal(mean, sigma);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchLogNormal(Matcher m) {
		try {
			double gmean = Clipboard.readDouble(m.group(1));
			double gsigma = Clipboard.readDouble(m.group(2));
			return Uncertainty.logNormal(gmean, gsigma);
		} catch (Exception e) {
			return null;
		}
	}
}

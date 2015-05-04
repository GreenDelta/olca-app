package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openlca.core.model.Parameter;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;

import com.google.common.base.Strings;

class Clipboard {

	private String text;

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

	private Clipboard(String text) {
		this.text = text;
	}

	public static List<Parameter> readInputParams(String text) {
		return new Clipboard(text).readParams(true);
	}

	public static List<Parameter> readCalculatedParams(String text) {
		return new Clipboard(text).readParams(false);
	}

	private List<Parameter> readParams(boolean input) {
		if (text == null)
			return Collections.emptyList();
		List<Parameter> list = new ArrayList<>();
		String[] rows = text.toString().split("\n");
		for (String row : rows) {
			String[] fields = row.split("\t");
			Parameter p = input ? readInputParam(fields)
					: readCalcParam(fields);
			if (p != null)
				list.add(p);
		}
		return list;
	}

	private Parameter readCalcParam(String[] fields) {
		if (fields == null || fields.length < 2)
			return null;
		String name = fields[0];
		String formula = fields[1];
		if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(formula))
			return null;
		Parameter p = new Parameter();
		p.setInputParameter(false);
		p.setName(name);
		p.setFormula(formula);
		if (fields.length > 2)
			p.setValue(parseValue(fields[2]));
		if (fields.length > 3)
			p.setDescription(fields[3]);
		return p;
	}

	private Parameter readInputParam(String[] fields) {
		if (fields == null || fields.length < 2)
			return null;
		String name = fields[0];
		if (Strings.isNullOrEmpty(name))
			return null;
		double val = parseValue(fields[1]);
		Parameter p = new Parameter();
		p.setInputParameter(true);
		p.setName(name);
		p.setValue(val);
		if (fields.length > 2)
			p.setUncertainty(getUncertainty(fields[2]));
		if (fields.length > 3)
			p.setDescription(fields[3]);
		return p;
	}

	private double parseValue(String field) {
		if (field == null)
			return Double.NaN;
		try {
			String f = field.replace(',', '.');
			return Double.parseDouble(f);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

	private Uncertainty getUncertainty(String field) {
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
			double min = parseValue(m.group(1));
			double max = parseValue(m.group(2));
			return Uncertainty.uniform(min, max);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchTriangle(Matcher m) {
		try {
			double min = parseValue(m.group(1));
			double mode = parseValue(m.group(2));
			double max = parseValue(m.group(3));
			return Uncertainty.triangle(min, mode, max);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchNormal(Matcher m) {
		try {
			double mean = parseValue(m.group(1));
			double sigma = parseValue(m.group(2));
			return Uncertainty.normal(mean, sigma);
		} catch (Exception e) {
			return null;
		}
	}

	private Uncertainty matchLogNormal(Matcher m) {
		try {
			double gmean = parseValue(m.group(1));
			double gsigma = parseValue(m.group(2));
			return Uncertainty.logNormal(gmean, gsigma);
		} catch (Exception e) {
			return null;
		}
	}
}

package org.openlca.app.editors.parameters.clipboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.openlca.core.model.Parameter;

import com.google.common.base.Strings;

public class Clipboard {

	private String text;
	private final UncertaintyParser uncertainties = new UncertaintyParser();

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
		p.setRefId(UUID.randomUUID().toString());
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
		p.setRefId(UUID.randomUUID().toString());
		p.setInputParameter(true);
		p.setName(name);
		p.setValue(val);
		if (fields.length > 2)
			p.setUncertainty(uncertainties.read(fields[2]));
		if (fields.length > 3)
			p.setDescription(fields[3]);
		return p;
	}

	static double parseValue(String field) {
		if (field == null)
			return Double.NaN;
		try {
			String f = field.replace(',', '.');
			return Double.parseDouble(f);
		} catch (Exception e) {
			return Double.NaN;
		}
	}

}

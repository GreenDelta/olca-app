package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;

import com.google.common.base.Strings;

class Clipboard {

	static List<Parameter> readAsInputParams(String text, ParameterScope scope) {
		ClipboardText ct = ClipboardText.split(text);
		List<Parameter> params = readParams(ct);
		for (Parameter param : params) {
			param.scope = scope;
			param.isInputParameter = true;
			param.formula = null;
		}
		return params;
	}

	static List<Parameter> readAsCalculatedParams(String text, ParameterScope scope) {
		ClipboardText ct = ClipboardText.split(text);
		List<Parameter> params = readParams(ct);
		for (Parameter param : params) {
			param.scope = scope;
			param.isInputParameter = false;
			if (param.formula == null) {
				param.formula = Double.toString(param.value);
			}
			param.uncertainty = null;
		}
		return params;
	}

	private static List<Parameter> readParams(ClipboardText text) {
		List<Parameter> list = new ArrayList<>();
		for (String[] row : text.rows) {
			Parameter param = text.forInputParameters
					? readInputParam(row)
					: readCalcParam(row);
			if (param != null) {
				list.add(param);
			}
		}
		return list;
	}

	private static Parameter readCalcParam(String[] row) {
		if (row == null || row.length < 2)
			return null;
		String name = row[0];
		String formula = row[1];
		if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(formula))
			return null;
		Parameter p = new Parameter();
		p.refId = UUID.randomUUID().toString();
		p.isInputParameter = false;
		p.name = name;
		p.formula = formula;
		if (row.length > 2)
			p.value = readDouble(row[2]);
		if (row.length > 3)
			p.description = row[3];
		return p;
	}

	private static Parameter readInputParam(String[] fields) {
		if (fields == null || fields.length < 2)
			return null;
		String name = fields[0];
		if (Strings.isNullOrEmpty(name))
			return null;
		double val = readDouble(fields[1]);
		Parameter p = new Parameter();
		p.refId = UUID.randomUUID().toString();
		p.isInputParameter = true;
		p.name = name;
		p.value = val;
		if (fields.length > 2)
			p.uncertainty = Uncertainty.fromString(fields[2]);
		if (fields.length > 3)
			p.description = fields[3];
		return p;
	}

	static double readDouble(String field) {
		if (field == null)
			return 0.0;
		try {
			String f = field.replace(',', '.');
			return Double.parseDouble(f);
		} catch (Exception e) {
			return 0.0;
		}
	}

}

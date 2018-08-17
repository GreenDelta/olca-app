package org.openlca.app.editors.parameters.clipboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;

import com.google.common.base.Strings;

public class Clipboard {

	private final ClipboardText text;
	private final UncertaintyParser uncertainties = new UncertaintyParser();

	private Clipboard(ClipboardText text) {
		this.text = text;
	}

	public static List<Parameter> readAsInputParams(String text, ParameterScope scope) {
		ClipboardText ct = ClipboardText.split(text);
		List<Parameter> params = new Clipboard(ct).readParams();
		for (Parameter param : params) {
			param.setScope(scope);
			param.setInputParameter(true);
			param.setFormula(null);
		}
		return params;
	}

	public static List<Parameter> readAsCalculatedParams(String text, ParameterScope scope) {
		ClipboardText ct = ClipboardText.split(text);
		List<Parameter> params = new Clipboard(ct).readParams();
		for (Parameter param : params) {
			param.setScope(scope);
			param.setInputParameter(false);
			param.setUncertainty(null);
		}
		return params;
	}

	private List<Parameter> readParams() {
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

	private Parameter readCalcParam(String[] row) {
		if (row == null || row.length < 2)
			return null;
		String name = row[0];
		String formula = row[1];
		if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(formula))
			return null;
		Parameter p = new Parameter();
		p.setRefId(UUID.randomUUID().toString());
		p.setInputParameter(false);
		p.setName(name);
		p.setFormula(formula);
		if (row.length > 2)
			p.setValue(readDouble(row[2]));
		if (row.length > 3)
			p.setDescription(row[3]);
		return p;
	}

	private Parameter readInputParam(String[] fields) {
		if (fields == null || fields.length < 2)
			return null;
		String name = fields[0];
		if (Strings.isNullOrEmpty(name))
			return null;
		double val = readDouble(fields[1]);
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

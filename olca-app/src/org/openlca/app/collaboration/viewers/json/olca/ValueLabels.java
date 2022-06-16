package org.openlca.app.collaboration.viewers.json.olca;

import java.text.DateFormat;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.util.Labels;
import org.openlca.jsonld.Json;

class ValueLabels {

	private static DateFormat timestampFormatter = DateFormat.getDateTimeInstance();
	private static DateFormat dateFormatter = DateFormat.getDateInstance();

	static String get(JsonNode node, Side side) {
		var value = ModelUtil.valueOf(node, side);
		if (value == null)
			return "null";
		var property = node.property;
		if (EnumFields.isEnum(property)) {
			var enumValue = EnumFields.getEnum(property, value);
			value = Labels.getEnumText(enumValue);
		} else if (DateFields.isDateOrTimestamp(property)) {
			var date = Json.parseDate(value);
			if (date != null) {
				var formatter = DateFields.isDate(property)
						? dateFormatter
						: timestampFormatter;
				value = formatter.format(date);
			}
		}
		if (value == null)
			return "null";
		if (property.equals("isInputParameter"))
			return getInputParameterValue(value);
		if (property.equals("isInput"))
			return getInputValue(value);
		if (value.equalsIgnoreCase("true"))
			return "Yes";
		if (value.equalsIgnoreCase("false"))
			return "No";
		return value;
	}

	private static String getInputParameterValue(String value) {
		if (value.equalsIgnoreCase("true"))
			return M.InputParameter;
		return M.DependenantParameter;
	}

	private static String getInputValue(String value) {
		if (value.equalsIgnoreCase("true"))
			return M.Input;
		return M.Output;
	}

}

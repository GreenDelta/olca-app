package org.openlca.app.cloud.ui.compare;

import java.text.DateFormat;
import java.util.Date;

import org.openlca.app.M;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Parameter;
import org.openlca.jsonld.Dates;

import com.google.gson.JsonElement;

class ValueLabels {

	private static DateFormat dateFormatter = DateFormat.getDateTimeInstance();

	static String get(String property, JsonElement element, JsonElement parent,
			String value) {
		if (value == null)
			return "null";
		if (EnumFields.isEnum(parent, property)) {
			Object enumValue = EnumFields.getEnum(parent, property, value);
			value = Labels.getEnumText(enumValue);
		} else if (DateFields.isTimestamp(parent, property)) {
			Date date = Dates.fromString(value);
			if (date != null)
				value = dateFormatter.format(date);
		}
		if (value == null)
			return "null";
		if (isInputParameterField(property, parent))
			return getInputParameterValue(value);
		if (isInputField(property, parent)) {
			return getInputValue(value);
		}
		if (value.equalsIgnoreCase("true"))
			return "Yes";
		if (value.equalsIgnoreCase("false"))
			return "No";
		return value;
	}

	private static boolean isInputParameterField(String property, JsonElement parent) {
		return ModelUtil.isType(parent, Parameter.class) && property.equals("inputParameter");
	}

	private static String getInputParameterValue(String value) {
		if (value.equalsIgnoreCase("true"))
			return M.InputParameter;
		return M.DependenantParameter;
	}

	private static boolean isInputField(String property, JsonElement parent) {
		return ModelUtil.isType(parent, Exchange.class)	&& property.equals("input");
	}

	private static String getInputValue(String value) {
		if (value.equalsIgnoreCase("true"))
			return M.Input;
		return M.Output;
	}

}

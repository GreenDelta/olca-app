package org.openlca.app.editors.reports;

import org.openlca.core.model.ParameterRedef;

public class ReportParameter {

	private ParameterRedef redef;
	private String userFriendlyName;
	private double value;
	private double defaultValue;
	private String description;

	public ParameterRedef getRedef() {
		return redef;
	}

	public void setRedef(ParameterRedef redef) {
		this.redef = redef;
	}

	public String getUserFriendlyName() {
		return userFriendlyName;
	}

	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(double defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

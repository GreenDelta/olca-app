package org.openlca.app.editors.reports;

import org.openlca.core.model.ProjectVariant;

public class ReportVariant {

	private ProjectVariant variant;
	private String userFriendlyName = "";
	private String description = "";

	public ProjectVariant getVariant() {
		return variant;
	}

	public void setVariant(ProjectVariant variant) {
		this.variant = variant;
	}

	public String getUserFriendlyName() {
		return userFriendlyName;
	}

	public void setUserFriendlyName(String userFriendlyName) {
		this.userFriendlyName = userFriendlyName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

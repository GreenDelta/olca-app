package org.openlca.app.editors.reports.model;

import org.openlca.core.model.ParameterRedef;

import java.util.HashMap;
import java.util.Map;

public class ReportParameter {

	private ParameterRedef redef;
	private String name;
	private String description;
	private Map<Integer, Double> variantValues = new HashMap<>();

	public ParameterRedef getRedef() {
		return redef;
	}

	public void setRedef(ParameterRedef redef) {
		this.redef = redef;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void putValue(int variantId, double value) {
		variantValues.put(variantId, value);
	}

	public void removeValue(int variantId) {
		variantValues.remove(variantId);
	}

}

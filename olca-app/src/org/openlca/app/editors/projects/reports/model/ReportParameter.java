package org.openlca.app.editors.projects.reports.model;

import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ParameterRedef;

public class ReportParameter {

	public ParameterRedef redef;
	public String name;
	public String description;
	public Map<Integer, Double> variantValues = new HashMap<>();

	public void putValue(int variantId, double value) {
		variantValues.put(variantId, value);
	}

	public void removeValue(int variantId) {
		variantValues.remove(variantId);
	}

}

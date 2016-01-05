package org.openlca.app.editors.reports.model;

import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * Wraps an LCIA category for a report with additional information.
 */
public class ReportIndicator {

	public final int id;
	public ImpactCategoryDescriptor descriptor;
	public String reportName;
	public String reportDescription;
	public boolean displayed;
	public Double normalisationFactor;
	public Double weightingFactor;

	public ReportIndicator(int id) {
		this.id = id;
	}

}

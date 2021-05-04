package org.openlca.app.editors.projects.reports.model;

import org.openlca.core.model.descriptors.ImpactDescriptor;

/**
 * Wraps an LCIA category for a report with additional information.
 */
public class ReportIndicator {

	public final int id;
	public ImpactDescriptor descriptor;
	public String reportName;
	public String reportDescription;
	public boolean displayed;
	public Double normalisationFactor;
	public Double weightingFactor;

	public ReportIndicator(int id) {
		this.id = id;
	}

}

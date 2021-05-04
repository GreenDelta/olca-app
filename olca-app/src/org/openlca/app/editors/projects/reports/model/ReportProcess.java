package org.openlca.app.editors.projects.reports.model;

import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ReportProcess {

	public final long id;
	public final ProcessDescriptor descriptor;
	public String reportName;
	public String reportDescription;

	public ReportProcess(ProcessDescriptor d) {
		this.id = d.id;
		this.descriptor = d;
		this.reportName = d.name;
		this.reportDescription = d.description;
	}
}

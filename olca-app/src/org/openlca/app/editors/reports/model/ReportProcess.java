package org.openlca.app.editors.reports.model;

import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ReportProcess {

	public final long id;
	public final ProcessDescriptor descriptor;
	public String reportName;
	public String reportDescription;

	public ReportProcess(ProcessDescriptor descriptor) {
		this.id = descriptor.getId();
		this.descriptor = descriptor;
		this.reportName = descriptor.getName();
		this.reportDescription = descriptor.getDescription();
	}
}

package org.openlca.app.editors.reports.model;

import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ReportProcess {

	private long id;
	private ProcessDescriptor descriptor;
	private String reportName;
	private String reportDescription;

	public ReportProcess(ProcessDescriptor descriptor) {
		this.id = descriptor.getId();
		this.descriptor = descriptor;
		this.reportName = descriptor.getName();
		this.reportDescription = descriptor.getDescription();
	}

	public long getId() {
		return id;
	}

	public ProcessDescriptor getDescriptor() {
		return descriptor;
	}

	public String getReportName() {
		return reportName;
	}

	public String getReportDescription() {
		return reportDescription;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public void setReportDescription(String reportDescription) {
		this.reportDescription = reportDescription;
	}
}

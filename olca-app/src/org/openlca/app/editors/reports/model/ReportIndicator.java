package org.openlca.app.editors.reports.model;

import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * Wraps an LCIA category for a report with additional information.
 */
public class ReportIndicator {

	private final int id;
	private ImpactCategoryDescriptor descriptor;
	private String reportName;
	private String reportDescription;
	private boolean displayed;
	private Double normalisationFactor;
	private Double weightingFactor;

	public ReportIndicator(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public ImpactCategoryDescriptor getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(ImpactCategoryDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getReportDescription() {
		return reportDescription;
	}

	public void setReportDescription(String reportDescription) {
		this.reportDescription = reportDescription;
	}

	public boolean isDisplayed() {
		return displayed;
	}

	public void setDisplayed(boolean displayed) {
		this.displayed = displayed;
	}

	public void setNormalisationFactor(Double normalisationFactor) {
		this.normalisationFactor = normalisationFactor;
	}

	public Double getNormalisationFactor() {
		return normalisationFactor;
	}

	public void setWeightingFactor(Double weightingFactor) {
		this.weightingFactor = weightingFactor;
	}

	public Double getWeightingFactor() {
		return weightingFactor;
	}

}

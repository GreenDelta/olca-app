package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.ProjectDescriptor;

public class Report {

	private String title;
	private ProjectDescriptor project;
	private List<ReportSection> sections = new ArrayList<>();
	private List<ReportParameter> parameters = new ArrayList<>();
	private List<ReportVariant> variants = new ArrayList<>();
	private List<ReportResult> results = new ArrayList<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ProjectDescriptor getProject() {
		return project;
	}

	public void setProject(ProjectDescriptor project) {
		this.project = project;
	}

	public List<ReportSection> getSections() {
		return sections;
	}

	public List<ReportParameter> getParameters() {
		return parameters;
	}

	public List<ReportVariant> getVariants() {
		return variants;
	}

	public List<ReportResult> getResults() {
		return results;
	}
}

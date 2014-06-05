package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Project;

public class Report {

	private String title;
	private transient Project project;
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

	public void setProject(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
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

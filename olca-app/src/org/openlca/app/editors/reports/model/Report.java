package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

public class Report {

	private String title;
	private boolean withNormalisation;
	private boolean withWeighting;
	private List<ReportSection> sections = new ArrayList<>();
	private List<ReportParameter> parameters = new ArrayList<>();
	private List<ReportVariant> variants = new ArrayList<>();
	private List<ReportResult> results = new ArrayList<>();
	private List<ReportIndicator> indicators = new ArrayList<>();
	private List<ReportProcess> processes = new ArrayList<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public List<ReportIndicator> getIndicators() {
		return indicators;
	}

	public List<ReportProcess> getProcesses() {
		return processes;
	}

	public void setWithNormalisation(boolean withNormalisation) {
		this.withNormalisation = withNormalisation;
	}

	public boolean isWithWeighting() {
		return withWeighting;
	}

	public void setWithWeighting(boolean withWeighting) {
		this.withWeighting = withWeighting;
	}

	public boolean isWithNormalisation() {
		return withNormalisation;
	}

}

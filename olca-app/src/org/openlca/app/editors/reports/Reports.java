package org.openlca.app.editors.reports;

import org.openlca.core.model.Project;

public final class Reports {

	private Reports() {
	}

	public static Report create(Project project) {
		Report report = new Report();
		if (project == null) {
			report.setTitle("No project");
			return report;
		}
		report.setProject(project);
		report.setTitle(project.getName());
		createDefaultSections(report);
		return report;
	}

	private static void createDefaultSections(Report report) {
		String[] headers = { "Introduction", "Functional Unit",
				"System boundaries", "Impact assessment method", "Results",
				"Assumptions and uncertainties", "Discussion and conclusions" };
		for (String header : headers) {
			ReportSection section = new ReportSection();
			section.setTitle(header);
			section.setText("TODO: add some text here");
			report.getSections().add(section);
		}
	}
}

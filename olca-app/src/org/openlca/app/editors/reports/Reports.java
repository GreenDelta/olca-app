package org.openlca.app.editors.reports;

import org.openlca.app.App;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportCalculator;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.core.model.Project;

public final class Reports {

	private Reports() {
	}

	public static void createAndOpen(Project project) {
		final Report report = new Report();
		createDefaultSections(report);
		if (project == null) {
			report.setTitle("No project");
			ReportEditor.open(report);
			return;
		}
		report.setProject(project);
		report.setTitle(project.getName());
		App.run("Calculate results",
				new ReportCalculator(project, report),
				() -> ReportEditor.open(report));
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

package org.openlca.app.editors.reports;

import org.openlca.app.App;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportCalculator;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;

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
		createVariants(project, report);
		App.run("Calculate results",
				new ReportCalculator(project, report),
				() -> ReportEditor.open(report));
	}

	private static void createVariants(Project project, Report report) {
		if (project == null)
			return;
		for (ProjectVariant variant : project.getVariants()) {
			ReportVariant reportVariant = new ReportVariant();
			reportVariant.setName(variant.getName());
			reportVariant.setUserFriendlyName(variant.getName());
			report.getVariants().add(reportVariant);
		}
	}

	private static void createDefaultSections(Report report) {
		String[] headers = { "Introduction", "Functional Unit",
				"System boundaries", "Impact assessment method", "Results",
				"Assumptions and uncertainties", "Discussion and conclusions" };
		for (int i = 0; i < headers.length; i++) {
			ReportSection section = new ReportSection();
			section.setIndex(i);
			section.setTitle(headers[i]);
			section.setText("TODO: add some text here");
			report.getSections().add(section);
		}
	}
}

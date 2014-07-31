package org.openlca.app.editors.reports;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportComponent;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public final class Reports {

	private Reports() {
	}

	public static Report createOrOpen(Project project) {
		Report report = openReport(project);
		return report != null ? report : createNew(project);
	}

	private static Report createNew(Project project) {
		Report report = new Report();
		createDefaultSections(report);
		if (project == null) {
			report.setTitle("No project");
			return report;
		}
		report.setProject(Descriptors.toDescriptor(project));
		report.setTitle("Results of project '" + project.getName() + "'");
		createVariants(project, report);
		return report;
	}

	private static Report openReport(Project project) {
		if (project == null)
			return null;
		File file = getReportFile(project.getRefId());
		if (file == null || !file.exists())
			return null;
		try (FileInputStream fis = new FileInputStream(file);
				Reader reader = new InputStreamReader(fis, "utf-8")) {
			Gson gson = new Gson();
			return gson.fromJson(reader, Report.class);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Reports.class);
			log.error("failed to open report file " + file, e);
			return null;
		}
	}

	public static void save(Report report) {
		if (report == null || report.getProject() == null)
			return;
		Logger log = LoggerFactory.getLogger(Reports.class);
		File file = getReportFile(report.getProject().getRefId());
		if (file == null) {
			log.error("failed to get report file {} for {}" + file, report);
			return;
		}
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		log.trace("write report {} to file {}", report, file);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			Gson gson = new Gson();
			String json = gson.toJson(report);
			IOUtils.write(json, fos, "utf-8");
		} catch (Exception e) {
			log.error("failed to write report file", e);
		}

	}

	private static File getReportFile(String projectId) {
		if (projectId == null)
			return null;
		File dir = DatabaseFolder.getFileStorageLocation(Database.get());
		if (dir == null)
			return null;
		dir = new File(dir, "projects");
		dir = new File(dir, projectId);
		return new File(dir, "report.json");
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
		report.getSections().add(createIntroSection());
		report.getSections().add(createVariantsSection());
		report.getSections().add(createMethodSection());
	}

	private static ReportSection createIntroSection() {
		ReportSection section = new ReportSection();
		section.setIndex(0);
		section.setTitle("Introduction");
		String text = "In the following the results of the project 'new' are shown. "
				+ "This is a default template for the report of the project results. "
				+ "You can configure template this via the project editor by \n"
				+ "\n"
				+ "<p><ul>\n"
				+ "<li>changing the text of the sections, \n"
				+ "<li>adding or removing sections, \n"
				+ "<li>and selecting visual components that should be shown. \n"
				+ "</ul></p>\n"
				+ "\n"
				+ "Note that you can also use HTML elements to format the section texts. "
				+ "Additionally, you can export this report as an HTML page using "
				+ "the export button in the toolbar of the report view.";
		section.setText(text);
		return section;
	}

	private static ReportSection createVariantsSection() {
		ReportSection section = new ReportSection();
		section.setIndex(1);
		section.setTitle("Project variants");
		String text = "The following table shows the name and description of " +
				"the different variants from the project setup.";
		section.setText(text);
		section.setComponentId(ReportComponent.VARIANT_TABLE.getId());
		return section;
	}

	private static ReportSection createMethodSection() {
		ReportSection section = new ReportSection();
		section.setIndex(2);
		section.setTitle("LCIA method");
		String text = "The table below shows the LCIA categories of the selected"
				+ " LCIA method for the project. Only the LCIA categories that are"
				+ " selected to be displayed are shown in the report. Additionally, "
				+ "a user friendly name and a description for the report can be provided.";
		section.setText(text);
		section.setComponentId(ReportComponent.INDICATOR_TABLE.getId());
		return section;
	}

}

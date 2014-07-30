package org.openlca.app.editors.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

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
		report.setTitle(project.getName());
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

package org.openlca.app.editors.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportComponent;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.ParameterRedef;
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
		createReportVariants(project, report);
		report.setProject(Descriptors.toDescriptor(project));
		report.setTitle("Results of project '" + project.getName() + "'");
		return report;
	}

	private static void createReportVariants(Project project, Report report) {
		if (project == null || report == null)
			return;
		int id = 0;
		for (ProjectVariant projectVar : project.getVariants()) {
			ReportVariant reportVar = new ReportVariant(id++);
			reportVar.setName(projectVar.getName());
			report.getVariants().add(reportVar);
			for (ParameterRedef redef : projectVar.getParameterRedefs()) {
				ReportParameter param = findOrCreateParameter(redef, report);
				param.putValue(reportVar.getId(), redef.getValue());
			}
		}
	}

	private static ReportParameter findOrCreateParameter(ParameterRedef redef,
			Report report) {
		for (ReportParameter parameter : report.getParameters()) {
			ParameterRedef reportRedef = parameter.getRedef();
			if (reportRedef == null)
				continue;
			if (Objects.equals(redef.getName(), reportRedef.getName())
					&& Objects.equals(redef.getContextId(),
							reportRedef.getContextId()))
				return parameter;
		}
		ReportParameter parameter = new ReportParameter();
		report.getParameters().add(parameter);
		parameter.setName(redef.getName());
		parameter.setRedef(redef);
		return parameter;
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

	private static void createDefaultSections(Report report) {
		report.getSections().add(createIntroSection(0));
		report.getSections().add(createVariantsSection(1));
		report.getSections().add(createMethodSection(2));
		report.getSections().add(createResultTableSection(3));
		report.getSections().add(createProcessContributionSection(4));
	}

	private static ReportSection createIntroSection(int idx) {
		ReportSection section = new ReportSection();
		section.setIndex(idx);
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

	private static ReportSection createVariantsSection(int idx) {
		ReportSection section = new ReportSection();
		section.setIndex(idx);
		section.setTitle("Project variants");
		String text = "The following table shows the name and description of " +
				"the different variants from the project setup.";
		section.setText(text);
		section.setComponentId(
				ReportComponent.VARIANT_DESCRIPTION_TABLE.getId());
		return section;
	}

	private static ReportSection createMethodSection(int idx) {
		ReportSection section = new ReportSection();
		section.setIndex(idx);
		section.setTitle("LCIA method");
		String text = "The table below shows the LCIA categories of the selected"
				+ " LCIA method for the project. Only the LCIA categories that are"
				+ " selected to be displayed are shown in the report. Additionally, "
				+ "a user friendly name and a description for the report can be provided.";
		section.setText(text);
		section.setComponentId(
				ReportComponent.INDICATOR_DESCRIPTION_TABLE.getId());
		return section;
	}

	private static ReportSection createResultTableSection(int idx) {
		ReportSection section = new ReportSection();
		section.setIndex(idx);
		section.setTitle("LCIA Results");
		String text = "The following table shows the LCIA results of the project.";
		section.setText(text);
		section.setComponentId(ReportComponent.IMPACT_RESULT_TABLE.getId());
		return section;
	}

	private static ReportSection createProcessContributionSection(int idx) {
		ReportSection section = new ReportSection();
		section.setIndex(idx);
		section.setTitle("Process contributions");
		String text = "The chart below shows the top process contributions of "
				+ "the project variants to the selected LCIA category";
		section.setText(text);
		section.setComponentId(
				ReportComponent.PROCESS_CONTRIBUTION_CHARTS.getId());
		return section;
	}

}

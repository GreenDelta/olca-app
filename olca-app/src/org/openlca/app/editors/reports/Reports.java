package org.openlca.app.editors.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.openlca.app.M;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportComponent;
import org.openlca.app.editors.reports.model.ReportIndicator;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public final class Reports {

	private Reports() {
	}

	public static Report createOrOpen(Project project, IDatabase db) {
		Report report = openReport(project, db);
		return report != null ? report : createNew(project, db);
	}

	private static Report createNew(Project project, IDatabase db) {
		Report report = new Report();
		createDefaultSections(report);
		if (project == null) {
			report.title = "No project";
			return report;
		}
		createReportVariants(project, report);
		createReportIndicators(project, report, db);
		report.title = M.ResultsOfProject + " " + project.name;
		return report;
	}

	private static void createReportVariants(Project project, Report report) {
		if (project == null || report == null)
			return;
		int id = 0;
		for (ProjectVariant projectVar : project.variants) {
			ReportVariant reportVar = new ReportVariant(id++);
			reportVar.name = projectVar.name;
			report.variants.add(reportVar);
			for (ParameterRedef redef : projectVar.parameterRedefs) {
				ReportParameter param = findOrCreateParameter(redef, report);
				param.putValue(reportVar.id, redef.value);
			}
		}
	}

	private static ReportParameter findOrCreateParameter(ParameterRedef redef,
			Report report) {
		for (ReportParameter parameter : report.parameters) {
			ParameterRedef reportRedef = parameter.redef;
			if (reportRedef == null)
				continue;
			if (Objects.equals(redef.name, reportRedef.name)
					&& Objects.equals(redef.contextId,
							reportRedef.contextId))
				return parameter;
		}
		ReportParameter parameter = new ReportParameter();
		report.parameters.add(parameter);
		parameter.name = redef.name;
		parameter.redef = redef;
		return parameter;
	}

	private static void createReportIndicators(Project project, Report report,
			IDatabase database) {
		if (project.impactMethodId == null)
			return;
		ImpactMethodDao dao = new ImpactMethodDao(database);
		List<ImpactCategoryDescriptor> descriptors = dao
				.getCategoryDescriptors(project.impactMethodId);
		int id = 0;
		for (ImpactCategoryDescriptor d : descriptors) {
			ReportIndicator i = new ReportIndicator(id++);
			i.descriptor = d;
			i.reportName = d.name;
			i.displayed = true;
			report.indicators.add(i);
		}
	}

	private static Report openReport(Project project, IDatabase db) {
		if (project == null)
			return null;
		File file = getReportFile(project, db);
		if (file == null || !file.exists())
			return null;
		try (FileInputStream fis = new FileInputStream(file);
				Reader reader = new InputStreamReader(fis, "utf-8")) {
			Report r = new Gson().fromJson(reader, Report.class);
			return r;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Reports.class);
			log.error("failed to open report file " + file, e);
			return null;
		}
	}

	public static void save(Project project, Report report, IDatabase database) {
		if (report == null || project == null)
			return;
		Logger log = LoggerFactory.getLogger(Reports.class);
		File file = getReportFile(project, database);
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

	private static File getReportFile(Project project, IDatabase database) {
		if (project == null)
			return null;
		File dir = DatabaseDir.getDir(project);
		if (dir == null)
			return null;
		return new File(dir, "report.json");
	}

	private static void createDefaultSections(Report report) {
		report.sections.add(createIntroSection(0));
		ReportComponent[] components = {
				ReportComponent.VARIANT_DESCRIPTION_TABLE,
				ReportComponent.INDICATOR_DESCRIPTION_TABLE,
				ReportComponent.IMPACT_RESULT_TABLE,
				ReportComponent.INDICATOR_BAR_CHART,
				ReportComponent.PROCESS_CONTRIBUTION_CHART,
				ReportComponent.RELATIVE_INDICATOR_BAR_CHART
		};
		try {
			Properties props = new Properties();
			props.load(Reports.class
					.getResourceAsStream("default_sections.properties"));
			int idx = 1;
			for (ReportComponent component : components) {
				report.sections.add(createSection(idx, props, component));
				idx++;
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Reports.class);
			log.error("failed to create default report sections", e);
		}
	}

	private static ReportSection createIntroSection(int idx) {
		ReportSection section = new ReportSection();
		section.index = idx;
		section.title = "Introduction";
		String text = "In the following the results of the project are shown. "
				+ "This is a default template for the report of the project results. "
				+ "You can configure this template via the project editor by \n"
				+ "\n"
				+ "<p><ul>\n"
				+ "<li>changing the text of the sections, \n"
				+ "<li>adding or removing sections, \n"
				+ "<li>moving sections around, \n"
				+ "<li>and selecting visual components that should be shown. \n"
				+ "</ul></p>\n"
				+ "\n"
				+ "Note that you can also use HTML elements to format the section texts. "
				+ "Additionally, you can export this report as an HTML page using "
				+ "the export button in the toolbar of the report view.";
		section.text = text;
		return section;
	}

	private static ReportSection createSection(int idx, Properties props,
			ReportComponent component) {
		ReportSection section = new ReportSection();
		section.index = idx;
		section.title = props.getProperty(component.name() + ".title");
		section.text = props.getProperty(component.name() + ".text");
		section.componentId = component.getId();
		return section;
	}
}

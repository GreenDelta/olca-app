package org.openlca.app.editors.projects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;

class ReportParameterSync {

	private final ProjectEditor editor;

	public ReportParameterSync(ProjectEditor editor) {
		this.editor = editor;
		initSync();
	}

	private void initSync() {
		Report report = editor.getReport();
		Project project = editor.getModel();
		if (report == null || project == null)
			return;
		List<ParameterRedef> unSynced = getUnSyncedRedefs(project, report);
		for (ParameterRedef redef : unSynced) {
			ReportParameter param = initParam(redef, report);
			for (ProjectVariant projectVariant : project.variants) {
				ParameterRedef varRedef = getVariantRedef(redef, projectVariant);
				ReportVariant reportVariant = getReportVariant(projectVariant,
						report);
				if (varRedef == null || reportVariant == null)
					continue;
				param.putValue(reportVariant.id, varRedef.value);
			}
		}
	}

	private List<ParameterRedef> getUnSyncedRedefs(Project project,
			Report report) {
		if (project.variants.isEmpty())
			return Collections.emptyList();
		ProjectVariant firstVar = project.variants.get(0);
		List<ParameterRedef> unSynced = new ArrayList<>();
		for (ParameterRedef redef : firstVar.parameterRedefs) {
			ReportParameter parameter = getReportParameter(redef);
			if (parameter == null)
				unSynced.add(redef);
		}
		return unSynced;
	}

	private ReportParameter initParam(ParameterRedef redef, Report report) {
		ReportParameter param = new ReportParameter();
		report.parameters.add(param);
		param.name = redef.name;
		param.redef = redef;
		return param;
	}

	private ParameterRedef getVariantRedef(ParameterRedef redef,
			ProjectVariant var) {
		if (redef == null || var == null)
			return null;
		for (ParameterRedef varRedef : var.parameterRedefs) {
			if (equal(redef, varRedef))
				return varRedef;
		}
		return null;
	}

	public String getName(ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		return param == null ? null : param.name;
	}

	public void setName(String name, ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		if (param == null)
			return;
		param.name = name;
	}

	public String getDescription(ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		return param == null ? null : param.description;
	}

	public void setDescription(String description, ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		if (param == null)
			return;
		param.description = description;
	}

	public void parameterAdded(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return;
		ReportParameter param = getReportParameter(redef);
		if (param != null)
			return;
		param = new ReportParameter();
		report.parameters.add(param);
		param.name = redef.name;
		param.redef = redef;
		for (ReportVariant variant : report.variants)
			param.putValue(variant.id, redef.value);
	}

	public void parameterRemoved(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return;
		ReportParameter param = getReportParameter(redef);
		if (param != null)
			report.parameters.remove(param);
	}

	public void valueChanged(ParameterRedef redef, ProjectVariant variant,
			double value) {
		Report report = editor.getReport();
		ReportParameter parameter = getReportParameter(redef);
		ReportVariant reportVariant = getReportVariant(variant, report);
		if (reportVariant == null || parameter == null)
			return;
		parameter.putValue(reportVariant.id, value);
	}

	private ReportVariant getReportVariant(ProjectVariant projectVariant,
			Report report) {
		if (projectVariant == null || report == null)
			return null;
		for (ReportVariant reportVariant : report.variants) {
			if (Objects.equals(
					reportVariant.name, projectVariant.name))
				return reportVariant;
		}
		return null;
	}

	private ReportParameter getReportParameter(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return null;
		for (ReportParameter param : report.parameters) {
			if (equal(param.redef, redef))
				return param;
		}
		return null;
	}

	private boolean equal(ParameterRedef r1, ParameterRedef r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 == null || r2 == null)
			return false;
		return Objects.equals(r1.name, r2.name)
				&& Objects.equals(r1.contextType, r2.contextType)
				&& Objects.equals(r1.contextId, r2.contextId);
	}
}

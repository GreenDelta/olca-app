package org.openlca.app.editors.projects;

import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProjectVariant;

import java.util.Objects;

class ReportParameterSync {

	private final ProjectEditor editor;

	public ReportParameterSync(ProjectEditor editor) {
		this.editor = editor;
	}

	public String getName(ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		return param == null ? null : param.getName();
	}

	public void setName(String name, ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		if (param == null)
			return;
		param.setName(name);
	}

	public String getDescription(ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		return param == null ? null : param.getDescription();
	}

	public void setDescription(String description, ParameterRedef redef) {
		ReportParameter param = getReportParameter(redef);
		if (param == null)
			return;
		param.setDescription(description);
	}

	public void parameterAdded(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return;
		ReportParameter param = getReportParameter(redef);
		if (param != null)
			return;
		param = new ReportParameter();
		report.getParameters().add(param);
		param.setName(redef.getName());
		param.setRedef(redef);
		for (ReportVariant variant : report.getVariants())
			param.putValue(variant.getId(), redef.getValue());
	}

	public void parameterRemoved(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return;
		ReportParameter param = getReportParameter(redef);
		if (param != null)
			report.getParameters().remove(param);
	}

	public void valueChanged(ParameterRedef redef, ProjectVariant variant,
			double value) {
		Report report = editor.getReport();
		ReportParameter parameter = getReportParameter(redef);
		if (report == null || parameter == null)
			return;
		for (ReportVariant var : report.getVariants()) {
			if (Objects.equals(variant.getName(), var.getName())) {
				parameter.putValue(var.getId(), value);
				break;
			}
		}
	}

	private ReportParameter getReportParameter(ParameterRedef redef) {
		Report report = editor.getReport();
		if (report == null || redef == null)
			return null;
		for (ReportParameter param : report.getParameters()) {
			if (equal(param.getRedef(), redef))
				return param;
		}
		return null;
	}

	private boolean equal(ParameterRedef r1, ParameterRedef r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 == null || r2 == null)
			return false;
		return Objects.equals(r1.getName(), r2.getName())
				&& Objects.equals(r1.getContextType(), r2.getContextType())
				&& Objects.equals(r1.getContextId(), r2.getContextId());
	}
}

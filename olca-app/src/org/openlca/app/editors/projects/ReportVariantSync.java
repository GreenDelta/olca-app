package org.openlca.app.editors.projects;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.ProjectVariant;

/**
 * Synchronizes the variant information that are shared between project and
 * report variants.
 */
class ReportVariantSync {

	private final ProjectEditor editor;

	public ReportVariantSync(ProjectEditor editor) {
		this.editor = editor;
	}

	public void variantAdded(ProjectVariant variant) {
		Report report = editor.getReport();
		if (report == null || variant == null)
			return;
		TreeSet<Integer> existingIds = new TreeSet<>();
		for (ReportVariant var : report.variants)
			existingIds.add(var.id);
		int newId = 0;
		while (existingIds.contains(newId))
			newId++;
		ReportVariant var = new ReportVariant(newId);
		var.name = variant.name;
		report.variants.add(var);
		addParameterValues(newId, report);
	}

	private void addParameterValues(int newId, Report report) {
		for (ReportParameter parameter : report.parameters) {
			if (parameter.redef == null)
				parameter.putValue(newId, 0);
			else
				parameter.putValue(newId, parameter.redef.value);
		}
	}

	public void variantsRemoved(List<ProjectVariant> variants) {
		Report report = editor.getReport();
		if (report == null || variants == null)
			return;
		for (ProjectVariant variant : variants) {
			ReportVariant var = findReportVariant(variant);
			if (var == null)
				continue;
			report.variants.remove(var);
			for (ReportParameter parameter : report.parameters)
				parameter.removeValue(var.id);
		}
	}

	/**
	 * Sets the given name in the project and report variant. Note that the given
	 * project variant should contain the old variant name because this is used to
	 * find the respective report variant.
	 */
	public void updateName(ProjectVariant variant, String newName) {
		ReportVariant var = findReportVariant(variant);
		if (var != null)
			var.name = newName;
		variant.name = newName;
	}

	public void updateDescription(ProjectVariant variant, String description) {
		ReportVariant var = findReportVariant(variant);
		if (var != null)
			var.description = description;
	}

	public void updateDisabled(ProjectVariant pvar) {
		ReportVariant rvar = findReportVariant(pvar);
		if (rvar != null) {
			rvar.isDisabled = pvar.isDisabled;
		}
	}

	public String getDescription(ProjectVariant variant) {
		ReportVariant var = findReportVariant(variant);
		return var == null ? null : var.description;
	}

	private ReportVariant findReportVariant(ProjectVariant variant) {
		if (editor.getReport() == null || variant == null)
			return null;
		for (ReportVariant reportVariant : editor.getReport().variants) {
			if (Objects.equals(variant.name, reportVariant.name))
				return reportVariant;
		}
		return null;
	}
}

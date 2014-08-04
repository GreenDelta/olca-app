package org.openlca.app.editors.projects;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.core.model.ProjectVariant;

/**
 * Synchronizes the variant information that are shared between project and
 * report variants.
 */
class VariantSync {

	private final ProjectEditor editor;

	public VariantSync(ProjectEditor editor) {
		this.editor = editor;
	}

	public void variantAdded(ProjectVariant variant) {
		Report report = editor.getReport();
		if (report == null || variant == null)
			return;
		TreeSet<Integer> existingIds = new TreeSet<>();
		for (ReportVariant var : report.getVariants())
			existingIds.add(var.getId());
		int newId = 0;
		while (existingIds.contains(newId))
			newId++;
		ReportVariant var = new ReportVariant(newId);
		var.setName(variant.getName());
		report.getVariants().add(var);
	}

	public void variantsRemoved(List<ProjectVariant> variants) {
		Report report = editor.getReport();
		if (report == null || variants == null)
			return;
		for (ProjectVariant variant : variants) {
			ReportVariant var = findReportVariant(variant);
			if (var != null)
				report.getVariants().remove(var);
		}
	}

	/**
	 * Sets the given name in the project and report variant. Note that the
	 * given project variant should contain the old variant because this is used
	 * to find the respective report variant.
	 */
	public void updateName(ProjectVariant variant, String newName) {
		ReportVariant var = findReportVariant(variant);
		if (var != null)
			var.setName(newName);
		variant.setName(newName);
	}

	public void updateDescription(ProjectVariant variant, String description) {
		ReportVariant var = findReportVariant(variant);
		if (var != null)
			var.setDescription(description);
	}

	public String getDescription(ProjectVariant variant) {
		ReportVariant var = findReportVariant(variant);
		return var == null ? null : var.getDescription();
	}

	private ReportVariant findReportVariant(ProjectVariant variant) {
		if (editor.getReport() == null || variant == null)
			return null;
		for (ReportVariant reportVariant : editor.getReport().getVariants()) {
			if (Objects.equals(variant.getName(), reportVariant.getName()))
				return reportVariant;
		}
		return null;
	}
}

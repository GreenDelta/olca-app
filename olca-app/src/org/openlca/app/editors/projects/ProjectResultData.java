package org.openlca.app.editors.projects;

import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.results.Sort;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.util.Strings;

public record ProjectResultData(
		IDatabase db,
		Project project,
		ProjectResult result,
		Report report,
		ProjectVariant[] variants,
		ResultItemOrder items,
		NwSetTable nwFactors
) {

	static ProjectResultData of(
			IDatabase db, Project project, ProjectResult result, Report report
	) {
		var variants = result.getVariants()
				.stream()
				.sorted((v1, v2) -> Strings.compare(v1.name, v2.name))
				.toArray(ProjectVariant[]::new);
		var items = ResultItemOrder.of(result);
		Sort.sort(items);
		var nwFactors = project.nwSet != null
				? NwSetTable.of(db, project.nwSet)
				: null;
		nwFactors = nwFactors != null && !nwFactors.isEmpty()
				? nwFactors
				: null;
		return new ProjectResultData(
				db, project, result, report, variants, items, nwFactors);
	}

	public boolean hasReport() {
		return report != null;
	}

	public boolean hasNormalization() {
		return nwFactors != null && nwFactors.hasNormalization();
	}

	public boolean hasWeighting() {
		return nwFactors != null && nwFactors.hasWeighting();
	}

	public String weightedScoreUnit() {
		return project != null && project.nwSet != null
				? project.nwSet.weightedScoreUnit
				: null;
	}
}

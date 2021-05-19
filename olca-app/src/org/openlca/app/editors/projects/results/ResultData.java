package org.openlca.app.editors.projects.results;

import org.openlca.app.results.Sort;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

class ResultData {

	private final Project project;
	private final ProjectResult result;
	private final ProjectVariant[] variants;
	private final ResultItemView items;
	private final NwSetTable nwFactors;

	private ResultData(Project project, ProjectResult result, IDatabase db) {
		this.project = project;
		this.result = result;
		this.variants = result.getVariants()
			.stream()
			.sorted((v1, v2) -> Strings.compare(v1.name, v2.name))
			.toArray(ProjectVariant[]::new);
		this.items = ResultItemView.of(result);
		Sort.sort(this.items);
		var nwFactors = project.nwSet != null
			? NwSetTable.of(db, project.nwSet)
			: null;
		this.nwFactors = nwFactors != null && !nwFactors.isEmpty()
			? nwFactors
			: null;
	}

	static ResultData of(Project project, ProjectResult result, IDatabase db) {
		return new ResultData(project, result, db);
	}

	ProjectResult result() {
		return result;
	}

	Project project() {
		return project;
	}

	ProjectVariant[] variants() {
		return variants;
	}

	ResultItemView items() {
		return items;
	}

	NwSetTable nwFactors() {
		return nwFactors;
	}

	boolean hasNormalization() {
		return nwFactors != null && nwFactors.hasNormalization();
	}

	boolean hasWeighting() {
		return nwFactors != null && nwFactors.hasWeighting();
	}

	String weightedScoreUnit() {
		return project != null && project.nwSet != null
			? project.nwSet.weightedScoreUnit
			: null;
	}

}

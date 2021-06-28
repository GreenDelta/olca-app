package org.openlca.app.editors.projects;

import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.results.Sort;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ProjectResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.util.Strings;

public class ProjectResultData {

	private final IDatabase db;
	private final Project project;
	private final ProjectResult result;
	private final Report report;
	private final ProjectVariant[] variants;
	private final ResultItemView items;
	private final NwSetTable nwFactors;

	private ProjectResultData(
		IDatabase db, Project project, ProjectResult result, Report report) {
		this.db = db;
		this.project = project;
		this.result = result;
		this.report = report;
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

	static ProjectResultData of(
		IDatabase db, Project project, ProjectResult result, Report report) {
		return new ProjectResultData(db, project, result, report);
	}

	public IDatabase db() {
		return db;
	}

	public ProjectResult result() {
		return result;
	}

	public Project project() {
		return project;
	}

	public boolean hasReport() {
		return report != null;
	}

	public Report report() {
		return report;
	}

	public ProjectVariant[] variants() {
		return variants;
	}

	public ResultItemView items() {
		return items;
	}

	public NwSetTable nwFactors() {
		return nwFactors;
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

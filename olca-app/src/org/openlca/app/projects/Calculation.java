package org.openlca.app.projects;

import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Editors;
import org.openlca.core.math.ProjectCalculator;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starts the calculation from the UI thread. */
class Calculation {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ProjectResult result;
	private Project project;

	private Calculation(final Project project) {
		this.project = project;
	}

	static void run(final Project project) {
		new Calculation(project).doIt();
	}

	private void doIt() {
		if (project == null)
			return;
		final ProjectCalculator calculator = new ProjectCalculator(
				Cache.getMatrixCache(), App.getMatrixFactory());
		App.run("Calculate project", new Runnable() {
			public void run() {
				result = tryCalculate(calculator, project);
			}
		}, new Runnable() {
			public void run() {
				if (result == null)
					return;
				String key = Cache.getAppCache().put(result);
				Editors.open(new ProjectResultInput(project.getId(), key),
						ProjectResultEditor.ID);
				result = null;
			}
		});
	}

	private ProjectResult tryCalculate(ProjectCalculator calculator,
			Project project) {
		try {
			return calculator.solve(project);
		} catch (Exception e) {
			log.error("Calculation of project failed");
			return null;
		}
	}

}

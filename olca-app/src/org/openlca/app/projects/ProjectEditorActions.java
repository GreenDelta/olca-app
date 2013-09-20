package org.openlca.app.projects;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.math.ProjectCalculator;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectEditorActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	/** used for passing the result from the calculation thread to the UI thread */
	private ProjectResult result;

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(Actions.onCalculate(new Runnable() {
			public void run() {
				runCalculate();
			}
		}));
	}

	private void runCalculate() {
		final Project project = getProject();
		if (project == null)
			return;
		final ProjectCalculator calculator = new ProjectCalculator(
				Database.getMatrixCache());
		App.run("Calculate project", new Runnable() {
			public void run() {
				result = tryCalculate(calculator, project);
			}
		}, new Runnable() {
			public void run() {
				if (result == null)
					return;
				String key = App.getCache().put(result);
				Editors.open(new ProjectResultInput(project.getId(), key),
						ProjectResultEditor.ID);
				result = null;
			}
		});
	}

	private Project getProject() {
		log.trace("action -> calculate project");
		ProjectEditor editor = Editors.getActive();
		if (editor == null || editor.getModel() == null) {
			log.error("Could not get project from editor");
			return null;
		}
		final Project project = editor.getModel();
		if (project.getVariants().isEmpty()) {
			Dialog.showError(UI.shell(), "No project varaints are defined");
			return null;
		}
		return project;
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

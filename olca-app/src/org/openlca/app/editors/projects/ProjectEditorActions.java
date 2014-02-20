package org.openlca.app.editors.projects;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectEditorActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(Actions.onCalculate(new Runnable() {
			public void run() {
				Project project = getProject();
				Calculation.run(project);
			}
		}));
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

}

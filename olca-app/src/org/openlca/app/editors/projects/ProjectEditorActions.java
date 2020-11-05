package org.openlca.app.editors.projects;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.editors.reports.Reports;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportCalculator;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectEditorActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolBar) {
		toolBar.add(Actions.onCalculate(() -> {
			log.trace("action -> calculate project");
			ProjectEditor e = getEditor();
			if (e == null)
				return;
			calculate(e.getModel(), e.getReport());
		}));
	}

	private ProjectEditor getEditor() {
		ProjectEditor editor = Editors.getActive();
		if (editor == null || editor.getModel() == null) {
			log.error("Could not get project from editor");
			return null;
		}
		Project project = editor.getModel();
		if (project.variants.isEmpty()) {
			MsgBox.error(M.NoProjectVaraintsAreDefined);
			return null;
		}
		return editor;
	}

	static void calculate(Project project, Report report) {
		ReportCalculator calculator = new ReportCalculator(project, report);
		App.runWithProgress(M.Calculate, calculator, () -> {
			if (calculator.hadError)
				return;
			Reports.save(project, report, Database.get());
			ReportViewer.open(report);
		});
	}
}

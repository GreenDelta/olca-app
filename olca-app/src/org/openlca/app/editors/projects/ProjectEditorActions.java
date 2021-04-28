package org.openlca.app.editors.projects;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.projects.results.ProjectResultEditor;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.editors.reports.Reports;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportBuilder;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectEditorActions extends EditorActionBarContributor {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
		var db = Database.get();
		if (db == null || project == null)
			return;

		var ref = new Object() { ProjectResult result; };
		Runnable calculation = () -> {
			try {
				var calculator = new SystemCalculator(db);
				ref.result = calculator.calculate(project);
				ReportBuilder.of(db, project, ref.result).fill(report);
				Reports.save(project, report, db);
			} catch (OutOfMemoryError e) {
				MsgBox.error(M.OutOfMemory, M.CouldNotAllocateMemoryError);
			} catch (MathIllegalArgumentException e) {
				MsgBox.error("Matrix error", e.getMessage());
			} catch (Exception e) {
				ErrorReporter.on("Calculation failed", e);
			}
		};

		App.runWithProgress(M.Calculate, calculation, () -> {
			if (ref.result == null)
				return;
			ReportViewer.open(report);
			ProjectResultEditor.open(project, ref.result);
		});
	}
}

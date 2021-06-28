package org.openlca.app.editors.projects;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.projects.results.ProjectResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.core.results.ProjectResult;

public class ProjectEditorToolBar extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBar) {
		toolBar.add(Actions.onCalculate(() -> {
			try {
				ProjectEditor editor = Editors.getActive();
				calculate(editor);
			} catch (Exception e) {
				ErrorReporter.on("Failed to calculate project", e);
			}
		}));
	}

	static void calculate(ProjectEditor editor) {
		if (editor == null)
			return;
		var db = Database.get();
		var project = editor.getModel();
		if (db == null || project == null)
			return;
		if (project.variants.isEmpty()) {
			MsgBox.error(
				"Nothing to calculate",
				"The project does not contain any product system.");
			return;
		}
		if (project.impactMethod == null) {
			MsgBox.error(
				"Nothing to calculate",
				"No impact assessment method is selected.");
			return;
		}

		var ref = new Object() {
			ProjectResult result;
		};
		Runnable calculation = () -> {
			try {
				ref.result = ProjectResult.calculate(project, db);
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
			var data = ProjectResultData.of(db, project, ref.result, editor.report);
			ProjectResultEditor.open(data);
		});
	}
}

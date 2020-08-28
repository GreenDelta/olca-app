package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.editors.Editors;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;

public abstract class ResultEditor<T extends ContributionResult>
		extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;

	public static void open(
			CalculationSetup setup,
			ContributionResult result) {
		open(setup, result, null);
	}

	public static void open(
			CalculationSetup setup,
			ContributionResult result,
			DQResult dqResult) {
		var input = ResultEditorInput.create(
				setup, result).with(dqResult);
		var id = result instanceof FullResult
				? AnalyzeEditor.ID
				: QuickResultEditor.ID;
		Editors.open(input, id);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		SaveProcessDialog.open(this);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void setFocus() {
	}

}

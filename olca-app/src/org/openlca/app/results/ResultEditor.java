package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.ResultItemView;

public abstract class ResultEditor<T extends ContributionResult>
		extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;
	public ResultItemView resultItems;

	public static void open(CalculationSetup setup, ContributionResult result) {
		open(setup, result, null);
	}

	public static void open(
			CalculationSetup setup, ContributionResult result, DQResult dqResult) {
		var input = ResultEditorInput
				.create(setup, result)
				.with(dqResult);
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
		SaveResultDialog.open(this);
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

	static class ResultEditorInput implements IEditorInput {

		private final String name;
		public final String resultKey;
		public final String setupKey;
		public String dqResultKey;

		private ResultEditorInput(
				String name, String resultKey, String setupKey) {
			this.name = name;
			this.resultKey = resultKey;
			this.setupKey = setupKey;
		}

		static ResultEditorInput create(
				CalculationSetup setup, ContributionResult result) {
			if (setup == null)
				return null;
			var name = Labels.name(setup.target());
			var resultKey = Cache.getAppCache().put(result);
			var setupKey = Cache.getAppCache().put(setup);
			return new ResultEditorInput(name, resultKey, setupKey);
		}

		/**
		 * With data quality
		 */
		public ResultEditorInput with(DQResult dqResult) {
			if (dqResult != null)
				dqResultKey = Cache.getAppCache().put(dqResult);
			return this;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes"})
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.CHART.descriptor();
		}

		@Override
		public String getName() {
			return M.Results + ": " + name;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getName();
		}
	}
}

package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;

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
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.ResultItemView;

public abstract class ResultEditor<T extends ContributionResult> extends FormEditor {

	public T result;
	public CalculationSetup setup;
	public DQResult dqResult;
	public ResultItemView resultItems;

	public static void open(CalculationSetup setup, ContributionResult result) {
		open(setup, result, null);
	}

	public static void open(
			CalculationSetup setup, ContributionResult result,DQResult dqResult) {
		var input = ResultEditorInput
			.create(setup, result)
			.with(dqResult);
		var id = result instanceof FullResult
				? AnalyzeEditor.ID
				: QuickResultEditor.ID;
		Editors.open(input, id);
	}

	public static void open(CalculationSetup setup, ArrayList<ContributionResult> results, DQResult dqResult, List<ProjectVariant> variants) {
		var input = ResultEditorInput.create(setup, results, variants).with(dqResult);
		Editors.open(input, ProjectResultEditor.ID);
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

	public static class ResultEditorInput implements IEditorInput {

		public final long productSystemId;
		public final String resultKey;
		public final String setupKey;
		public String dqResultKey;
		public final String variantKey;

		private ResultEditorInput(long productSystemId, String resultKey, String setupKey, String variantKey) {
			this.productSystemId = productSystemId;
			this.resultKey = resultKey;
			this.setupKey = setupKey;
			this.variantKey = variantKey;
		}

		public static ResultEditorInput create(CalculationSetup setup, ArrayList<ContributionResult> results, List<ProjectVariant> variants) {
			if (setup == null)
				return null;
			String resultKey = Cache.getAppCache().put(results);
			String setupKey = Cache.getAppCache().put(setup);
			String variantKey = Cache.getAppCache().put(variants);
			long systemId = 0;
			if (setup.productSystem != null)
				systemId = setup.productSystem.id;
			return new ResultEditorInput(systemId, resultKey, setupKey, variantKey);
		}

		static ResultEditorInput create(CalculationSetup setup, ContributionResult result) {
			if (setup == null)
				return null;
			String resultKey = Cache.getAppCache().put(result);
			String setupKey = Cache.getAppCache().put(setup);
			long systemId = 0;
			if (setup.productSystem != null)
				systemId = setup.productSystem.id;
			return new ResultEditorInput(systemId, resultKey, setupKey, null);
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
		@SuppressWarnings({ "unchecked", "rawtypes" })
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
			EntityCache cache = Cache.getEntityCache();
			if (cache == null)
				return "";
			ProductSystemDescriptor d = cache.get(ProductSystemDescriptor.class, productSystemId);
			return M.Results + ": " + Labels.name(d);
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

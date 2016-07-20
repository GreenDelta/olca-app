package org.openlca.app.results.quick;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.results.IResultEditor;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickResultEditor extends FormEditor implements IResultEditor<ContributionResultProvider<?>> {

	public static String ID = "QuickResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private ContributionResultProvider<?> result;
	private DQResult dqResult;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ResultEditorInput input = (ResultEditorInput) editorInput;
			setup = Cache.getAppCache().remove(input.getSetupKey(),
					CalculationSetup.class);
			result = Cache.getAppCache().remove(
					input.getResultKey(), ContributionResultProvider.class);
			String dqResultKey = input.getDqResultKey();
			if (dqResultKey != null)
				dqResult = Cache.getAppCache().remove(dqResultKey, DQResult.class);
		} catch (Exception e) {
			log.error("failed to load inventory result", e);
			throw new PartInitException("failed to load inventory result", e);
		}
	}

	@Override
	public CalculationSetup getSetup() {
		return setup;
	}

	@Override
	public ContributionResultProvider<?> getResult() {
		return result;
	}

	@Override
	public DQResult getDqResult() {
		return dqResult;
	}
	
	@Override
	protected void addPages() {
		try {
			addPage(new QuickResultInfoPage(this, result, dqResult));
			addPage(new TotalFlowResultPage(this, result, dqResult));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this, result, dqResult, this::getImpactFactor));
			if (result.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new LocationPage(this, result));
			addPage(new GroupPage(this, result));
		} catch (Exception e) {
			log.error("failed to add pages", e);
		}
	}

	private double getImpactFactor(ImpactCategoryDescriptor impactCategory, ProcessDescriptor process,
			FlowDescriptor flow) {
		ContributionResult cr = result.result;
		FlowIndex flowIdx = cr.flowIndex;
		int row = cr.impactIndex.getIndex(impactCategory.getId());
		int col = flowIdx.getIndex(flow.getId());
		double value = cr.impactFactors.getEntry(row, col);
		if (flowIdx.isInput(flow.getId())) {
			// characterization factors for input flows are negative in the
			// matrix. A simple abs() is not correct because the original
			// characterization factor maybe was already negative (-(-(f))).
			value = -value;
		}
		return value;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		// TODO: save result as system process
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false; // result != null;
	}

	@Override
	public void setFocus() {
	}
}

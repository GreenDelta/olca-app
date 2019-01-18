package org.openlca.app.results.quick;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.results.IResultEditor;
import org.openlca.app.results.ImpactChecksPage;
import org.openlca.app.results.InventoryPage;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.SaveProcessDialog;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickResultEditor extends FormEditor implements IResultEditor<ContributionResult> {

	public static String ID = "QuickResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private ContributionResult result;
	private DQResult dqResult;

	@Override
	public void init(IEditorSite site, IEditorInput iInput)
			throws PartInitException {
		super.init(site, iInput);
		try {
			ResultEditorInput input = (ResultEditorInput) iInput;
			setup = Cache.getAppCache().remove(input.setupKey,
					CalculationSetup.class);
			result = Cache.getAppCache().remove(
					input.resultKey, ContributionResult.class);
			String dqResultKey = input.dqResultKey;
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
	public ContributionResult getResult() {
		return result;
	}

	@Override
	public DQResult getDqResult() {
		return dqResult;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new QuickResultInfoPage(this, result, dqResult, setup));
			addPage(new InventoryPage(this, result, dqResult, setup));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(
						this, result, dqResult, setup, this::getImpactFactor));
			if (result.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new LocationPage(this, result, setup));
			addPage(new GroupPage(this, result, setup));
			if (result.hasImpactResults()) {
				addPage(new ImpactChecksPage(this, setup, result));
			}
		} catch (Exception e) {
			log.error("failed to add pages", e);
		}
	}

	private double getImpactFactor(
			ImpactCategoryDescriptor impact,
			CategorizedDescriptor process,
			FlowDescriptor flow) {
		int row = result.impactIndex.of(impact);
		int col = result.flowIndex.of(flow);
		double value = result.impactFactors.get(row, col);
		if (result.isInput(flow)) {
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

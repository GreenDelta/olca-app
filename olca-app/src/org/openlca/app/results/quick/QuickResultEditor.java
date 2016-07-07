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
import org.openlca.app.results.contributions.ContributionTablePage;
import org.openlca.app.results.contributions.FlowImpactPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
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
	protected void addPages() {
		try {
			addPage(new QuickResultInfoPage(this));
			addPage(new TotalFlowResultPage(this, result, dqResult));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this, result));
			if (result.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new ContributionTablePage(this, result));
			if (result.hasImpactResults())
				addPage(new FlowImpactPage(this, result));
			addPage(new LocationPage(this, result));
			addPage(new GroupPage(this, result));
		} catch (Exception e) {
			log.error("failed to add pages", e);
		}
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

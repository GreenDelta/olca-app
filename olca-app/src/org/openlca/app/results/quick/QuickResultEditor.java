package org.openlca.app.results.quick;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.results.ContributionTablePage;
import org.openlca.app.results.FlowImpactPage;
import org.openlca.app.results.GroupPage;
import org.openlca.app.results.LocationContributionPage;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.viz.ContributionBubblePage;
import org.openlca.app.results.viz.ProcessTreemapPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickResultEditor extends FormEditor {

	public static String ID = "QuickResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private ContributionResultProvider<?> result;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ResultEditorInput input = (ResultEditorInput) editorInput;
			setup = Cache.getAppCache().remove(input.getSetupKey(),
					CalculationSetup.class);
			ContributionResult result = Cache.getAppCache().remove(
					input.getResultKey(), ContributionResult.class);
			this.result = new ContributionResultProvider<>(result,
					Cache.getEntityCache());
		} catch (Exception e) {
			log.error("failed to load inventory result", e);
			throw new PartInitException("failed to load inventory result", e);
		}
	}

	CalculationSetup getSetup() {
		return setup;
	}

	ContributionResultProvider<?> getResult() {
		return result;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new QuickResultInfoPage(this));
			addPage(new TotalFlowResultPage(this, result));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this, result));
			if (result.hasImpactResults() && setup.getNwSet() != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new ContributionTablePage(this, result));
			if (result.hasImpactResults())
				addPage(new FlowImpactPage(this, result));
			addPage(new LocationContributionPage(this, result));
			addPage(new GroupPage(this, result));
			if (FeatureFlag.EXPERIMENTAL_VISUALISATIONS.isEnabled()) {
				addPage(new ProcessTreemapPage(this, result));
				addPage(new ContributionBubblePage(this, result));
			}
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

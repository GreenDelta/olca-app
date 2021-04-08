package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.db.Cache;
import org.openlca.app.results.comparison.ComparisonPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.ContributionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickResultEditor extends ResultEditor<ContributionResult> {

	public static String ID = "QuickResultEditor";
	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void init(IEditorSite site, IEditorInput iInput) throws PartInitException {
		super.init(site, iInput);
		try {
			ResultEditorInput input = (ResultEditorInput) iInput;
			setup = Cache.getAppCache().remove(input.setupKey, CalculationSetup.class);
			result = Cache.getAppCache().remove(input.resultKey, ContributionResult.class);
			String dqkey = input.dqResultKey;
			if (dqkey != null) {
				dqResult = Cache.getAppCache().remove(dqkey, DQResult.class);
			}
		} catch (Exception e) {
			log.error("failed to load inventory result", e);
			throw new PartInitException("failed to load inventory result", e);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new InventoryPage(this));
			if (result.hasImpactResults()) {
				addPage(new TotalImpactResultPage(this));
			}
			if (result.hasImpactResults() && setup.nwSet != null) {
				addPage(new NwResultPage(this, result, setup));
			}
			addPage(new LocationPage(this, result, setup));
			addPage(new GroupPage(this, result, setup));
			if (result.hasImpactResults()) {
				addPage(new ImpactChecksPage(this));
			}
			addPage(new ComparisonPage(this));
		} catch (Exception e) {
			log.error("failed to add pages", e);
		}
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

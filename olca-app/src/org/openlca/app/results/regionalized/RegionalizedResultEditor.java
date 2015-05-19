package org.openlca.app.results.regionalized;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.results.ContributionTablePage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.geo.RegionalizedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionalizedResultEditor extends FormEditor {

	public static String ID = "RegionalizedResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private RegionalizedResult result;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ResultEditorInput input = (ResultEditorInput) editorInput;
			setup = Cache.getAppCache().remove(input.getSetupKey(),
					CalculationSetup.class);
			result = Cache.getAppCache().remove(input.getResultKey(),
					RegionalizedResult.class);
		} catch (Exception e) {
			log.error("failed to load regionalized result", e);
			throw new PartInitException("failed to load regionalized result", e);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new TotalFlowResultPage(this, result.getBaseResult()));
			if (result.getRegionalizedResult() != null) {
				addPage(new TotalImpactResultPage(this,
						result.getRegionalizedResult()));
				addPage(new ContributionTablePage(this,
						result.getRegionalizedResult()));
				addPage(new KmlResultView(this, result));
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
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}

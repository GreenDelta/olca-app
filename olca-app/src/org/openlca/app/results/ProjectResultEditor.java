package org.openlca.app.results;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.db.Cache;
import org.openlca.app.results.comparison.ProjectComparisonPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ContributionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectResultEditor extends ResultEditor<ContributionResult> {

	public static String ID = "ProjectResultEditor";
	private Logger log = LoggerFactory.getLogger(getClass());
	public List<ContributionResult> results;
	public List<ProjectVariant> variants;

	@Override
	public void init(IEditorSite site, IEditorInput iInput) throws PartInitException {
		super.init(site, iInput);
		try {
			ResultEditorInput input = (ResultEditorInput) iInput;
			setup = Cache.getAppCache().remove(input.setupKey, CalculationSetup.class);
			results = (List<ContributionResult>) Cache.getAppCache().remove(input.resultKey, List.class);
			variants = (List<ProjectVariant>) Cache.getAppCache().remove(input.variantKey, List.class);

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
			addPage(new ProjectComparisonPage(this));
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

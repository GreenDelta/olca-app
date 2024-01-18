package org.openlca.app.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.contributions.TagResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.app.results.impacts.ImpactTreePage;
import org.openlca.app.results.slca.ui.SocialResultPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MemoryError;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.openlca.app.tools.graphics.EditorActionBarContributor.refreshActionBar;

/**
 * View for the analysis results of a product system.
 */
public class ResultEditor extends FormEditor {

	public static final String ID = "editors.analyze";

	private ResultBundle bundle;

	public static void open(ResultBundle bundle) {
		if (bundle == null)
			return;
		var name = M.ResultsOf + ": " + Labels.name(bundle.setup().target());
		var id = Cache.getAppCache().put(bundle);
		var input = new SimpleEditorInput(id, name);
		Editors.open(input, ResultEditor.ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.ANALYSIS_RESULT.get());
		var inp = (SimpleEditorInput) input;
		bundle = Cache.getAppCache().remove(inp.id, ResultBundle.class);
		setPartName(inp.getName());
	}

	public LcaResult result() {
		return bundle.result();
	}

	public CalculationSetup setup() {
		return bundle.setup();
	}

	public ResultItemOrder items() {
		return bundle.items();
	}

	public DQResult dqResult() {
		return bundle.dqResult();
	}

	@Override
	public void dispose() {
		result().dispose();
		super.dispose();
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new InventoryPage(this));
			if (result().hasImpacts()) {
				addPage(new ImpactTreePage(this));
			}
			if (result().hasImpacts() && setup().nwSet() != null) {
				addPage(new NwResultPage(this));
			}
			if (bundle.hasSocialResult()) {
				addPage(new SocialResultPage(this, bundle.socialResult()));
			}
			addPage(new ProcessResultPage(this));
			addPage(new ContributionTreePage(this));
			addPage(new GroupPage(this));
			addPage(new LocationPage(this));

			var sankey = new SankeyEditor(this);
			var sankeyIndex = addPage(sankey, getEditorInput());
			setPageText(sankeyIndex, M.SankeyDiagram);
			// Add a page listener to set the graph when
			// it is activated the first time.
			setSankeyPageListener(sankey);

			if (result().hasImpacts()) {
				addPage(new ImpactChecksPage(this));
			}
			if (FeatureFlag.TAG_RESULTS.isEnabled()) {
				addPage(new TagResultPage(this));
			}

		} catch (Throwable e) {
			ErrorReporter.on("failed to create result pages", e);
			this.close(false);
		}
	}

	@Override
	protected void pageChange(int newPageIndex) {
		try {
			super.pageChange(newPageIndex);
		} catch (OutOfMemoryError e) {
			MemoryError.show();
			this.close(false);
		}
	}

	private void setSankeyPageListener(SankeyEditor sankeyEditor) {
		var sankeyInit = new AtomicReference<IPageChangedListener>();
		IPageChangedListener fn = e -> {
			if (!Objects.equals(e.getSelectedPage(), sankeyEditor))
				return;
			var listener = sankeyInit.get();
			if (listener == null)
				return;

			sankeyEditor.onFirstActivation();

			// Artificially refreshing the ActionBarContributor.
			refreshActionBar(this);

			removePageChangedListener(listener);
			sankeyInit.set(null);
		};
		sankeyInit.set(fn);
		addPageChangedListener(fn);
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

}

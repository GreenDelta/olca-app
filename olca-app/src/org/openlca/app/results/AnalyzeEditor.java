package org.openlca.app.results;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.contributions.TagResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.app.util.Labels;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.ResultItemView;
import org.slf4j.LoggerFactory;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends ResultEditor<FullResult> {

	public static final String ID = "editors.analyze";

	private SankeyDiagram diagram;
	private int diagramIndex;

	@Override
	public void init(IEditorSite site, IEditorInput iInput)
			throws PartInitException {
		super.init(site, iInput);
		ResultEditorInput inp = (ResultEditorInput) iInput;
		result = Cache.getAppCache().remove(inp.resultKey, FullResult.class);
		if (inp.dqResultKey != null) {
			dqResult = Cache.getAppCache().remove(
					inp.dqResultKey, DQResult.class);
		}
		setup = Cache.getAppCache().remove(inp.setupKey, CalculationSetup.class);
		resultItems = ResultItemView.of(result);
		Sort.sort(resultItems);
		String name = M.AnalysisResultOf + " " + Labels.name(setup.target());
		setPartName(name);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new InventoryPage(this));
			if (result.hasImpacts())
				addPage(new TotalImpactResultPage(this));
			if (result.hasImpacts() && setup.nwSet() != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new ProcessResultPage(this, result, setup));
			addPage(new ContributionTreePage(this));
			addPage(new GroupPage(this, result, setup));
			addPage(new LocationPage(this, result, setup));
			diagram = new SankeyDiagram(this);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, M.SankeyDiagram);
			if (result.hasImpacts()) {
				addPage(new ImpactChecksPage(this));
			}
			if (FeatureFlag.TAG_RESULTS.isEnabled()) {
				addPage(new TagResultPage(this));
			}

			// add a page listener to initialize the Sankey diagram
			// lazily when it is activated the first time
			var sankeyInit = new AtomicReference<IPageChangedListener>();
			IPageChangedListener fn = e -> {
				if (e.getSelectedPage() != diagram)
					return;
				var listener = sankeyInit.get();
				if (listener == null)
					return;
				diagram.initContent();
				removePageChangedListener(listener);
				sankeyInit.set(null);
			};
			sankeyInit.set(fn);
			addPageChangedListener(fn);

		} catch (final PartInitException e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("Add pages failed", e);
		}
	}

	public SankeyDiagram getDiagram() {
		return diagram;
	}

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == diagramIndex) {
			page = diagram;
		}
		return page;
	}

}

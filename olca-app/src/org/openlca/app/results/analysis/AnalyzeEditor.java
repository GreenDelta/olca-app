package org.openlca.app.results.analysis;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.results.ImpactChecksPage;
import org.openlca.app.results.InfoPage;
import org.openlca.app.results.InventoryPage;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.FullResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends ResultEditor<FullResult> {

	public static final String ID = "editors.analyze";

	private Logger log = LoggerFactory.getLogger(getClass());

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
		ProductSystem system = setup.productSystem;
		String name = M.AnalysisResultOf + " " + system.name;
		setPartName(name);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new InventoryPage(this));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this));
			if (result.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new ProcessResultPage(this, result, setup));
			addPage(new ContributionTreePage(this, result, setup));
			addPage(new GroupPage(this, result, setup));
			addPage(new LocationPage(this, result, setup));
			diagram = new SankeyDiagram(result, dqResult, setup);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, M.SankeyDiagram);
			if (result.hasImpactResults()) {
				addPage(new ImpactChecksPage(this));
			}
		} catch (final PartInitException e) {
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
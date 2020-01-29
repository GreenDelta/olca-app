package org.openlca.app.results.regionalized;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
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
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.geo.RegionalizedResult;
import org.openlca.geo.parameter.ParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionalizedResultEditor extends ResultEditor<FullResult> {

	public static String ID = "RegionalizedResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private RegionalizedResult result;
	private CalculationSetup setup;
	private SankeyDiagram diagram;
	private int diagramIndex;
	private FactorCalculator factorCalculator;
	private ImpactCategoryDao impactCategoryDao;
	private Map<Long, ImpactCategory> impactCategories = new HashMap<>();
	private Map<LongPair, Map<FlowDescriptor, Double>> factorsMap = new HashMap<>();
	private DQResult dqResult;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ResultEditorInput input = (ResultEditorInput) editorInput;
			setup = Cache.getAppCache().remove(input.setupKey,
					CalculationSetup.class);
			result = Cache.getAppCache().remove(input.resultKey,
					RegionalizedResult.class);
			impactCategoryDao = new ImpactCategoryDao(Database.get());
			ParameterSet parameterSet = Cache.getAppCache().remove(
					input.parameterSetKey, ParameterSet.class);
			factorCalculator = new FactorCalculator(parameterSet,
					Database.get(), setup);
			String dqResultKey = input.dqResultKey;
			if (dqResultKey != null)
				dqResult = Cache.getAppCache().remove(dqResultKey, DQResult.class);
		} catch (Exception e) {
			log.error("failed to load regionalized result", e);
			throw new PartInitException("failed to load regionalized result", e);
		}
	}

	@Override
	protected void addPages() {
		try {
			FullResult regioResult = this.result.result;
			addPage(new InfoPage(this));
			addPage(new InventoryPage(this));
			if (regioResult.hasImpactResults())
				addPage(new TotalImpactResultPage(this));
			if (regioResult.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, regioResult, setup));
			addPage(new KmlResultView(this, result, setup));
			addPage(new LocationPage(this, regioResult, setup, false));
			addPage(new ProcessResultPage(this, regioResult, setup));
			addPage(new ContributionTreePage(this, regioResult, setup));
			addPage(new GroupPage(this, regioResult, setup));
			diagram = new SankeyDiagram(regioResult, dqResult, setup);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, M.SankeyDiagram);
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

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == diagramIndex) {
			page = diagram;
		}
		return page;
	}
}

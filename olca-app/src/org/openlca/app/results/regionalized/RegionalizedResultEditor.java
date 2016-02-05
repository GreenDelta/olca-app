package org.openlca.app.results.regionalized;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.results.IResultEditor;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.SunBurstView;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.analysis.AnalyzeInfoPage;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.contributions.ContributionTablePage;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ImpactTreePage;
import org.openlca.app.results.contributions.ImpactTreePage.FlowWithProcess;
import org.openlca.app.results.contributions.LocationContributionPage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.app.results.viz.ContributionBubblePage;
import org.openlca.app.results.viz.ProcessTreemapPage;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.FullResultProvider;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.parameter.ParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionalizedResultEditor extends FormEditor implements IResultEditor<FullResultProvider> {

	public static String ID = "RegionalizedResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private RegionalizedResultProvider result;
	private CalculationSetup setup;
	private SankeyDiagram diagram;
	private int diagramIndex;
	private ImpactFactorCalculator factorCalculator;
	private ImpactCategoryDao impactCategoryDao;
	private Map<Long, ImpactCategory> impactCategories = new HashMap<>();
	private Map<LongPair, Map<FlowDescriptor, Double>> factorsMap = new HashMap<>();

	@Override
	public CalculationSetup getSetup() {
		return setup;
	}

	@Override
	public FullResultProvider getResult() {
		return result.result;
	}

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			ResultEditorInput input = (ResultEditorInput) editorInput;
			setup = Cache.getAppCache().remove(input.getSetupKey(),
					CalculationSetup.class);
			result = Cache.getAppCache().remove(input.getResultKey(),
					RegionalizedResultProvider.class);
			impactCategoryDao = new ImpactCategoryDao(Database.get());
			ParameterSet parameterSet = Cache.getAppCache().remove(
					input.getParameterSetKey(), ParameterSet.class);
			factorCalculator = new ImpactFactorCalculator(parameterSet);
		} catch (Exception e) {
			log.error("failed to load regionalized result", e);
			throw new PartInitException("failed to load regionalized result", e);
		}
	}

	@Override
	protected void addPages() {
		try {
			FullResultProvider regioResult = this.result.result;
			addPage(new AnalyzeInfoPage(this, regioResult, setup));
			addPage(new TotalFlowResultPage(this, regioResult));
			if (regioResult.hasImpactResults())
				addPage(new TotalImpactResultPage(this, regioResult));
			if (regioResult.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, regioResult, setup));
			addPage(new ContributionTablePage(this, regioResult));
			addPage(new KmlResultView(this, this.result));
			addPage(new LocationContributionPage(this, regioResult, false));
			addPage(new ProcessResultPage(this, regioResult, setup));
			// if (regioResult.hasImpactResults())
			// addPage(new FlowImpactPage(this, regioResult));
			addPage(new ContributionTreePage(this, regioResult));
			addPage(new ImpactTreePage(this, regioResult, this::getImpactFactor));
			addPage(new GroupPage(this, regioResult));
			if (FeatureFlag.EXPERIMENTAL_VISUALISATIONS.isEnabled()) {
				addPage(new ProcessTreemapPage(this, regioResult));
				addPage(new ContributionBubblePage(this, regioResult));
				addPage(new SunBurstView(this, regioResult));
			}
			diagram = new SankeyDiagram(setup, regioResult);
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

	private double getImpactFactor(ImpactCategoryDescriptor category,
			FlowWithProcess descriptor) {
		if (descriptor.process.getLocation() == null)
			return _getImpactFactor(category, descriptor);
		Map<FlowDescriptor, Double> impactFactors = getImpactFactors(
				category.getId(), descriptor.process.getLocation());
		if (!impactFactors.containsKey(descriptor.flow))
			return 0d;
		return impactFactors.get(descriptor.flow);
	}

	private double _getImpactFactor(ImpactCategoryDescriptor category,
			FlowWithProcess descriptor) {
		FullResult result = this.result.result.result;
		int row = result.impactIndex.getIndex(category.getId());
		int col = result.flowIndex.getIndex(descriptor.flow.getId());
		return Math.abs(result.impactFactors.getEntry(row, col));
	}

	private Map<FlowDescriptor, Double> getImpactFactors(long categoryId,
			long locationId) {
		LongPair id = new LongPair(categoryId, locationId);
		Map<FlowDescriptor, Double> factors = factorsMap.get(id);
		if (factors == null) {
			ImpactCategory category = getImpactCategory(categoryId);
			factors = factorCalculator.calculate(category, locationId);
			factorsMap.put(id, factors);
		}
		return factors;
	}

	private ImpactCategory getImpactCategory(long id) {
		ImpactCategory category = impactCategories.get(id);
		if (category == null)
			impactCategories.put(id, category = impactCategoryDao.getForId(id));
		return category;
	}
}

package org.openlca.app.results.analysis;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.results.IResultEditor;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.SunBurstView;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.FullResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends FormEditor implements IResultEditor<FullResultProvider> {

	public static final String ID = "editors.analyze";

	private Logger log = LoggerFactory.getLogger(getClass());

	private SankeyDiagram diagram;
	private int diagramIndex;
	private CalculationSetup setup;
	private FullResultProvider result;
	private DQResult dqResult;

	@Override
	public CalculationSetup getSetup() {
		return setup;
	}

	@Override
	public FullResultProvider getResult() {
		return result;
	}

	public DQResult getDqResult() {
		return dqResult;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		ResultEditorInput editorInput = (ResultEditorInput) input;
		String resultKey = editorInput.resultKey;
		String setupKey = editorInput.setupKey;
		FullResultProvider result = Cache.getAppCache().remove(resultKey,
				FullResultProvider.class);
		String dqResultKey = editorInput.dqResultKey;
		if (dqResultKey != null)
			dqResult = Cache.getAppCache().remove(dqResultKey, DQResult.class);
		setup = Cache.getAppCache().remove(setupKey, CalculationSetup.class);
		ProductSystem system = setup.productSystem;
		String name = M.AnalysisResultOf + " " + system.getName();
		setPartName(name);
		this.result = result;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new AnalyzeInfoPage(this, result, dqResult, setup));
			addPage(new TotalFlowResultPage(this, result, dqResult));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this, result, dqResult, this::getImpactFactor));
			if (result.hasImpactResults() && setup.nwSet != null)
				addPage(new NwResultPage(this, result, setup));
			addPage(new ProcessResultPage(this, result));
			addPage(new ContributionTreePage(this, result));
			addPage(new GroupPage(this, result));
			addPage(new LocationPage(this, result));
			if (FeatureFlag.EXPERIMENTAL_VISUALISATIONS.isEnabled()) {
				addPage(new SunBurstView(this, result));
			}
			diagram = new SankeyDiagram(setup, result, dqResult);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, M.SankeyDiagram);
		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {

	}

	public SankeyDiagram getDiagram() {
		return diagram;
	}

	@Override
	public boolean isDirty() {
		return false;
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

	private double getImpactFactor(ImpactCategoryDescriptor impactCategory, ProcessDescriptor process,
			FlowDescriptor flow) {
		FullResult fr = result.result;
		FlowIndex flowIdx = fr.flowIndex;
		int row = fr.impactIndex.getIndex(impactCategory.getId());
		int col = flowIdx.getIndex(flow.getId());
		double value = fr.impactFactors.get(row, col);
		if (flowIdx.isInput(flow.getId())) {
			// characterization factors for input flows are negative in the
			// matrix. A simple abs() is not correct because the original
			// characterization factor maybe was already negative (-(-(f))).
			value = -value;
		}
		return value;
	}

}
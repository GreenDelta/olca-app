package org.openlca.app.results.analysis;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.results.IResultEditor;
import org.openlca.app.results.ImpactChecksPage;
import org.openlca.app.results.InventoryPage;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.SaveProcessDialog;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.contributions.ContributionTreePage;
import org.openlca.app.results.contributions.ProcessResultPage;
import org.openlca.app.results.contributions.locations.LocationPage;
import org.openlca.app.results.grouping.GroupPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends FormEditor implements IResultEditor<FullResult> {

	public static final String ID = "editors.analyze";

	private Logger log = LoggerFactory.getLogger(getClass());

	private SankeyDiagram diagram;
	private int diagramIndex;
	private CalculationSetup setup;
	private FullResult result;
	private DQResult dqResult;

	@Override
	public CalculationSetup getSetup() {
		return setup;
	}

	@Override
	public FullResult getResult() {
		return result;
	}

	public DQResult getDqResult() {
		return dqResult;
	}

	@Override
	public void init(IEditorSite site, IEditorInput iInput)
			throws PartInitException {
		super.init(site, iInput);
		ResultEditorInput input = (ResultEditorInput) iInput;
		String resultKey = input.resultKey;
		String setupKey = input.setupKey;
		FullResult result = Cache.getAppCache().remove(resultKey,
				FullResult.class);
		String dqResultKey = input.dqResultKey;
		if (dqResultKey != null)
			dqResult = Cache.getAppCache().remove(dqResultKey, DQResult.class);
		setup = Cache.getAppCache().remove(setupKey, CalculationSetup.class);
		ProductSystem system = setup.productSystem;
		String name = M.AnalysisResultOf + " " + system.name;
		setPartName(name);
		this.result = result;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new AnalyzeInfoPage(this, result, dqResult, setup));
			addPage(new InventoryPage(this, result, dqResult, setup));
			if (result.hasImpactResults())
				addPage(new TotalImpactResultPage(this, result, dqResult, setup, this::getImpactFactor));
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
				addPage(new ImpactChecksPage(this, setup, result));
			}
		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		SaveProcessDialog.open(this);
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
		return true;
	}

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == diagramIndex) {
			page = diagram;
		}
		return page;
	}

	private double getImpactFactor(ImpactCategoryDescriptor impact,
			CategorizedDescriptor process, FlowDescriptor flow) {
		int row = result.impactIndex.of(impact);
		int col = result.flowIndex.of(flow);
		double value = result.impactFactors.get(row, col);
		if (result.isInput(flow)) {
			// characterization factors for input flows are negative in the
			// matrix. A simple abs() is not correct because the original
			// characterization factor maybe was already negative (-(-(f))).
			value = -value;
		}
		return value;
	}

}
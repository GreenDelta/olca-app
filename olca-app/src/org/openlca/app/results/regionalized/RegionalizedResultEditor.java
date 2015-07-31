package org.openlca.app.results.regionalized;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.results.ContributionTablePage;
import org.openlca.app.results.FlowImpactPage;
import org.openlca.app.results.GroupPage;
import org.openlca.app.results.LocationContributionPage;
import org.openlca.app.results.NwResultPage;
import org.openlca.app.results.ResultEditorInput;
import org.openlca.app.results.TotalFlowResultPage;
import org.openlca.app.results.TotalImpactResultPage;
import org.openlca.app.results.analysis.AnalyzeInfoPage;
import org.openlca.app.results.analysis.ContributionTreePage;
import org.openlca.app.results.analysis.ImpactTreePage;
import org.openlca.app.results.analysis.ProcessResultPage;
import org.openlca.app.results.analysis.SunBurstView;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.results.viz.ContributionBubblePage;
import org.openlca.app.results.viz.ProcessTreemapPage;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.FullResultProvider;
import org.openlca.geo.RegionalizedResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionalizedResultEditor extends FormEditor {

	public static String ID = "RegionalizedResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private RegionalizedResultProvider result;
	private CalculationSetup setup;
	private SankeyDiagram diagram;
	private int diagramIndex;

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
		} catch (Exception e) {
			log.error("failed to load regionalized result", e);
			throw new PartInitException("failed to load regionalized result",
					e);
		}
	}

	@Override
	protected void addPages() {
		try {
			FullResultProvider regioRresult = this.result
					.getRegionalizedResult();
			if (regioRresult != null) {
				addPage(new AnalyzeInfoPage(this, regioRresult, setup));
				addPage(new TotalFlowResultPage(this, regioRresult));
				if (regioRresult.hasImpactResults())
					addPage(new TotalImpactResultPage(this, regioRresult));
				if (regioRresult.hasImpactResults() && setup.getNwSet() != null)
					addPage(new NwResultPage(this, regioRresult, setup));
				addPage(new ContributionTablePage(this, regioRresult));
				addPage(new KmlResultView(this, this.result));
				addPage(new LocationContributionPage(this, regioRresult,
						false));
				addPage(new ProcessResultPage(this, regioRresult, setup));
				if (regioRresult.hasImpactResults())
					addPage(new FlowImpactPage(this, regioRresult));
				addPage(new ContributionTreePage(this, regioRresult));
				addPage(new ImpactTreePage(this, regioRresult));
				addPage(new GroupPage(this, regioRresult));
				if (FeatureFlag.EXPERIMENTAL_VISUALISATIONS.isEnabled()) {
					addPage(new ProcessTreemapPage(this, regioRresult));
					addPage(new ContributionBubblePage(this, regioRresult));
					addPage(new SunBurstView(this, regioRresult));
				}
				diagram = new SankeyDiagram(setup, regioRresult);
				diagramIndex = addPage(diagram, getEditorInput());
				setPageText(diagramIndex, Messages.SankeyDiagram);
			} else {
				addPage(new TotalFlowResultPage(this, result.getBaseResult()));
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

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == diagramIndex) {
			page = diagram;
		}
		return page;
	}

}

/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.analyze;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.core.application.App;
import org.openlca.core.application.FeatureFlag;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.AnalyzeEditorInput;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.editors.analyze.sankey.SankeyDiagram;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.editors.model.FlowInfoDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.results.AnalysisResult;

/**
 * View for the analysis results of a product system.
 */
public class AnalyzeEditor extends ModelEditor {

	public static final String ID = "org.openlca.core.editors.analyze.AnalyzeEditor";

	private SankeyDiagram diagram;
	private int diagramIndex;
	private AnalyzeEditorInput editorInput;
	private AnalysisResult result;
	private Map<Flow, FlowInfo> flowInfos;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		editorInput = (AnalyzeEditorInput) input;
		String resultKey = editorInput.getResultKey();
		result = App.getCache().remove(resultKey, AnalysisResult.class);
		setSite(site);
		setInput(input);
		String name = Messages.Analyze_ResultOf + " "
				+ result.getSetup().getProductSystem().getName();
		setPartName(name);
		initFlowInfows(editorInput.getDatabase());
	}

	private void initFlowInfows(IDatabase database) {
		log.trace("Initialize flow infos");
		flowInfos = new HashMap<Flow, FlowInfo>();
		if (result == null || database == null)
			return;
		try {
			FlowInfoDao dao = new FlowInfoDao(database);
			for (Flow flow : result.getFlows())
				flowInfos.put(flow, dao.fromFlow(flow));
		} catch (Exception e) {
			log.error("Failed to init. flow infos", e);
		}
	}

	FlowInfo getFlowInfo(Flow flow) {
		return flowInfos.get(flow);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new AnalyzeInfoPage(this, result, editorInput));
			addPage(new LCITotalPage(this, result));
			if (result.hasImpactResults())
				addPage(new LCIATotalPage(this, result));

			addPage(new ProcessContributionPage(this, result));
			addPage(new ProcessResultPage(this, result));
			if (result.hasImpactResults())
				addPage(new FlowImpactPage(this, result));
			addPage(new ContributionTreePage(this, result));
			addPage(new GroupPage(this, result));
			addPage(new LocationContributionPage(this, result));
			if (FeatureFlag.SUNBURST_CHART.isEnabled())
				addPage(new SunBurstView(this, result));
			if (FeatureFlag.LOCALISED_LCIA.isEnabled()
					&& result.hasImpactResults())
				addPage(new LocalisedImpactPage(this, result));
			diagram = new SankeyDiagram(editorInput, result);
			diagramIndex = addPage(diagram, getEditorInput());
			setPageText(diagramIndex, "Sankey diagram");
		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
	}

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[0];
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

	@Override
	public void setFocus() {
	}

	@Override
	public void close(boolean save) {
		super.close(save);
	}

}

package org.openlca.app.results;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.openlca.core.results.UpstreamTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class SunBurstView extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FullResultProvider result;
	private Browser browser;
	private ResultTypeSelection flowImpactSelection;
	private boolean loaded;
	
	public SunBurstView(FormEditor editor, FullResultProvider result) {
		super(editor, "analysis.SunBurstView", "Sun burst");
		this.result = result;

	}

	@Override
	public String getUrl() {
		return HtmlView.SUNBURST_CHART.getUrl();
	}

	@Override
	public void onLoaded() {
		loaded = true;
		App.run("Calculate result", () -> {
			FlowDescriptor first = firstFlow();
			if (first == null)
				return;
			log.trace("initialize the tree");
			result.getTree(first);
		} , () -> {
			FlowDescriptor first = firstFlow();
			if (first == null)
				return;
			flowImpactSelection.selectWithEvent(first);
		});
	}

	private FlowDescriptor firstFlow() {
		FlowIndex flowIndex = result.result.flowIndex;
		long flowId = flowIndex.getFlowAt(0);
		return Cache.getEntityCache().get(FlowDescriptor.class, flowId);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Sun burst");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = ResultTypeSelection
				.on(result, Cache.getEntityCache())
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(FlowDescriptor flow) {
			if (result == null || flow == null)
				return;
			UpstreamTree tree = result.getTree(flow);
			setResultData(tree);
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			if (result == null || impactCategory == null)
				return;
			UpstreamTree tree = result.getTree(impactCategory);
			setResultData(tree);
		}

		@Override
		public void costResultSelected(CostResultDescriptor cost) {
			if (result == null || cost == null)
				return;
			UpstreamTree tree = result.getCostTree();
			setResultData(tree);
		}

		private void setResultData(UpstreamTree tree) {
			if (!loaded)
				return;
			Gson gson = new Gson();
			SunBurstTree model = SunBurstTree.create(tree,
					Cache.getEntityCache());
			String json = gson.toJson(model);
			String command = "setData(" + json + ")";
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to set sunburst chart data", e);
			}
		}
	}

}

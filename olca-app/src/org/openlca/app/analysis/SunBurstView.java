package org.openlca.app.analysis;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.util.UI;
import org.openlca.core.editors.HtmlView;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.AnalysisResult;
import org.openlca.core.results.ContributionTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class SunBurstView extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private AnalysisResult result;
	private Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public SunBurstView(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, "analysis.SunBurstView", "Sun burst");
		this.result = result;

	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.SUNBURST_CHART.getResource();
	}

	@Override
	public void onLoaded() {
		App.run("Calculate result", new Runnable() {
			@Override
			public void run() {
				FlowDescriptor first = firstFlow();
				if (first == null)
					return;
				log.trace("initialize the tree");
				result.getContributions().getTree(first);
			}
		}, new Runnable() {
			@Override
			public void run() {
				FlowDescriptor first = firstFlow();
				if (first == null)
					return;
				flowImpactSelection.selectWithEvent(first);
			}
		});
	}

	private FlowDescriptor firstFlow() {
		long flowId = result.getFlowIndex().getFlowAt(0);
		FlowDescriptor flow = Cache.getEntityCache().get(FlowDescriptor.class,
				flowId);
		return flow;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Sun burst");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = FlowImpactSelection
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
			ContributionTree tree = result.getContributions().getTree(flow);
			setResultData(tree);
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			if (result == null || impactCategory == null
					|| !result.hasImpactResults())
				return;
			ContributionTree tree = result.getContributions().getTree(
					impactCategory);
			setResultData(tree);
		}

		private void setResultData(ContributionTree tree) {
			Gson gson = new Gson();
			String json = gson.toJson(tree);
			String command = "setData(" + json + ")";
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}

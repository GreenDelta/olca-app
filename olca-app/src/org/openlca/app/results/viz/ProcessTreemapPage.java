package org.openlca.app.results.viz;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTreemapPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionResultProvider<?> result;
	private Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public ProcessTreemapPage(FormEditor editor,
			ContributionResultProvider<?> result) {
		super(editor, "ProcessTreemapPage", "Process contributions - Treemap");
		this.result = result;
	}

	@Override
	public String getUrl() {
		return HtmlView.TREEMAP.getUrl();
	}

	@Override
	public void onLoaded() {
		FlowIndex flowIndex = result.getResult().getFlowIndex();
		long flowId = flowIndex.getFlowAt(0);
		FlowDescriptor first = Cache.getEntityCache().get(FlowDescriptor.class,
				flowId);
		if (first == null)
			return;
		flowImpactSelection.selectWithEvent(first);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Process contributions");
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
			String resultData = ProcessTreemap.calculate(result, flow);
			setResultData(resultData);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			String resultData = ProcessTreemap.calculate(result, impact);
			setResultData(resultData);
		}

		private void setResultData(String result) {
			String command = "setData(" + result + ")";
			try {
				log.trace("set treemap result data");
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to set treemap chart data", e);
			}
		}
	}
}

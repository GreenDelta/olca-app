package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.geo.KmlFeature;
import org.openlca.geo.RegionalizedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

class KmlResultView extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RegionalizedResult result;
	private Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public KmlResultView(FormEditor editor, RegionalizedResult result) {
		super(editor, "KmlResultView", "Result map");
		this.result = result;
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_RESULT_VIEW.getUrl();
	}

	@Override
	public void onLoaded() {
		Set<FlowDescriptor> flowDescriptors = result.getRegionalizedResult()
				.getFlowDescriptors();
		if (flowDescriptors.isEmpty())
			return;
		FlowDescriptor flow = flowDescriptors.iterator().next();
		flowImpactSelection.selectWithEvent(flow);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Result map");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = FlowImpactSelection
				.on(result.getRegionalizedResult(), Cache.getEntityCache())
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(FlowDescriptor flow) {
			ContributionResultProvider<?> provider = result
					.getRegionalizedResult();
			Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
			Map<Long, ProcessResult> results = new HashMap<>();
			for (ProcessDescriptor process : processes) {
				double amount = provider.getSingleFlowResult(process, flow)
						.getValue();
				ProcessResult result = new ProcessResult();
				result.amount = amount;
				result.process = process;
				results.put(process.getId(), result);
			}
			setResultData(results);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			ContributionResultProvider<?> provider = result
					.getRegionalizedResult();
			Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
			Map<Long, ProcessResult> results = new HashMap<>();
			for (ProcessDescriptor process : processes) {
				double amount = provider.getSingleImpactResult(process, impact)
						.getValue();
				ProcessResult resultItem = new ProcessResult();
				resultItem.amount = amount;
				resultItem.process = process;
				results.put(process.getId(), resultItem);
			}
			setResultData(results);
		}

		private void setResultData(Map<Long, ProcessResult> results) {
			Map<LongPair, KmlFeature> features = result.getKmlFeatures();
			List<KmlItem> items = new ArrayList<>();
			for (LongPair processProduct : features.keySet()) {
				ProcessResult resultItem = results.get(processProduct
						.getFirst());
				if (resultItem == null)
					continue;
				KmlItem item = new KmlItem();
				items.add(item);
				item.amount = resultItem.amount;
				item.name = Labels.getDisplayName(resultItem.process);
				item.kml = features.get(processProduct).getKml();
			}
			Gson gson = new Gson();
			String json = gson.toJson(items);
			String command = "setData(" + json + ")";
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to set treemap chart data", e);
			}
		}
	}

	@SuppressWarnings("unused")
	private class KmlItem {
		String name;
		double amount;
		String kml;
	}

	private class ProcessResult {
		ProcessDescriptor process;
		double amount;
	}

}

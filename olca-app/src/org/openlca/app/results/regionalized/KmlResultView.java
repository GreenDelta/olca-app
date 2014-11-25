package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.openlca.app.App;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.geo.RegionalizedResultProvider;
import org.openlca.geo.kml.KmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

class KmlResultView extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RegionalizedResultProvider result;
	private Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public KmlResultView(FormEditor editor, RegionalizedResultProvider result) {
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
			Map<Long, Double> results = new HashMap<>();
			for (ProcessDescriptor process : processes) {
				double amount = provider.getSingleFlowResult(process, flow)
						.getValue();
				results.put(process.getId(), amount);
			}
			setResultData(results);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			ContributionResultProvider<?> provider = result
					.getRegionalizedResult();
			Set<ProcessDescriptor> processes = provider.getProcessDescriptors();
			Map<Long, Double> results = new HashMap<>();
			for (ProcessDescriptor process : processes) {
				double amount = provider.getSingleImpactResult(process, impact)
						.getValue();
				results.put(process.getId(), amount);
			}
			setResultData(results);
		}

		private void setResultData(Map<Long, Double> results) {
			Map<LongPair, KmlFeature> features = result.getKmlFeatures();
			List<LongPair> keys = keysSortedByFeatures(features);
			double maximum = getMaximum(features, keys, results);
			evaluate("initData(" + maximum + ")");
			if (maximum == 0d)
				return;
			KmlItem nextItem = null;
			String lastFeatureId = null;
			for (LongPair processProduct : keys) {
				KmlFeature feature = features.get(processProduct);
				if (!feature.getIdentifier().equals(lastFeatureId)) {
					sendToView(nextItem);
					nextItem = null;
				}
				lastFeatureId = feature.getIdentifier();
				Double result = results.get(processProduct.getFirst());
				if (result == null)
					continue;
				if (nextItem == null)
					nextItem = createItem(feature, result);
				else
					nextItem.amount += result;
			}
			if (nextItem != null)
				sendToView(nextItem);
		}

		private double getMaximum(Map<LongPair, KmlFeature> features,
				List<LongPair> sortedKeys, Map<Long, Double> results) {
			String lastFeatureId = null;
			double maximum = 0;
			double current = 0;
			for (LongPair processProduct : sortedKeys) {
				KmlFeature feature = features.get(processProduct);
				if (!feature.getIdentifier().equals(lastFeatureId)) {
					maximum = Math.max(maximum, current);
					current = 0;
				}
				lastFeatureId = feature.getIdentifier();
				Double result = results.get(processProduct.getFirst());
				if (result == null)
					continue;
				current += result;
			}
			return Math.max(maximum, current);
		}

		private void sendToView(final KmlItem item) {
			if (item == null)
				return;
			if (item.amount == 0d)
				return;
			App.runInUI("Setting items", () -> evaluate(item));
		}

		private void evaluate(Object value) {
			String command = null;
			if (value instanceof String)
				command = value.toString();
			else {
				Gson gson = new Gson();
				String json = gson.toJson(value);
				command = "addFeature(" + json + ")";
			}
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to evaluate " + value, e);
			}
		}

		private List<LongPair> keysSortedByFeatures(
				Map<LongPair, KmlFeature> features) {
			List<LongPair> keys = new ArrayList<LongPair>(features.keySet());
			Collections.sort(keys, new ByFeatureComparator(features));
			return keys;
		}

		private KmlItem createItem(KmlFeature feature, double result) {
			KmlItem item = new KmlItem();
			item.amount = result;
			item.kml = feature.getKml();
			return item;
		}
	}

	@SuppressWarnings("unused")
	private class KmlItem {
		double amount;
		String kml;
	}

	private class ByFeatureComparator implements Comparator<LongPair> {

		private Map<LongPair, KmlFeature> features;

		private ByFeatureComparator(Map<LongPair, KmlFeature> features) {
			this.features = features;
		}

		@Override
		public int compare(LongPair o1, LongPair o2) {
			KmlFeature f1 = features.get(o1);
			String id1 = f1 != null ? f1.getIdentifier() : "";
			id1 = id1 != null ? id1 : "";
			KmlFeature f2 = features.get(o2);
			String id2 = f2 != null ? f2.getIdentifier() : "";
			id2 = id2 != null ? id2 : "";
			return id1.compareTo(id2);
		}

	}

}

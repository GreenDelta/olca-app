package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.geo.RegionalizedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import javafx.scene.web.WebEngine;

class KmlResultView extends FormPage implements WebPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RegionalizedResult result;
	private WebEngine webkit;
	private ResultTypeSelection flowImpactSelection;
	private boolean incompleteData = false;
	private boolean loaded;
	private CalculationSetup setup;

	public KmlResultView(FormEditor editor, RegionalizedResult result, CalculationSetup setup) {
		super(editor, "KmlResultView", "Result map");
		this.result = result;
		this.setup = setup;
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_RESULT_VIEW.getUrl();
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		loaded = true;
		Set<FlowDescriptor> flowDescriptors = result.result.getFlows();
		if (flowDescriptors.isEmpty())
			return;
		FlowDescriptor flow = flowDescriptors.iterator().next();
		flowImpactSelection.selectWithEvent(flow);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result.result));
		FormToolkit toolkit = mform.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = ResultTypeSelection
				.on(result.result)
				.withEventHandler(new KmlSelectionHandler(result))
				.create(composite, toolkit);
		Control browser = UI.createWebView(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (incompleteData && webkit != null) {
			// reset browser data
			try {
				webkit.executeScript("initData()");
			} catch (Exception e) {
				log.error("failed to evaluate initData()", e);
			}
		}
	}

	private class KmlSelectionHandler extends SelectionHandler {

		private List<Job> delayedJobs = new ArrayList<>();

		private KmlSelectionHandler(RegionalizedResult result) {
			super(result);
		}

		@Override
		protected void processResultData(List<LocationResult> results) {
			if (!loaded)
				return;
			delayedJobs.clear();
			incompleteData = false;
			double maximum = getMaximum(results);
			evaluate("initData(" + maximum + ")");
			for (LocationResult result : results)
				sendToView(result);
		}

		private double getMaximum(List<LocationResult> results) {
			Double maximum = null;
			for (LocationResult result : results)
				if (maximum == null)
					maximum = result.amount;
				else
					maximum = Math.max(maximum, result.amount);
			if (maximum == null)
				return 0;
			return maximum;
		}

		private void sendToView(LocationResult result) {
			if (result == null)
				return;
			if (result.amount == 0d)
				return;
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("kml", result.kmlFeature.kml);
			item.put("amount", result.amount);
			delayedJobs.add(App.runInUI("Setting item", () -> evaluate(item)));
		}

		private void evaluate(Object value) {
			if (!isActive()) {
				incompleteData = true;
				cancelDelayedJobs();
				return;
			}
			if (webkit == null)
				return;
			String command = null;
			if (value instanceof String)
				command = value.toString();
			else {
				Gson gson = new Gson();
				String json = gson.toJson(value);
				command = "addFeature(" + json + ")";
			}
			try {
				webkit.executeScript(command);
			} catch (Exception e) {
				log.error("failed to evaluate " + value, e);
			}
		}

		private void cancelDelayedJobs() {
			for (Job job : delayedJobs)
				job.cancel();
		}

	}

}

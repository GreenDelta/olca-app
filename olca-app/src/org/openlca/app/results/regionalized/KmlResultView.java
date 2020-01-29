package org.openlca.app.results.regionalized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.ResultTypeCombo;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.geo.RegionalizedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

class KmlResultView extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private RegionalizedResult result;
	private ResultTypeCombo flowImpactSelection;
	private boolean incompleteData = false;
	private boolean loaded;
	private CalculationSetup setup;

	private Browser browser;

	public KmlResultView(FormEditor editor,
			RegionalizedResult result, CalculationSetup setup) {
		super(editor, "KmlResultView", "Result map");
		this.result = result;
		this.setup = setup;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.name(setup.productSystem),
				Images.get(result.result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 2);
		flowImpactSelection = ResultTypeCombo
				.on(result.result)
				.withEventHandler(new KmlSelectionHandler(result))
				.create(comp, tk);
		browser = new Browser(body, SWT.NONE);
		browser.setJavascriptEnabled(true);

		UI.onLoaded(browser, HtmlFolder.getUrl("kml_results.html"), () -> {
			loaded = true;
			flowImpactSelection.initWithEvent();
		});

		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (incompleteData && browser != null) {
			// reset browser data
			try {
				browser.execute("initData()");
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
			evaluate("init(" + maximum + ")");
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
			if (browser == null)
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
				browser.execute(command);
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

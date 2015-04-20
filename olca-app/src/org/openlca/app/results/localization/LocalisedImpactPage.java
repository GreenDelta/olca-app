package org.openlca.app.results.localization;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.results.localization.LocalisedImpactResult.Entry;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.ContributionResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class LocalisedImpactPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionResultProvider<?> result;
	private Browser browser;
	private CalculationSetup setup;

	public LocalisedImpactPage(FormEditor editor,
			ContributionResultProvider<?> result, CalculationSetup setup) {
		super(editor, "analyse.LocalisedImpactPage", "Localised LCIA (beta)");
		this.result = result;
		this.setup = setup;
	}

	@Override
	public String getUrl() {
		return HtmlView.RESULT_LOCALISED_LCIA.getUrl();
	}

	@Override
	public void onLoaded() {
		final ResultProvider resultProvider = new ResultProvider();
		App.run("Calculate localised result", resultProvider, new Runnable() {
			@Override
			public void run() {
				if (resultProvider.json == null)
					return;
				try {
					String command = "setData(" + resultProvider.json + ")";
					browser.evaluate(command);
				} catch (Exception e) {
					log.error("Failed to calculate and set result", e);
				}
			}
		});
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				"Localised Impact Assessment (beta)");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	private class ResultProvider implements Runnable {

		private String json;

		@Override
		public void run() {
			LocalisedImpactMethod method = loadMethod();
			if (method == null)
				return;
			LocalisedImpactCalculator calculator = new LocalisedImpactCalculator(
					result, method);
			LocalisedImpactResult result = calculator.calculate(Cache
					.getEntityCache());
			json = new JsonConverter().toJson(result);
		}

		private LocalisedImpactMethod loadMethod() {
			ImpactMethodDescriptor impactMethod = setup.getImpactMethod();
			if (!result.hasImpactResults() || impactMethod == null)
				return null;
			return LocalisedMethodStorage.getOrCreate(Database.get(),
					impactMethod.getRefId());
		}

	}

	/**
	 * Converts the localised impact assessment result into a Json string that
	 * can be passed to the HTML view.
	 */
	private static class JsonConverter {

		public String toJson(LocalisedImpactResult result) {
			JsonObject methodJson = createJsonResult(result);
			return new Gson().toJson(methodJson);
		}

		private JsonObject createJsonResult(LocalisedImpactResult result) {
			JsonObject methodJson = new JsonObject();
			methodJson
					.addProperty("method", result.getImpactMethod().getName());
			JsonArray categoriesJson = new JsonArray();
			methodJson.add("categories", categoriesJson);
			for (ImpactCategoryDescriptor category : result
					.getImpactCategories()) {
				JsonObject categoryJson = new JsonObject();
				categoriesJson.add(categoryJson);
				categoryJson.addProperty("name", category.getName());
				categoryJson.addProperty("unit", category.getReferenceUnit());
				JsonArray resultsJson = createJsonResults(result, category);
				categoryJson.add("results", resultsJson);
			}
			return methodJson;
		}

		private JsonArray createJsonResults(LocalisedImpactResult result,
				ImpactCategoryDescriptor category) {
			JsonArray resultsJson = new JsonArray();
			for (Entry entry : result.getEntries(category)) {
				JsonObject resultJson = new JsonObject();
				resultsJson.add(resultJson);
				JsonObject locationJson = new JsonObject();
				locationJson.addProperty("name", entry.getLocation().getName());
				locationJson.addProperty("code", entry.getLocation().getCode());
				resultJson.add("location", locationJson);
				resultJson.addProperty("result", entry.getResult());
				resultJson.addProperty("localisedResult",
						entry.getLocalResult());
			}
			return resultsJson;
		}
	}

}

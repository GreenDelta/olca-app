package org.openlca.app.editors.lcia_methods;

import java.io.File;
import java.util.Collections;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.HtmlView;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.results.localization.LocalisedImpactCategory;
import org.openlca.app.results.localization.LocalisedImpactMethod;
import org.openlca.app.results.localization.LocalisedMethodExport;
import org.openlca.app.results.localization.LocalisedMethodImport;
import org.openlca.app.results.localization.LocalisedMethodStorage;
import org.openlca.app.util.InformationPopup;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The page shows localised impact assessment factors of an impact assessment
 * method. If there is no localised method available yet, it is created when the
 * page is initialised the first time.
 */
public class ImpactLocalisationPage extends ModelPage<ImpactMethod> implements
		HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Browser browser;
	private LocalisedImpactMethod localisedMethod;
	private IDatabase database = Database.get();
	private ImpactMethodEditor editor;

	public ImpactLocalisationPage(ImpactMethodEditor editor) {
		super(editor, "ImpactLocalisationPage", "Regionalisation (beta)");
		this.editor = editor;
	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.IMPACT_LOCALISATION_PAGE.getResource();
	}

	@Override
	public void onLoaded() {
		App.run("Initialise localised method", new Initializer(),
				new Runnable() {
					@Override
					public void run() {
						new ExportFunction(browser);
						new ImportFunction(browser);
						setBrowserData();
					}
				});
	}

	private void setBrowserData() {
		try {
			Gson gson = new Gson();
			String json = gson.toJson(localisedMethod);
			String command = "setData(" + json + ")";
			browser.evaluate(command);
		} catch (Exception e) {
			log.error("Failed to set browser data", e);
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI
				.formHeader(managedForm, "Regionalisation (beta)");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		body.setFocus();
		form.reflow(true);
	}

	private class ExportFunction extends BrowserFunction {

		public ExportFunction(Browser browser) {
			super(browser, "runExport");
		}

		@Override
		public Object function(Object[] arguments) {
			if (localisedMethod == null)
				return null;
			log.trace("Export localised impact assessment method");
			String fileName = localisedMethod.getImpactMethod().getName()
					+ ".xls";
			File file = FileChooser.forExport("*.xls", fileName);
			if (file == null)
				return null;
			final LocalisedMethodExport export = new LocalisedMethodExport(
					localisedMethod, file, database, Cache.getEntityCache());
			App.run("Export impact method", export, new Runnable() {
				@Override
				public void run() {
					if (export.isFinishedWithSuccess())
						InformationPopup.show("Export done");
				}
			});
			return null;
		}
	}

	private class ImportFunction extends BrowserFunction {

		public ImportFunction(Browser browser) {
			super(browser, "runImport");
		}

		@Override
		public Object function(Object[] arguments) {
			log.trace("Import localised impact assessment method");
			boolean b = Question.ask("Overwrite existing method?",
					"This will overwrite the existing "
							+ "localisation set. Do you want to continue?");
			if (!b)
				return null;
			File file = FileChooser.forImport("*.xls");
			if (file == null)
				return null;
			runImport(file);
			return null;
		}

		private void runImport(File file) {
			final LocalisedMethodImport methodImport = new LocalisedMethodImport(
					file, database, Cache.getEntityCache());
			App.run("Import LCIA method", methodImport, new Runnable() {
				@Override
				public void run() {
					if (methodImport.isFinishedWithSuccess()) {
						localisedMethod = methodImport.getMethod();
						LocalisedMethodStorage.save(localisedMethod, database);
						setBrowserData();
						InformationPopup.show("Import done");
					}
				}
			});
		}

	}

	/** Initialises the localised impact assessment method. */
	private class Initializer implements Runnable {

		@Override
		public void run() {
			try {
				initialise();
			} catch (Exception e) {
				log.error("Failed to initialze method", e);
			}
		}

		private void initialise() {
			ImpactMethod method = editor.getModel();
			loadMethod(method);
			for (LocalisedImpactCategory cat : localisedMethod
					.getImpactCategories())
				Collections.sort(cat.getFactors());
		}

		private void loadMethod(ImpactMethod method) {
			localisedMethod = LocalisedMethodStorage.getOrCreate(database,
					method.getRefId());
		}
	}

}

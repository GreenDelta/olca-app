package org.openlca.core.editors.lciamethod;

import java.io.File;
import java.util.Collections;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.core.application.App;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.HtmlView;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.editors.io.LocalisedMethodExport;
import org.openlca.core.editors.io.LocalisedMethodImport;
import org.openlca.core.editors.io.LocalisedMethodStorage;
import org.openlca.core.editors.io.ui.FileChooser;
import org.openlca.core.editors.model.LocalisedImpactCategory;
import org.openlca.core.editors.model.LocalisedImpactMethod;
import org.openlca.core.jobs.Status;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.HtmlPage;
import org.openlca.ui.InformationPopup;
import org.openlca.ui.Question;
import org.openlca.ui.UI;
import org.openlca.ui.html.IHtmlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The page shows localised impact assessment factors of an impact assessment
 * method. If there is no localised method available yet, it is created when the
 * page is initialised the first time.
 */
public class ImpactLocalisationPage extends ModelEditorPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Browser browser;
	private LocalisedImpactMethod localisedMethod;
	private IDatabase database;
	private LCIAMethodEditor editor;

	public ImpactLocalisationPage(LCIAMethodEditor editor) {
		super(editor, "ImpactLocalisationPage", "Regionalisation (beta)");
		this.database = editor.getDatabase();
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
	protected void createContents(Composite body, FormToolkit toolkit) {
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
	}

	@Override
	protected String getFormTitle() {
		return "Regionalisation (beta)";
	}

	@Override
	protected void setData() {
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
					localisedMethod, file, database);
			App.run("Export impact method", export, new Runnable() {
				@Override
				public void run() {
					if (export.getStatus().getFlag() == Status.OK)
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
					file, database);
			App.run("Import LCIA method", methodImport, new Runnable() {
				@Override
				public void run() {
					if (methodImport.getStatus().getFlag() == Status.OK) {
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
			IModelComponent comp = editor.getModelComponent();
			if (!(comp instanceof LCIAMethod))
				return;
			loadMethod(comp);
			for (LocalisedImpactCategory cat : localisedMethod
					.getImpactCategories())
				Collections.sort(cat.getFactors());
		}

		private void loadMethod(IModelComponent comp) {
			LCIAMethod method = (LCIAMethod) comp;
			localisedMethod = LocalisedMethodStorage.getOrCreate(database,
					method.getId());
		}
	}

}

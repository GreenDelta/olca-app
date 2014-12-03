package org.openlca.app.editors.systems;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class StatisticsPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem system;
	private Browser browser;

	public StatisticsPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemStatisticsPage", Messages.Statistics);
		system = editor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.ProductSystemStatistics);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	@Override
	public String getUrl() {
		return HtmlView.PRODUCT_SYSTEM_STATISTICS.getUrl();
	}

	@Override
	public void onLoaded() {
		new BrowserFunction(browser, "openProcess") {
			@Override
			public Object function(Object[] args) {
				openProcess(args);
				return null;
			}
		};
		new BrowserFunction(browser, "recalculate") {
			@Override
			public Object function(Object[] args) {
				calculate();
				return null;
			}
		};
		calculate();
	}

	private void openProcess(Object[] args) {
		if (args == null || args.length == 0)
			return;
		if (!(args[0] instanceof String))
			return;
		try {
			String json = (String) args[0];
			ProcessDescriptor descriptor = new Gson().fromJson(json,
					ProcessDescriptor.class);
			App.openEditor(descriptor);
		} catch (Exception e) {
			log.error("failed to open process " + args[0], e);
		}
	}

	private void calculate() {
		Statistics[] stats = new Statistics[1];
		App.run("@Calculate statistics",
				() -> {
					stats[0] = Statistics.calculate(system,
							Cache.getEntityCache());
				},
				() -> setBrowserData(stats[0]));
	}

	private void setBrowserData(Statistics statistics) {
		if (statistics == null)
			return;
		try {
			String json = statistics.toJson();
			String command = "setData(" + json + ")";
			browser.evaluate(command);
		} catch (Exception e) {
			log.error("failed to set browser data", e);
		}
	}
}

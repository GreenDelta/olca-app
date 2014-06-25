package org.openlca.app.editors.systems;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.HtmlView;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem system;
	private Browser browser;

	public StatisticsPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemStatisticsPage", "Statistics");
		system = editor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				"Product system statistics");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.PRODUCT_SYSTEM_STATISTICS.getResource();
	}

	@Override
	public void onLoaded() {
		calculate();
	}

	private void calculate() {
		Statistics[] stats = new Statistics[1];
		App.run("Calculate statistics",
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
			System.out.println(json);
			// String command = "setData(" + json + ")";
			// browser.evaluate(command);
		} catch (Exception e) {
			log.error("failed to set browser data", e);
		}
	}
}

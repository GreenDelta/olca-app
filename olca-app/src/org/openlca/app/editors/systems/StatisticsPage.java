package org.openlca.app.editors.systems;

import javafx.scene.web.WebEngine;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class StatisticsPage extends FormPage implements WebPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem system;
	private WebEngine webkit;

	public StatisticsPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemStatisticsPage", M.Statistics);
		system = editor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				M.ProductSystemStatistics);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new FillLayout());
		UI.createWebView(body, this);
		form.reflow(true);
	}

	@Override
	public String getUrl() {
		return HtmlView.PRODUCT_SYSTEM_STATISTICS.getUrl();
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		UI.bindVar(webkit, "java", new JsHandler());
		calculate();
	}

	private void calculate() {
		Statistics[] stats = new Statistics[1];
		App.run(M.CalculateStatistics,
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
			webkit.executeScript(command);
		} catch (Exception e) {
			log.error("failed to set browser data", e);
		}
	}

	public class JsHandler {

		public void openProcess(String json) {
			try {
				ProcessDescriptor descriptor = new Gson().fromJson(json,
						ProcessDescriptor.class);
				App.openEditor(descriptor);
			} catch (Exception e) {
				log.error("failed to open process " + json, e);
			}
		}

		public void recalculate() {
			calculate();
		}
	}
}

package org.openlca.app.rcp.html;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.gson.Gson;

public abstract class HtmlFormPage extends FormPage implements HtmlPage {

	private Browser browser;
	private String header;
	private FormToolkit toolkit;

	public HtmlFormPage(FormEditor editor, String id, String title,
			String header) {
		super(editor, id, title);
		this.header = header;
	}

	protected void call(String method, Object... args) {
		String command = method + "(";
		if (args != null) {
			Gson gson = new Gson();
			List<String> strArgs = new ArrayList<>();
			for (Object arg : args)
				strArgs.add(gson.toJson(arg));
			command += Joiner.on(',').join(strArgs);
		}
		command += ")";
		try {
			browser.evaluate(command);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to call JavaScript function " + method, e);
		}
	}

	protected void createContentBeforeBrowser(Composite body) {

	}

	protected void createContentAfterBrowser(Composite body) {

	}

	protected FormToolkit getToolkit() {
		return toolkit;
	}

	public Browser getBrowser() {
		return browser;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, header);
		toolkit = managedForm.getToolkit();
		Composite body = form.getBody();
		body.setLayout(UI.gridLayout(body, 1, 0, 0));
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		createContentBeforeBrowser(body);
		createBrowser(body);
		createContentAfterBrowser(body);
		form.reflow(true);
	}

	private void createBrowser(Composite body) {
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 1);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
	}

}

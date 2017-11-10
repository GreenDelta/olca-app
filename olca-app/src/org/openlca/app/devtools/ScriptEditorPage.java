package org.openlca.app.devtools;

import javafx.scene.web.WebEngine;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScriptEditorPage extends FormPage implements WebPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private WebEngine webkit;
	private Image image;
	
	public ScriptEditorPage(FormEditor editor, String id, String title, Image image) {
		super(editor, id, title);
		this.image = image;
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, getTitle(), image);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new FillLayout());
		UI.createWebView(body, this);
	}

	public String getScript() {
		try {
			Object script = webkit.executeScript("getContent();");
			if (script == null)
				return "";
			return script.toString();
		} catch (Exception e) {
			log.error("failed to get script content", e);
			return "";
		}
	}
}

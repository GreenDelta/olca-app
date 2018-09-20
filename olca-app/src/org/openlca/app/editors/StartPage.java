package org.openlca.app.editors;

import java.util.UUID;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.EclipseCommandLine;
import org.openlca.app.util.UI;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public class StartPage extends SimpleFormEditor {

	public static String TYPE = "olca.StartPage";
	private static Logger log = LoggerFactory.getLogger(StartPage.class);

	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, UUID.randomUUID().toString(), M.Welcome), TYPE);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage implements WebPage {

		public Page() {
			super(StartPage.this, "olca.StartPage.Page", M.Welcome);
		}

		@Override
		public String getUrl() {
			String langCode = EclipseCommandLine.getArg("nl");
			if (langCode == null || "en".equalsIgnoreCase(langCode)
					|| langCode.startsWith("en_"))
				return HtmlView.START_PAGE.getUrl();
			String pageName = "start_page_" + langCode + ".html";
			try {
				return HtmlFolder.getUrl(RcpActivator.getDefault().getBundle(),
						pageName);
			} catch (Exception e) {
				log.error("failed to get start page for language " + langCode,
						e);
				return HtmlView.START_PAGE.getUrl();
			}
		}

		@Override
		public void onLoaded(WebEngine webkit) {
			JSObject win = (JSObject) webkit.executeScript("window");
			win.setMember("java", new JsHandler());
			String version = M.Version + " " + App.getVersion() + " ("
					+ OS.getCurrent() + " " + getArch() + ")";
			webkit.executeScript("document.getElementById('version').innerHTML = '"
					+ version + "'");
		}

		private String getArch() {
			String osarch = System.getProperty("os.arch");
			if (osarch == null)
				return "";
			switch (osarch) {
			case "amd64":
				return "64 bit";
			case "x86":
				return "32 bit";
			case "i386":
				return "32 bit";
			default:
				return osarch;
			}
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			Composite composite = form.getBody();
			composite.setLayout(new FillLayout());
			UI.createWebView(composite, this);
		}

	}

	public class JsHandler {

		public void openUrl(String url) {
			log.trace("js-callback: openUrl");
			if (url == null) {
				log.warn("openUrl: no url given");
				return;
			}
			log.trace("open URL {}", url);
			Desktop.browse(url);
		}
	}
}

package org.openlca.app.editors;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.AppArg;
import org.openlca.app.M;
import org.openlca.app.preferences.LibraryDownload;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.openlca.util.OS;
import org.openlca.util.Strings;

import com.google.gson.Gson;

public class StartPage extends SimpleFormEditor {

	public static void open() {
		var input = new SimpleEditorInput("olca.StartPage", M.Welcome);
		Editors.open(input, "olca.StartPage");
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		public Page() {
			super(StartPage.this, "olca.StartPage.Page", M.Welcome);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = mform.getForm();
			Composite comp = form.getBody();
			comp.setLayout(new FillLayout());
			Browser browser = new Browser(comp, SWT.NONE);
			browser.setJavascriptEnabled(true);

			// handles link clicks and opens them in the browser
			UI.bindFunction(browser, "onOpenLink", (args) -> {
				if (args == null || args.length == 0)
					return null;
				Object s = args[0];
				if (!(s instanceof String))
					return null;
				Desktop.browse(s.toString());
				return null;
			});

			// handles click on the "native library hint"
			UI.bindFunction(browser, "onLibHintClick", (args) -> {
				LibraryDownload.open();
				return null;
			});

			// set the start page configuration
			UI.onLoaded(browser, HtmlFolder.getUrl("home.html"), () -> {
				var config = new HashMap<String, Object>();
				config.put("version", getVersion());
				var lang = AppArg.get("nl");
				config.put("lang", Strings.nullOrEmpty(lang) ? "en" : lang);
				config.put("showLibHint", !NativeLib.isLoaded(Module.UMFPACK));
				var json = new Gson().toJson(config);
				browser.execute("setData(" + json + ")");
			});
		}

		private String getVersion() {
			String v = App.getVersion();
			String build = AppArg.BUILD_NUMBER.getValue();
			if (Strings.notEmpty(build)) {
				v += " " + build;
			}
			v += " (" + OS.get();
			String osarch = System.getProperty("os.arch");
			if (Strings.notEmpty(osarch)) {
				v += " " + osarch;
			}
			return v + ")";
		}
	}
}

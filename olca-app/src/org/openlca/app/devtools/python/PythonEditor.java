package org.openlca.app.devtools.python;

import java.io.File;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.devtools.ScriptingEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class PythonEditor extends ScriptingEditor {

	private Page page;

	public static void open() {
		var id = UUID.randomUUID() + "_new";
		var input = new SimpleEditorInput(id, "Python");
		Editors.open(input, "PythonEditor");
	}

	public static void open(File file) {
		if (file == null || !file.exists())
			return;
		var id = file.getAbsolutePath();
		var input = new SimpleEditorInput(id, "Python");
		Editors.open(input, "PythonEditor");
	}

	@Override
	public void eval() {
		var script = page.getScript();
		App.run("Eval script", () -> Jython.exec(script));
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	private class Page extends FormPage {

		private Browser browser;

		public Page() {
			super(PythonEditor.this, "PythonEditorPage", "Python");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, getTitle(), Icon.PYTHON.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			body.setLayout(new FillLayout());
			try {
				browser = new Browser(body, SWT.NONE);
				browser.setJavascriptEnabled(true);

				// initialize the script
				UI.onLoaded(browser, HtmlFolder.getUrl("python.html"), () -> {

					// set the script content
					if (Strings.notEmpty(script)) {
						var js = script.replace("'", "\\'")
								.replaceAll("\\r?\\n", "\\\\n");
						browser.execute("setContent('" + js + "')");
					}

					// add the _onChange listener
					UI.bindFunction(browser, "_onChange", (args) -> {
						if (args == null || args.length == 0)
							return null;
						var arg = args[0] == null
								? null
								: args[0].toString();
						if (arg != null) {
							script = arg;
							setDirty();
						}
						return null;
					});
				});

			} catch (Exception e) {
				ErrorReporter.on("failed to create browser in Python editor", e);
			}
		}

		public String getScript() {
			try {
				var script = browser.evaluate("return getContent();");
				return script != null
						? script.toString()
						: "";
			} catch (Exception e) {
				ErrorReporter.on("failed to get script content", e);
				return "";
			}
		}
	}
}

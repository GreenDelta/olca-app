package org.openlca.app.devtools.python;

import java.io.File;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.devtools.ScriptingEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.logging.Console;
import org.openlca.app.logging.LoggerPreference;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.RcpActivator;
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
		Console.show();
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
						browser.execute("setContent(" + toJavaScript(script) + ")");
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

		/**
		 * We use a browser view as script editor. To initialize the browser view
		 * with an existing script, we need to pass the script to the browser
		 * by calling a JavaScript function with the script as string parameter.
		 * For this, we need to convert the script into a valid JavaScript string
		 * so that we can pass it to that function.
		 *
		 * @param script the raw string of the script as saved on disk
		 * @return the JavaScript string of the script enclosed in single-quotes.
		 */
		private static String toJavaScript(String script) {
			if (Strings.nullOrEmpty(script))
				return "''";
			var buffer = new StringBuilder("'");
			for (int i = 0; i < script.length(); i++) {
				char c = script.charAt(i);
				switch (c) {
					case '\r':
						break;
					case '\n':
						buffer.append("\\n");
						break;
					case '\'':
						buffer.append("\\'");
						break;
					case '\\':
						buffer.append("\\\\");
						break;
					default:
						buffer.append(c);
						break;
				}
			}
			buffer.append("'");
			return buffer.toString();
		}
	}
}

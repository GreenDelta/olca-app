package org.openlca.app.devtools.python;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.devtools.SaveScriptDialog;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonEditor extends SimpleFormEditor implements IScriptEditor {

	public static final String TYPE = "PythonEditor";

	private File file;
	private String script = "";
	private boolean _dirty;
	private Page page;

	public static void open() {
		var id = UUID.randomUUID().toString() + "_new";
		var input = new SimpleEditorInput(TYPE, id, "Python");
		Editors.open(input, TYPE);
	}

	public static void open(File file) {
		if (file == null || !file.exists())
			return;
		var id = file.getAbsolutePath();
		var input = new SimpleEditorInput(TYPE, id, "Python");
		Editors.open(input, TYPE);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (!(input instanceof SimpleEditorInput))
			return;
		var id = ((SimpleEditorInput) input).id;
		if (id.endsWith("_new"))
			return;
		var file = new File(id);
		if (!file.exists())
			return;
		this.file = file;
		try {
			script = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			setPartName(file.getName());
		} catch (Exception e) {
			MsgBox.error("Failed to read script",
					"Failed to read script " + file + ": " + e.getMessage());
		}
	}

	private void setDirty() {
		// can only set the editor dirty if there is a file
		if (file == null)
			return;
		_dirty = true;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	@Override
	public void eval() {
		var script = page.getScript();
		App.run("Eval script", () -> Python.exec(script));
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (file == null)
			return;
		try {
			Files.writeString(
					file.toPath(),
					script,
					StandardCharsets.UTF_8);
		} catch (Exception e) {
			MsgBox.error(
					"Failed to save script",
					"Failed to save script " + file + ": " + e.getMessage());
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		String name = "script.py";
		if (file != null) {
			name = file.getName();
			if (name.endsWith(".py")) {
				name = name.substring(0, name.length() - 3);
			}
			name += "_copy.py";
		}
		var newFile = SaveScriptDialog.forScriptOf(name, script)
				.orElse(null);
		if (file == null && newFile != null) {
			file = newFile;
			setPartName(file.getName());
		}
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

				// initialize the script
				UI.onLoaded(browser, HtmlFolder.getUrl("python.html"), () -> {
					if (Strings.nullOrEmpty(script))
						return;
					var js = script.replace("'", "\\'")
							.replaceAll("\\r?\\n", "\\n");
					browser.execute("setContent('" + js + "')");
				});

			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to create browser in Python editor", e);
			}
		}

		public String getScript() {
			try {
				var script = browser.evaluate("return getContent();");
				return script != null
						? script.toString()
						: "";
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to get script content", e);
				return "";
			}
		}
	}
}

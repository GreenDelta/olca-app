package org.openlca.app.devtools.python;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.devtools.IScriptEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonEditor extends SimpleFormEditor implements IScriptEditor {

	public static String TYPE = "PythonEditor";
	private Page page;
	private File file;

	public static void open() {
		Editors.open(new SimpleEditorInput(
				TYPE, UUID.randomUUID().toString(), "Python"), TYPE);
	}

	public static void open(File file) {
		var input = new SimpleEditorInput(
				TYPE, file.getAbsolutePath(), file.getName());
		Editors.open(input, TYPE);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (!(input instanceof SimpleEditorInput))
			return;
		var inp = (SimpleEditorInput) input;

		// if the ID ends with `py` we assume that this is
		// a path to a script file
		if (!inp.id.endsWith(".py"))
			return;
		setPartName(inp.getName());
		var file = new File(inp.id);
		if (file.exists()) {
			this.file = file;
		}
	}

	@Override
	protected FormPage getPage() {
		return page = new Page();
	}

	@Override
	public void eval() {
		String script = page.getScript();
		App.run("Eval script", () -> Python.exec(script));
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		var dialog = new InputDialog(
				UI.shell(),
				"Save script as file",
				"Please enter a file name for the new script",
				file != null ? file.getName() : "script.py",
				name -> {
					try {
						if (Strings.nullOrEmpty(name))
							return "The file name cannot be empty";
						Paths.get(name);
						return null;
					} catch (Exception e) {
						return name + " is not a valid file name";
					}
				});

		if (dialog.open() != Window.OK)
			return;
		var name = dialog.getValue();
		if (!name.endsWith(".py")) {
			name += ".py";
		}

		// check if the file already exists
		var scriptDir = new File(Workspace.getDir(), "Scripts");
		if (!scriptDir.exists()) {
			var created = scriptDir.mkdirs();
			if (!created) {
				MsgBox.error("Could not create `scripts`" +
						" folder in workspace");
				return;
			}
		}
		var file = new File(scriptDir, name);
		if (file.exists()) {
			var b = Question.ask(M.FileAlreadyExists,
					M.OverwriteFileQuestion);
			if (!b)
				return;
		}

		// finally, write the file
		try {
			Files.writeString(file.toPath(), page.getScript());
			Navigator.refresh();
		} catch (Exception e) {
			MsgBox.error("Failed to save script: " + e.getMessage());
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
				UI.onLoaded(browser,
						HtmlFolder.getUrl("python.html"), this::initScript);
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

		private void initScript() {
			if (file == null)
				return;
			try {
				var script = Files.readString(file.toPath())
						.replace("'", "\\'")
						.replaceAll("\\n", "\\\\n");
				browser.execute("setContent('" + script + "')");
			} catch (Exception e) {
				MsgBox.error("Failed to set script from file: " + file.getName());
			}
		}
	}
}

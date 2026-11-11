package org.openlca.app.logging;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.preferences.Theme;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;

public class LogFileEditor extends FormEditor {

	public static void open() {
		var files = HtmlLog.files();
		if (files.isEmpty()) {
			MsgBox.info(M.NoLogFileAvailable, M.NoLogFileAvailableInfo);
			return;
		}
		var input = new SimpleEditorInput(M.LogFiles, M.LogFile);
		Editors.open(input, "editors.LogFileEditor");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.FILE.get());
	}

	/// Adds a page for each log file, the newest file first.
	@Override
	protected void addPages() {
		try {
			for (File file : HtmlLog.files()) {
				addPage(new LogFilePage(file));
			}
		} catch (PartInitException e) {
			ErrorReporter.on("Failed to add log file editor page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private class LogFilePage extends FormPage {

		private final File file;

		public LogFilePage(File file) {
			super(LogFileEditor.this,
				"org.openlca.app.editors.LogFileEditor." + file.getName(),
				HtmlLog.dateOf(file));
			this.file = file;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = mform.getForm();
			var toolkit = mform.getToolkit();
			UI.header(mform, M.OpenLCALog);
			var body = UI.body(form, toolkit);
			var browser = new Browser(body, SWT.NONE);
			UI.gridData(browser, true, true);
			try {
				browser.setUrl(file.toURI().toURL().toString());
				if (Theme.isDark())
					browser.execute("document.querySelector(\"body\").style.background=\"#2b2b2b\"");
			} catch (IOException e) {
				ErrorReporter.on("Error loading log file: " + file, e);
			}
		}
	}
}

package org.openlca.app.logging;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;

public class LogFileEditor extends FormEditor {

	public static void open() {
		var files = logFiles();
		if (files.isEmpty()) {
			MsgBox.info("No log-files available",
				"Could not find any log file in the openLCA workspace.");
			return;
		}
		var input = new SimpleEditorInput("Log files", "Log file");
		Editors.open(input, "editors.LogFileEditor");
	}

	private static List<File> logFiles() {
		var logDir = new File(Workspace.root(), "log");
		if (!logDir.exists())
			return Collections.emptyList();
		var files = logDir.listFiles();
		return files == null
			? Collections.emptyList()
			: Arrays.asList(files);
	}

	@Override
	protected void addPages() {
		try {
			int count = 1;
			for (File file : logFiles()) {
				addPage(new LogFilePage(file, count++));
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

		public LogFilePage(File file, int count) {
			super(LogFileEditor.this,
				"org.openlca.app.editors.LogFileEditor",
				M.LogFile + " " + count);
			this.file = file;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = mform.getForm();
			var toolkit = mform.getToolkit();
			UI.formHeader(mform, M.OpenLCALog);
			var body = UI.formBody(form, toolkit);
			var browser = new Browser(body, SWT.NONE);
			UI.gridData(browser, true, true);
			try {
				browser.setUrl(file.toURI().toURL().toString());
			} catch (IOException e) {
				ErrorReporter.on("Error loading log file: " + file, e);
			}
		}
	}
}

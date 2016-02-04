package org.openlca.app.editors;

import org.openlca.app.M;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.logging.HtmlLogFile;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFileEditor extends FormEditor {

	public static final String ID = "editors.LogFileEditor";
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	protected void addPages() {
		try {
			int count = 1;
			for (File file : HtmlLogFile.getAllFiles())
				addPage(new LogFilePage(file, count++));
		} catch (PartInitException e) {
			log.error("Failed to add log file editor page", e);
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

		private File file;

		public LogFilePage(File file, int count) {
			super(LogFileEditor.this, "org.openlca.app.editors.LogFileEditor", M.LogFile + count);
			this.file = file;
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = managedForm.getForm();
			FormToolkit toolkit = managedForm.getToolkit();
			UI.formHeader(managedForm, M.OpenLCALog);
			Composite body = UI.formBody(form, toolkit);
			Browser browser = UI.createBrowser(body);
			UI.gridData(browser, true, true);
			try {
				String html = new String(Files.readAllBytes(file.toPath()));
				browser.setText(html);
			} catch (IOException e) {
				log.error("Error loading log files", e);
			}
		}

	}

}

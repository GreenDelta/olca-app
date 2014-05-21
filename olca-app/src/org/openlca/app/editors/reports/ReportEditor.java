package org.openlca.app.editors.reports;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.editors.IEditor;
import org.openlca.app.util.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportEditor extends FormEditor implements IEditor {

	public static String ID = "ReportEditor";

	private Logger log = LoggerFactory.getLogger(getClass());

	private Report report;
	private boolean dirty;

	public static void open(Report report) {
		Editors.open(new ReportEditorInput(report), ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			ReportEditorInput editorInput = (ReportEditorInput)input;
			this.report = editorInput.getReport();
		} catch (Exception e) {
			String message = "failed to init report editor";
			log.error(message, e);
			throw new PartInitException(message, e);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ReportInfoPage(this, report));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void setDirty(boolean b) {
		if (dirty != b) {
			dirty = b;
			editorDirtyStateChanged();
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
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

}

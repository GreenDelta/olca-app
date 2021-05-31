package org.openlca.app.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.util.ErrorReporter;

public abstract class SimpleFormEditor extends FormEditor {

	protected abstract FormPage getPage();

	@Override
	protected void addPages() {
		try {
			addPage(getPage());
		} catch (Exception e) {
			ErrorReporter.on("Failed to add editor page", e);
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

}

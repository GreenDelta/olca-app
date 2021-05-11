package org.openlca.app.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleFormEditor extends FormEditor {

	private static final Logger log = LoggerFactory.getLogger(SimpleFormEditor.class);
	
	protected abstract FormPage getPage();

	@Override
	protected void addPages() {
		try {
			addPage(getPage());
		} catch (Exception e) {
			log.error("Error adding page to " + getClass().getSimpleName(), e);
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

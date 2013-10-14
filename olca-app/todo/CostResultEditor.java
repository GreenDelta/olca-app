package org.openlca.core.editors.result;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CostResultEditor extends FormEditor {

	public static final String ID = "CostResultEditor";
	private Logger log = LoggerFactory.getLogger(getClass());
	private CostResultEditorInput input;

	@Override
	protected void addPages() {
		try {
			addPage(new CostResultEditorPage(this, input));
		} catch (Exception e) {
			log.error("Failed to add cost result page", e);
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

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof CostResultEditorInput)
			this.input = (CostResultEditorInput) input;
		super.init(site, input);
	}

}

package org.openlca.app.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.SourceDao;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceEditor extends FormEditor implements IEditor {

	private Logger log = LoggerFactory.getLogger(getClass());
	public static String ID = "SourceEditor";
	private Source source;
	private SourceDao dao;
	private boolean dirty;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		log.trace("open source editor {}", input);
		setPartName(input.getName());
		try {
			dao = new SourceDao(Database.get());
			ModelEditorInput i = (ModelEditorInput) input;
			source = dao.getForId(i.getDescriptor().getId());
		} catch (Exception e) {
			log.error("failed to load source from editor input", e);
		}
	}

	public Source getSource() {
		return source;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SourceInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add editor pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Save source ...", IProgressMonitor.UNKNOWN);
			dao.update(source);
			dirty = false;
			editorDirtyStateChanged();
			monitor.done();
		} catch (Exception e) {
			log.error("failed to update source", e);
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean b) {
		if (dirty != b) {
			dirty = b;
			editorDirtyStateChanged();
		}
	}

}

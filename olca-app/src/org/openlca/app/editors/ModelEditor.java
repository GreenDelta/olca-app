package org.openlca.app.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.CategorizedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ModelEditor<T extends CategorizedEntity> extends FormEditor implements IEditor {

	private Logger log = LoggerFactory.getLogger(getClass());
	private boolean dirty;
	private T model;
	private BaseDao<T> dao;
	private Class<T> modelClass;

	ModelEditor(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		log.trace("open " + modelClass.getSimpleName() + " editor {}", input);
		setPartName(input.getName());
		try {
			dao = new BaseDao<>(modelClass, Database.get());
			ModelEditorInput i = (ModelEditorInput) input;
			model = dao.getForId(i.getDescriptor().getId());
		} catch (Exception e) {
			log.error("failed to load " + modelClass.getSimpleName()
					+ " from editor input", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Save " + modelClass.getSimpleName() + "...",
					IProgressMonitor.UNKNOWN);
			dao.update(model);
			setDirty(false);
			monitor.done();
		} catch (Exception e) {
			log.error("failed to update " + modelClass.getSimpleName());
		}
	}
	
	@Override
	public ModelEditorInput getEditorInput() {
		return (ModelEditorInput) super.getEditorInput();
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
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public T getModel() {
		return model;
	}

}

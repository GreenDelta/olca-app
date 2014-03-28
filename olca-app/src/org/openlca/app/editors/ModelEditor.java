package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.Event;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.events.EventHandler;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Labels;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

public abstract class ModelEditor<T extends CategorizedEntity> extends
		FormEditor implements IEditor {

	private Logger log = LoggerFactory.getLogger(getClass());
	private boolean dirty;
	private T model;
	private BaseDao<T> dao;
	private Class<T> modelClass;
	private EventBus eventBus = new EventBus();

	private List<EventHandler> savedHandlers = new ArrayList<>();

	public ModelEditor(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	public void onSaved(EventHandler handler) {
		savedHandlers.add(handler);
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public void postEvent(String message, Object source) {
		eventBus.post(new Event(message, source));
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
			eventBus.register(this);
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
			model = dao.update(model);
			setDirty(false);
			monitor.done();
			BaseDescriptor descriptor = getEditorInput().getDescriptor();
			EntityCache cache = Cache.getEntityCache();
			cache.refresh(descriptor.getClass(), descriptor.getId());
			cache.invalidate(modelClass, model.getId());
			Navigator.refresh(Navigator.findElement(descriptor));
			this.setPartName(Labels.getDisplayName(descriptor));
			Cache.evict(descriptor);
			for (EventHandler handler : savedHandlers)
				handler.handleEvent();
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

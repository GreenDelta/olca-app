package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.Event;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentsPage;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Categories;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.Comments;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Version;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public abstract class ModelEditor<T extends CategorizedEntity>
		extends FormEditor {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Class<T> modelClass;
	private final EventBus eventBus = new EventBus();
	private final DataBinding binding = new DataBinding(this);
	private final List<Runnable> savedHandlers = new ArrayList<>();

	private boolean dirty;
	private T model;
	private BaseDao<T> dao;
	private Comments comments;

	public ModelEditor(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	public Comments getComments() {
		return comments;
	}

	public boolean hasComment(String path) {
		return App.isCommentingEnabled() && comments != null
				&& comments.hasPath(path);
	}

	public boolean hasAnyComment(String path) {
		return App.isCommentingEnabled() && comments != null
				&& comments.hasAnyPath(path);
	}

	protected void addCommentPage() throws PartInitException {
		if (!App.isCommentingEnabled() || comments == null
				|| !comments.hasRefId(model.refId))
			return;
		addPage(new CommentsPage(this, comments, model));
	}

	/**
	 * Calls the given event handler AFTER the model in this editor was saved.
	 */
	public void onSaved(Runnable handler) {
		savedHandlers.add(handler);
	}

	/**
	 * @deprecated Do not expose the event bus anymore we even could replace the
	 *             event bus with a simple list of subscribers.
	 */
	@Deprecated
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Post an event with the given ID and sender to possible subscribers.
	 */
	public void postEvent(String eventID, Object sender) {
		eventBus.post(new Event(eventID, sender));
	}

	/**
	 * Subscribes a handler for events with the given ID to this editor. When an
	 * event occurs the original sender of that event is injected to the respective
	 * handlers.
	 */
	public void onEvent(String eventID, Consumer<Object> handler) {
		if (handler == null)
			return;
		eventBus.register(new Object() {
			@Subscribe
			public void handle(Event e) {
				if (e == null || !Objects.equal(e.id, eventID))
					return;
				handler.accept(e.sender);
			}
		});
	}

	public DataBinding getBinding() {
		return binding;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		log.trace("open " + modelClass.getSimpleName() + " editor {}", input);
		ModelEditorInput i = (ModelEditorInput) input;
		setPartName(input.getName());
		setTitleImage(Images.get(i.getDescriptor()));
		try {
			dao = Daos.base(Database.get(), modelClass);
			model = dao.getForId(i.getDescriptor().id);
			loadComments(i.getDescriptor().type,
					i.getDescriptor().refId);
			eventBus.register(this);
		} catch (Exception e) {
			log.error("failed to load " + modelClass.getSimpleName()
					+ " from editor input", e);
		}
	}

	private void loadComments(ModelType type, String refId) {
		if (!App.isCommentingEnabled())
			return;
		try {
			comments = Database.getRepositoryClient().getComments(type, refId);
		} catch (WebRequestException e) {
			log.error("Error loading comments from repository", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			if (monitor != null)
				monitor.beginTask(M.Save + " " + modelClass.getSimpleName()
						+ "...", IProgressMonitor.UNKNOWN);
			model.lastChange = Calendar.getInstance().getTimeInMillis();
			Version.incUpdate(model);
			model = dao.update(model);
			doAfterUpdate();
			if (monitor != null)
				monitor.done();
		} catch (Exception e) {
			ErrorReporter.on(
					"failed to update " + modelClass.getSimpleName(), e);
		}
	}

	public boolean isEditable() {
		return model != null && !model.isFromLibrary();
	}

	public void updateModel() {
		if (model == null)
			return;
		if (model.id == 0)
			return;
		model = dao.getForId(model.id);
	}

	protected void doAfterUpdate() {
		setDirty(false);
		var descriptor = getEditorInput().getDescriptor();
		EntityCache cache = Cache.getEntityCache();
		cache.refresh(descriptor.getClass(), descriptor.id);
		cache.invalidate(modelClass, model.id);
		this.setPartName(Labels.name(model));
		Cache.evict(descriptor);
		for (Runnable handler : savedHandlers) {
			if (handler != null) {
				handler.run();
			}
		}
		Navigator.refresh(Navigator.findElement(descriptor));
	}

	@Override
	public ModelEditorInput getEditorInput() {
		return (ModelEditorInput) super.getEditorInput();
	}

	/**
	 * A shortcut for {@code setDirty(true)}.
	 */
	public void setDirty() {
		setDirty(true);
	}

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
	@SuppressWarnings("unchecked")
	public void doSaveAs() {
		var diag = new InputDialog(UI.shell(), M.SaveAs, M.SaveAs,
				model.name + " - Copy", (name) -> {
					if (Strings.nullOrEmpty(name))
						return M.NameCannotBeEmpty;
					if (Strings.nullOrEqual(name, model.name))
						return M.NameShouldBeDifferent;
					return null;
				});
		if (diag.open() != Window.OK)
			return;
		String newName = diag.getValue();
		try {
			T clone = (T) model.copy();
			if (clone.isFromLibrary()) {
				clone.library = null;
				clone.category = Categories.removeLibraryFrom(clone.category);
			}
			clone.name = newName;
			clone = dao.insert(clone);
			App.open(clone);
			Navigator.refresh();
		} catch (Exception e) {
			ErrorReporter.on("failed to save " + model + " as " + newName, e);
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	public T getModel() {
		return model;
	}

}

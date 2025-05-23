package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentsPage;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Categories;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Version;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

public abstract class ModelEditor<T extends RootEntity> extends FormEditor {

	/**
	 * An event that is emitted by the model editor by default after the model
	 * of this editor was saved.
	 */
	public static final String ON_SAVED = "event.on.saved";

	private final Class<T> modelClass;
	private final List<EventHandler> eventHandlers = new ArrayList<>();
	private final DataBinding binding = new DataBinding(this);

	private boolean dirty;
	private T model;
	private BaseDao<T> dao;
	private Comments comments;

	public ModelEditor(Class<T> modelClass) {
		this.modelClass = modelClass;
	}

	public Comments getComments() {
		if (comments == null) {
			comments = loadComments();
		}
		return comments;
	}

	public boolean hasComment(String path) {
		return getComments().hasPath(path);
	}

	public boolean hasAnyComment(String path) {
		return getComments().hasAnyPath(path);
	}

	protected void addExtensionPages() throws PartInitException {
		if (getComments().hasRefId(model.refId)) {
			addPage(new CommentsPage(this, getComments(), model));
		}
		if (FeatureFlag.ADDITIONAL_PROPERTIES.isEnabled()) {
			addPage(new AdditionalPropertiesPage<>(this));
		}
	}

	public void emitEvent(String eventId) {
		var matched = false;
		for (var handler : eventHandlers) {
			if (Objects.equals(eventId, handler.eventId)) {
				matched = true;
				handler.action.run();
			}
		}
		if (matched || ON_SAVED.equals(eventId))
			return;
		var log = LoggerFactory.getLogger(getClass());
		log.warn("unmatched event ID: {}", eventId);
	}

	public void onEvent(String eventId, Runnable action) {
		if (eventId == null || action == null)
			return;
		eventHandlers.add(new EventHandler(eventId, action));
	}

	/**
	 * A short form for `onEvent(ON_SAVED, ...)`.
	 */
	public void onSaved(Runnable action) {
		if (action == null)
			return;
		eventHandlers.add(new EventHandler(ON_SAVED, action));
	}

	public DataBinding getBinding() {
		return binding;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		ModelEditorInput i = (ModelEditorInput) input;
		setPartName(input.getName());
		setTitleImage(Images.get(i.getDescriptor()));
		try {
			dao = Daos.base(Database.get(), modelClass);
			model = dao.getForId(i.getDescriptor().id);
		} catch (Exception e) {
			ErrorReporter.on("failed to load " + modelClass.getSimpleName()
					+ " from editor input", e);
		}
	}

	private Comments loadComments() {
		if (!App.isCommentingEnabled()
				|| !Repository.isConnected()
				|| !Repository.CURRENT.isCollaborationServer())
			return new Comments(new ArrayList<>());
		return new Comments(WebRequests.execute(M.Comments,
				() -> Repository.CURRENT.client.getComments(Repository.CURRENT.id,
						ModelType.of(model.getClass()).name(), model.refId),
				new ArrayList<>()));
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			if (monitor != null) {
				monitor.beginTask(M.SaveDots + " " + modelClass.getSimpleName(),
						IProgressMonitor.UNKNOWN);
			}
			model.lastChange = Calendar.getInstance().getTimeInMillis();
			Version.incUpdate(model);
			model = dao.update(model);
			doAfterUpdate();
			if (monitor != null) {
				monitor.done();
			}
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
		this.setPartName(Labels.name(model));
		AppContext.evict(descriptor);
		emitEvent(ON_SAVED);
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
		var diag = new InputDialog(UI.shell(), M.SaveAsDots, M.SaveAsDots,
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

	private record EventHandler(String eventId, Runnable action) {
	}
}

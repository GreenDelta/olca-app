package org.openlca.app.navigation.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.Category;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DeleteModelAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final List<ModelElement> models = new ArrayList<>();
	private final List<CategoryElement> categories = new ArrayList<>();
	private boolean showInUseMessage = true;

	@Override
	public boolean accept(INavigationElement<?> element) {
		models.clear();
		categories.clear();
		if (element instanceof CategoryElement) {
			categories.add((CategoryElement) element);
			return true;
		}
		if (element instanceof ModelElement) {
			models.add((ModelElement) element);
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		models.clear();
		categories.clear();
		if (elements == null)
			return false;
		for (INavigationElement<?> element : elements) {
			if (element instanceof CategoryElement) {
				categories.add((CategoryElement) element);
			} else if (element instanceof ModelElement) {
				models.add((ModelElement) element);
			}
		}
		if (models.isEmpty() && categories.isEmpty())
			return false;
		return true;
	}

	@Override
	public String getText() {
		return M.Delete;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public void run() {
		boolean canceled = false;
		boolean dontAsk = false;
		showInUseMessage = true;
		for (ModelElement element : models) {
			CategorizedDescriptor descriptor = element.getContent();
			if (descriptor == null)
				continue;
			String name = Labels.getDisplayName(descriptor);
			int answer = dontAsk ? IDialogConstants.YES_ID : askDelete(name);
			if (answer == IDialogConstants.CANCEL_ID) {
				canceled = true;
				break;
			}
			if (answer == IDialogConstants.NO_TO_ALL_ID)
				break;
			if (answer == IDialogConstants.NO_ID)
				continue;
			if (answer == IDialogConstants.YES_TO_ALL_ID)
				dontAsk = true;
			if (isUsed(descriptor))
				continue;
			App.closeEditor(descriptor);
			delete(descriptor);
			Navigator.refresh(element.getParent());
		}
		models.clear();
		if (canceled) {
			categories.clear();
			return;
		}
		boolean dontAskEmpty = false;
		for (CategoryElement element : categories) {
			Category category = element.getContent();
			if (category == null)
				continue;
			boolean empty = element.getChildren().isEmpty();
			int answer = IDialogConstants.CANCEL_ID;
			if (!empty) {
				answer = dontAskEmpty ? IDialogConstants.YES_ID : askNotEmptyDelete(category.name);
			} else {
				answer = dontAsk ? IDialogConstants.YES_ID : askDelete(category.name);
			}
			if (answer == IDialogConstants.NO_TO_ALL_ID)
				break;
			if (answer == IDialogConstants.CANCEL_ID)
				break;
			if (answer == IDialogConstants.NO_ID)
				continue;
			if (answer == IDialogConstants.YES_TO_ALL_ID) {
				if (empty) {
					dontAskEmpty = true;
				} else {
					dontAsk = true;
				}
			}
			if (delete(element)) {
				INavigationElement<?> typeElement = Navigator.findElement(category.modelType);
				Navigator.refresh(typeElement);
			}
		}
		categories.clear();
	}

	private boolean isUsed(CategorizedDescriptor d) {
		IUseSearch<CategorizedDescriptor> search = IUseSearch.FACTORY
				.createFor(d.type, Database.get());
		List<CategorizedDescriptor> descriptors = search.findUses(d);
		if (descriptors.isEmpty())
			return false;
		if (showInUseMessage) {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openError(UI.shell(), M.CannotDelete,
					d.name + ": " + M.CannotDeleteMessage,
					M.DoNotShowThisMessageAgain, false, null, null);
			showInUseMessage = !dialog.getToggleState();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private <T extends RootEntity> void delete(BaseDescriptor descriptor) {
		try {
			log.trace("delete model {}", descriptor);
			IDatabase database = Database.get();
			Class<T> clazz = (Class<T>) descriptor.type.getModelClass();
			BaseDao<T> dao = Daos.base(database, clazz);
			T instance = dao.getForId(descriptor.id);
			dao.delete(instance);
			Cache.evict(descriptor);
			DatabaseDir.deleteDir(descriptor);
			log.trace("element deleted");
		} catch (Exception e) {
			log.error("failed to delete element " + descriptor, e);
		}
	}

	private boolean delete(CategoryElement element) {
		boolean canBeDeleted = true;
		for (INavigationElement<?> child : element.getChildren()) {
			if (child instanceof CategoryElement) {
				boolean deleted = delete((CategoryElement) child);
				if (!deleted) {
					canBeDeleted = false;
				}
			} else if (child instanceof ModelElement) {
				CategorizedDescriptor descriptor = ((ModelElement) child).getContent();
				if (isUsed(descriptor)) {
					canBeDeleted = false;
					continue;
				}
				App.closeEditor(descriptor);
				delete(descriptor);
			}
		}
		if (!canBeDeleted) {
			Navigator.refresh(element);
			return false;
		}
		Category category = element.getContent();
		try {
			CategoryDao dao = new CategoryDao(Database.get());
			Category parent = category.category;
			if (parent != null) {
				parent.childCategories.remove(category);
				category.category = null;
				dao.update(parent);
			}
			dao.delete(category);
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to delete category " + category, e);
			return false;
		}
	}

	private int askDelete(String name) {
		String message = NLS.bind(M.DoYouReallyWantToDelete, name);
		return Question.askWithAll(M.Delete, message);
	}

	private int askNotEmptyDelete(String name) {
		String message = NLS.bind(M.DoYouReallyWantToDelete, name);
		return Question.askWithAll(M.Delete, M.CategoryNotEmpty + " " + message);
	}

}

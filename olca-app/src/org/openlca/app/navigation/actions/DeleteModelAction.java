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
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.usage.UsageSearch;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteModelAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final List<ModelElement> models = new ArrayList<>();
	private final List<CategoryElement> categories = new ArrayList<>();
	private final List<INavigationElement<?>> toRefresh = new ArrayList<>();
	private boolean showInUseMessage = true;

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		models.clear();
		categories.clear();
		if (elements == null)
			return false;
		for (var elem : elements) {
			if (elem.getLibrary().isPresent())
				return false;
			if (elem instanceof CategoryElement cat) {
				if (cat.hasLibraryContent())
					return false;
				categories.add(cat);
			} else if (elem instanceof ModelElement mod) {
				if (mod.isFromLibrary())
					return false;
				models.add(mod);
			}
		}
		return !models.isEmpty() || !categories.isEmpty();
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
		showInUseMessage = true;
		try {
			int continuationFlag = deleteModels();
			deleteCategories(continuationFlag);
			models.clear();
			categories.clear();
		} finally {
			Navigator.refresh(toRefresh);
		}
	}

	private int deleteModels() {
		boolean dontAsk = false;
		for (var elem : models) {
			var model = elem.getContent();
			if (model == null)
				continue;
			int a = dontAsk
					? IDialogConstants.YES_ID
					: askDelete(Labels.name(model));

			if (a == IDialogConstants.CANCEL_ID
					|| a == IDialogConstants.NO_TO_ALL_ID) {
				return IDialogConstants.CANCEL_ID;
			}
			if (a == IDialogConstants.NO_ID || isUsed(model))
				continue;
			if (a == IDialogConstants.YES_TO_ALL_ID) {
				dontAsk = true;
			}

			// delete the model
			App.close(model);
			delete(model);
			toRefresh.add(elem.getParent());
		}

		return dontAsk
				? IDialogConstants.YES_TO_ALL_ID
				: IDialogConstants.YES_ID;
	}

	private void deleteCategories(int continuationFlag) {
		if (continuationFlag != IDialogConstants.YES_ID
				&& continuationFlag != IDialogConstants.YES_TO_ALL_ID) {
			return;
		}

		boolean dontAsk = continuationFlag == IDialogConstants.YES_TO_ALL_ID;
		boolean askWhenNotEmpty = true;
		for (var elem : categories) {
			var category = elem.getContent();
			if (category == null)
				continue;

			int a;
			if (elem.getChildren().isEmpty()) {
				a = dontAsk
						? IDialogConstants.YES_ID
						: askDelete(category.name);
			} else {
				a = askWhenNotEmpty
						? askNotEmptyDelete(category.name)
						: IDialogConstants.YES_ID;
				if (a == IDialogConstants.YES_TO_ALL_ID) {
					askWhenNotEmpty = false;
					dontAsk = true;
				}
			}

			if (a == IDialogConstants.NO_TO_ALL_ID
					|| a == IDialogConstants.CANCEL_ID)
				return;
			if (a == IDialogConstants.NO_ID)
				continue;

			if (delete(elem)) {
				var typeElement = Navigator.findElement(category.modelType);
				toRefresh.add(typeElement);
			}
		}
	}

	private boolean isUsed(RootDescriptor d) {
		var search = UsageSearch.of(d.type, Database.get());
		var descriptors = search.find(d.id);
		if (descriptors.isEmpty())
			return false;
		if (showInUseMessage) {
			var dialog = MessageDialogWithToggle.openError(
					UI.shell(),
					M.CannotDelete,
					d.name + ": " + M.CannotDeleteMessage,
					M.DoNotShowThisMessageAgain,
					false,
					null,
					null);
			showInUseMessage = !dialog.getToggleState();
		}
		return true;
	}

	private void delete(Descriptor d) {
		try {
			log.trace("delete model {}", d);
			var db = Database.get();
			var instance = db.get(d.type.getModelClass(), d.id);
			db.delete(instance);
			Cache.evict(d);
			DatabaseDir.deleteDir(d);
		} catch (Exception e) {
			ErrorReporter.on("failed to delete " + d, e);
		}
	}

	private boolean delete(CategoryElement element) {

		// delete the category content
		boolean canBeDeleted = true;
		for (INavigationElement<?> child : element.getChildren()) {
			if (child instanceof CategoryElement cat) {
				boolean deleted = delete(cat);
				if (!deleted) {
					canBeDeleted = false;
				}
			} else if (child instanceof ModelElement mod) {
				var model = mod.getContent();
				if (isUsed(model)) {
					canBeDeleted = false;
					continue;
				}
				App.close(model);
				delete(model);
			}
		}

		// delete the category
		if (!canBeDeleted) {
			toRefresh.add(element);
			return false;
		}
		var category = element.getContent();
		try {
			var dao = new CategoryDao(Database.get());
			var parent = category.category;
			if (parent != null) {
				parent.childCategories.remove(category);
				category.category = null;
				dao.update(parent);
			}
			dao.delete(category);
			Cache.evict(Descriptor.of(category));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("failed to delete category " + category, e);
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

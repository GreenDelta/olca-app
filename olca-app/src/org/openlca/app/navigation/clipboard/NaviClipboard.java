package org.openlca.app.navigation.clipboard;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.Libraries;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.Category;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.RootDescriptor;

/// The clipboard for the navigation viewer. Note that this class is not
/// thread-safe. It is designed to be used as a singleton instance in the UI
/// thread only.
public class NaviClipboard {

	private final static NaviClipboard instance = new NaviClipboard();

	private List<INavigationElement<?>> content;
	private Action currentAction;

	private NaviClipboard() {
	}

	public static NaviClipboard get() {
		return instance;
	}

	// region: predicates

	public static boolean canCopy(List<INavigationElement<?>> elems) {
		return Checks.canCopy(elems);
	}

	public static boolean canCopyTo(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		return Checks.canCopyTo(elems, target);
	}

	public static boolean canCut(List<INavigationElement<?>> elems) {
		return Checks.canCut(elems);
	}

	public static boolean canMoveTo(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		return Checks.canMoveTo(elems, target);
	}

	// endregion

	public void copy(List<INavigationElement<?>> elements) {
		if (!canCopy(elements))
			return;
		initialize(Action.COPY, elements);
	}

	public void cut(List<INavigationElement<?>> elements) {
		if (!canCut(elements))
			return;
		initialize(Action.CUT, elements);
		var navigator = Navigator.getInstance();
		for (var elem : content) {
			elem.getParent().getChildren().remove(elem);
			if (navigator != null) {
				navigator.getCommonViewer().refresh(elem.getParent());
			}
		}
	}

	private void initialize(Action action,List<INavigationElement<?>> elements) {
		// TODO check if extending the content makes sense here because
		// this is also not how typical file browsers work
		if (action == Action.CUT && currentAction == Action.CUT) {
			extendCache(elements);
			return;
		}
		if (currentAction == Action.CUT) {
			restore();
		}
		content = elements;
		currentAction = action;
	}

	private void extendCache(List<INavigationElement<?>> elements) {
		if (elements == null || elements.isEmpty())
			return;
		if (isEmpty()) {
			content = elements;
			return;
		}
		content.addAll(elements);
	}

	private void restore() {
		if (isEmpty())
			return;
		for (var element : content) {
			paste(element, element.getParent());
			var type = Checks.typeOf(element);

			var modelRoot = Navigator.findElement(type);
			Navigator.refresh(modelRoot);
		}
	}

	public void pasteTo(INavigationElement<?> categoryElement) {
		if (isEmpty())
			return;
		if (!canPasteTo(categoryElement))
			return;
		try {
			for (INavigationElement<?> element : content) {
				paste(element, categoryElement);
			}
		} finally {
			clear();
			var root = Navigator.findElement(Database.getActiveConfiguration());
			Navigator.refresh(root);
		}
	}

	public void clear() {
		content = null;
		currentAction = null;
	}

	public static boolean canMove(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		if (!isSupported(elems))
			return false;
		if (!(target instanceof CategoryElement || target instanceof ModelTypeElement))
			return false;
		return getModelType(target) == getModelType(elems.get(0));
	}

	public boolean canPasteTo(INavigationElement<?> elem) {
		if (isEmpty())
			return false;
		if (!(elem instanceof CategoryElement || elem instanceof ModelTypeElement))
			return false;
		return getModelType(elem) == getModelType(content.get(0));
	}

	private void paste(INavigationElement<?> element, INavigationElement<?> category) {
		if (currentAction == Action.CUT) {
			if (element instanceof CategoryElement ce)
				move(ce, category);
			else if (element instanceof ModelElement me)
				move(me, category);
		} else if (currentAction == Action.COPY) {
			if (element instanceof CategoryElement ce) {
				copy(ce, category);
			} else if (element instanceof ModelElement me) {
				copyTo(me, getCategory(category));
			}
		}
	}

	private Category getCategory(INavigationElement<?> element) {
		return element instanceof CategoryElement catElem
				? catElem.getContent()
				: null;
	}

	private void move(CategoryElement element, INavigationElement<?> categoryElement) {
		Category newParent = getCategory(categoryElement);
		Category oldParent = getCategory(element.getParent());
		Category category = element.getContent();
		if (Objects.equals(category, newParent))
			return;
		if (Checks.isChildOf(newParent, category))
			return; // do not create category cycles
		if (oldParent != null)
			oldParent.childCategories.remove(category);
		if (newParent != null)
			newParent.childCategories.add(category);
		category.category = newParent;
		CategoryDao dao = new CategoryDao(Database.get());
		if (oldParent != null) {
			dao.update(oldParent);
		}
		if (newParent != null) {
			dao.update(newParent);
		}
		dao.update(category);
	}

	private void move(ModelElement element, INavigationElement<?> categoryElement) {
		RootDescriptor entity = element.getContent();
		Category category = getCategory(categoryElement);
		Optional<Category> parent = Optional.ofNullable(category);
		Daos.root(Database.get(), entity.type).updateCategory(entity, parent);
	}

	/**
	 * Copy the category `copyRoot` with its content under the `copyTarget`.
	 */
	private void copy(
			CategoryElement copyRoot, INavigationElement<?> copyTarget
	) {
		CategoryCopy.create(copyRoot, copyTarget, NaviClipboard::copyTo);
	}

	private static void copyTo(ModelElement e, Category category) {
		var d = e.getContent();
		if (d == null)
			return;
		var dao = Daos.root(Database.get(), d.type);
		if (dao == null)
			return;
		var entity = dao.getForId(d.id);
		if (entity == null)
			return;
		if (entity.isFromLibrary()) {
			if (entity instanceof Process p) {
				Libraries.fillExchangesOf(p);
			} else if (entity instanceof ImpactCategory i) {
				Libraries.fillFactorsOf(i);
			}
		}
		var copy = (RootEntity) entity.copy();
		copy.library = null;
		copy.category = category;
		copy.name = copy.name + " (copy)";
		DatabaseDir.copyDir(entity, copy);
		Database.get().insert(copy);
	}

	public boolean isEmpty() {
		return content == null || content.isEmpty();
	}
}

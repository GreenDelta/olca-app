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
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.RootDescriptor;

public class NaviClipboard {

	private enum Action {
		NONE, COPY, CUT
	}

	private static List<INavigationElement<?>> content = null;
	private static Action currentAction = Action.NONE;

	public static void copy(List<INavigationElement<?>> elements) {
		if (!isSupported(elements))
			return;
		initialize(Action.COPY, elements);
	}

	public static void cut(List<INavigationElement<?>> elements) {
		if (!isSupported(elements))
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

	public static boolean isSupported(List<INavigationElement<?>> elems) {
		if (elems == null || elems.isEmpty())
			return false;
		ModelType modelType = null;
		for (INavigationElement<?> element : elems) {
			if (!isSupported(element))
				return false;
			ModelType currentModelType = getModelType(element);
			if (modelType == null)
				modelType = currentModelType;
			else if (currentModelType != modelType)
				return false;
		}
		return true;
	}

	private static boolean isSupported(INavigationElement<?> elem) {
		if (!(elem instanceof ModelElement) && !(elem instanceof CategoryElement))
			return false;
		return elem.getLibrary().isEmpty();
	}

	private static ModelType getModelType(INavigationElement<?> e) {
		if (e instanceof ModelElement me)
			return me.getContent().type;
		if (e instanceof CategoryElement ce)
			return ce.getContent().modelType;
		if (e instanceof ModelTypeElement mte)
			return mte.getContent();
		return null;
	}

	private static void initialize(Action action,List<INavigationElement<?>> elements) {
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

	private static void extendCache(List<INavigationElement<?>> elements) {
		if (elements == null || elements.isEmpty())
			return;
		if (cacheIsEmpty()) {
			content = elements;
			return;
		}
		content.addAll(elements);
	}

	private static void restore() {
		if (cacheIsEmpty())
			return;
		for (INavigationElement<?> element : content) {
			paste(element, element.getParent());
			var modelRoot = Navigator.findElement(getModelType(element));
			Navigator.refresh(modelRoot);
		}
	}

	public static void pasteTo(INavigationElement<?> categoryElement) {
		if (cacheIsEmpty())
			return;
		if (!canPasteTo(categoryElement))
			return;
		try {
			for (INavigationElement<?> element : content) {
				paste(element, categoryElement);
			}
		} finally {
			clearCache();
			var root = Navigator.findElement(Database.getActiveConfiguration());
			Navigator.refresh(root);
		}
	}

	public static void clearCache() {
		content = null;
		currentAction = Action.NONE;
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

	public static boolean canPasteTo(INavigationElement<?> elem) {
		if (cacheIsEmpty())
			return false;
		if (!(elem instanceof CategoryElement || elem instanceof ModelTypeElement))
			return false;
		return getModelType(elem) == getModelType(content.get(0));
	}

	private static void paste(INavigationElement<?> element, INavigationElement<?> category) {
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

	private static Category getCategory(INavigationElement<?> element) {
		return element instanceof CategoryElement catElem
				? catElem.getContent()
				: null;
	}

	private static void move(CategoryElement element, INavigationElement<?> categoryElement) {
		Category newParent = getCategory(categoryElement);
		Category oldParent = getCategory(element.getParent());
		Category category = element.getContent();
		if (Objects.equals(category, newParent))
			return;
		if (isChild(newParent, category))
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

	private static boolean isChild(Category category, Category parent) {
		if (category == null || parent == null)
			return false;
		Category p = category.category;
		while (p != null) {
			if (Objects.equals(p, parent))
				return true;
			p = p.category;
		}
		return false;
	}

	private static void move(ModelElement element, INavigationElement<?> categoryElement) {
		RootDescriptor entity = element.getContent();
		Category category = getCategory(categoryElement);
		Optional<Category> parent = Optional.ofNullable(category);
		Daos.root(Database.get(), entity.type).updateCategory(entity, parent);
	}

	/**
	 * Copy the category `copyRoot` with its content under the `copyTarget`.
	 */
	private static void copy(
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

	public static boolean cacheIsEmpty() {
		return content == null || content.isEmpty();
	}
}

package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.openlca.app.db.Database;
import org.openlca.app.db.Resources;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class CopyPaste {

	private enum Action {
		NONE, COPY, CUT;
	}

	private static INavigationElement<?>[] cache = null;
	private static Action currentAction = Action.NONE;

	public static void copy(Collection<INavigationElement<?>> elements) {
		copy(elements.toArray(new INavigationElement<?>[elements.size()]));
	}

	public static void copy(INavigationElement<?>[] elements) {
		if (!isSupported(elements))
			return;
		initialize(Action.COPY, elements);
	}

	public static void cut(Collection<INavigationElement<?>> elements) {
		cut(elements.toArray(new INavigationElement<?>[elements.size()]));
	}

	private static void cut(INavigationElement<?>[] elements) {
		if (!isSupported(elements))
			return;
		initialize(Action.CUT, elements);
		for (INavigationElement<?> element : cache) {
			element.getParent().getChildren().remove(element);
			Navigator.getInstance().getCommonViewer()
					.refresh(element.getParent());
		}
	}

	public static boolean isSupported(INavigationElement<?> element) {
		return element instanceof ModelElement
				|| element instanceof CategoryElement;
	}

	public static boolean isSupported(Collection<INavigationElement<?>> elements) {
		if (elements == null)
			return false;
		return isSupported(elements.toArray(new INavigationElement[elements
				.size()]));
	}

	public static boolean isSupported(INavigationElement<?>[] elements) {
		if (elements == null || elements.length == 0)
			return false;
		ModelType modelType = null;
		for (INavigationElement<?> element : elements) {
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

	private static ModelType getModelType(INavigationElement<?> element) {
		if (element instanceof ModelElement)
			return ((ModelElement) element).getContent().getModelType();
		if (element instanceof CategoryElement)
			return ((CategoryElement) element).getContent().getModelType();
		if (element instanceof ModelTypeElement)
			return ((ModelTypeElement) element).getContent();
		return ModelType.UNKNOWN;
	}

	private static void initialize(Action action,
			INavigationElement<?>[] elements) {
		if (action == Action.CUT && currentAction == Action.CUT) {
			extendCache(elements);
			return;
		}
		if (currentAction == Action.CUT)
			restore();
		cache = elements;
		currentAction = action;
	}

	private static void extendCache(INavigationElement<?>[] elements) {
		if (elements == null || elements.length == 0)
			return;
		if (cacheIsEmpty()) {
			cache = elements;
			return;
		}
		INavigationElement<?>[] newCache = new INavigationElement<?>[cache.length
				+ elements.length];
		System.arraycopy(cache, 0, newCache, 0, cache.length);
		System.arraycopy(elements, 0, newCache, cache.length, elements.length);
		cache = newCache;
	}

	private static void restore() {
		if (cacheIsEmpty())
			return;
		for (INavigationElement<?> element : cache) {
			paste(element, element.getParent());
			INavigationElement<?> root = Navigator
					.findElement(getModelType(element));
			Navigator.refresh(root);
		}
	}

	public static void pasteTo(INavigationElement<?> categoryElement) {
		if (cacheIsEmpty())
			return;
		if (!canPasteTo(categoryElement))
			return;
		for (INavigationElement<?> element : cache) {
			paste(element, categoryElement);
			INavigationElement<?> root = Navigator
					.findElement(getModelType(element));
			Navigator.refresh(root);
		}
		if (currentAction == Action.CUT) {
			cache = null;
			currentAction = Action.NONE;
		}
	}

	public static boolean canMove(Collection<INavigationElement<?>> elements,
			INavigationElement<?> target) {
		return canMove(
				elements.toArray(new INavigationElement[elements.size()]),
				target);
	}

	private static boolean canMove(INavigationElement<?>[] elements,
			INavigationElement<?> target) {
		if (!isSupported(elements))
			return false;

		if (!(target instanceof CategoryElement || target instanceof ModelTypeElement))
			return false;
		return getModelType(target) == getModelType(elements[0]);
	}

	public static boolean canPasteTo(INavigationElement<?> element) {
		if (cacheIsEmpty())
			return false;
		if (!(element instanceof CategoryElement || element instanceof ModelTypeElement))
			return false;
		return getModelType(element) == getModelType(cache[0]);
	}

	private static void paste(INavigationElement<?> element,
			INavigationElement<?> category) {
		if (currentAction == Action.CUT) {
			if (element instanceof CategoryElement)
				move((CategoryElement) element, category);
			else if (element instanceof ModelElement)
				move((ModelElement) element, category);
		} else if (currentAction == Action.COPY) {
			if (element instanceof CategoryElement)
				copy((CategoryElement) element, category);
			else if (element instanceof ModelElement)
				copy((ModelElement) element, category);
		}
	}

	private static void copy(ModelElement element,
			INavigationElement<?> category) {
		CategorizedEntity copy = copy(element);
		if (copy == null)
			return;
		copy.setCategory(getCategory(category));
		insert(copy);
	}

	private static Category getCategory(INavigationElement<?> element) {
		return element instanceof CategoryElement ? ((CategoryElement) element)
				.getContent() : null;
	}

	private static void move(CategoryElement element,
			INavigationElement<?> categoryElement) {
		Category newParent = getCategory(categoryElement);
		Category oldParent = getCategory(element.getParent());
		Category category = element.getContent();
		if (Objects.equals(category, newParent))
			return;
		if (isChild(newParent, category))
			return; // do not create category cycles
		if (oldParent != null)
			oldParent.getChildCategories().remove(category);
		if (newParent != null)
			newParent.getChildCategories().add(category);
		category.setParentCategory(newParent);
		CategoryDao dao = new CategoryDao(Database.get());
		if (oldParent != null)
			dao.update(oldParent);
		if (newParent != null)
			dao.update(newParent);
		else
			dao.update(category);
	}

	private static boolean isChild(Category category, Category parent) {
		if (category == null || parent == null)
			return false;
		Category p = category.getParentCategory();
		while (p != null) {
			if (Objects.equals(p, parent))
				return true;
			p = p.getParentCategory();
		}
		return false;
	}

	private static void move(ModelElement element,
			INavigationElement<?> category) {
		Optional<Category> parent = Optional
				.fromNullable(getCategory(category));
		BaseDescriptor descriptor = element.getContent();
		Database.createRootDao(descriptor.getModelType()).updateCategory(
				descriptor, parent);
	}

	private static void copy(CategoryElement element,
			INavigationElement<?> category) {
		List<CategorizedEntity> entitiesToInsert = new ArrayList<>();
		Category parent = getCategory(category);
		Category rootCopy = null;
		Queue<CategoryElement> elements = new LinkedList<>();
		elements.add(element);
		Category currentParent = parent;
		while (!elements.isEmpty()) {
			CategoryElement current = elements.poll();
			Category copy = current.getContent().clone();
			if (rootCopy == null)
				rootCopy = copy;
			copy.getChildCategories().clear();
			copy.setParentCategory(parent);
			if (currentParent != null)
				currentParent.getChildCategories().add(copy);
			for (INavigationElement<?> child : current.getChildren())
				if (child instanceof CategoryElement)
					elements.add((CategoryElement) child);
				else {
					CategorizedEntity modelCopy = copy((ModelElement) child);
					modelCopy.setCategory(copy);
					entitiesToInsert.add(modelCopy);
				}
			currentParent = copy;
		}
		if (parent != null)
			new CategoryDao(Database.get()).update(parent);
		else
			new CategoryDao(Database.get()).insert(rootCopy);
		for (CategorizedEntity entity : entitiesToInsert)
			insert(entity);
	}

	private static CategorizedEntity copy(ModelElement element) {
		BaseDescriptor descriptor = element.getContent();
		CategorizedEntityDao<?, ?> dao = Database.createRootDao(descriptor
				.getModelType());
		CategorizedEntity entity = dao.getForId(descriptor.getId());
		CategorizedEntity copy = cloneIt(entity);
		if (copy != null)
			copy.setName(copy.getName() + " (copy)");
		return copy;
	}

	private static CategorizedEntity cloneIt(CategorizedEntity entity) {
		try {
			CategorizedEntity clone = (CategorizedEntity) entity.clone();
			Resources.copy(entity, clone);
			return clone;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(CopyPaste.class);
			log.error("failed to clone " + entity, e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends CategorizedEntity> void insert(T entity) {
		BaseDao<T> dao = (BaseDao<T>) Database.get().createDao(
				entity.getClass());
		dao.insert(entity);
	}

	public static boolean cacheIsEmpty() {
		return cache == null || cache.length == 0;
	}
}

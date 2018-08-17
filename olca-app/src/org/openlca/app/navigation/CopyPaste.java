package org.openlca.app.navigation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			Navigator.getInstance().getCommonViewer().refresh(element.getParent());
		}
	}

	public static boolean isSupported(INavigationElement<?> element) {
		return element instanceof ModelElement || element instanceof CategoryElement;
	}

	public static boolean isSupported(Collection<INavigationElement<?>> elements) {
		if (elements == null)
			return false;
		return isSupported(elements.toArray(new INavigationElement[elements.size()]));
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

	private static void initialize(Action action, INavigationElement<?>[] elements) {
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
		INavigationElement<?>[] newCache = new INavigationElement<?>[cache.length + elements.length];
		System.arraycopy(cache, 0, newCache, 0, cache.length);
		System.arraycopy(elements, 0, newCache, cache.length, elements.length);
		cache = newCache;
	}

	private static void restore() {
		if (cacheIsEmpty())
			return;
		for (INavigationElement<?> element : cache) {
			paste(element, element.getParent());
			INavigationElement<?> root = Navigator.findElement(getModelType(element));
			Navigator.refresh(root);
		}
	}

	public static void pasteTo(INavigationElement<?> categoryElement) {
		if (cacheIsEmpty())
			return;
		if (!canPasteTo(categoryElement))
			return;
		boolean started = false;
		try {
			Database.getIndexUpdater().beginTransaction();
			started = true;
			for (INavigationElement<?> element : cache)
				paste(element, categoryElement);
		} finally {
			if (started)
				Database.getIndexUpdater().endTransaction();
			clearCache();
		}
	}

	public static void clearCache() {
		cache = null;
		currentAction = Action.NONE;
		INavigationElement<?> root = Navigator.findElement(Database.getActiveConfiguration());
		Navigator.refresh(root);
	}

	public static boolean canMove(Collection<INavigationElement<?>> elements, INavigationElement<?> target) {
		return canMove(elements.toArray(new INavigationElement[elements.size()]), target);
	}

	private static boolean canMove(INavigationElement<?>[] elements, INavigationElement<?> target) {
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

	private static void paste(INavigationElement<?> element, INavigationElement<?> category) {
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

	private static void copy(ModelElement element, INavigationElement<?> categoryElement) {
		CategorizedEntity copy = copy(element);
		if (copy == null)
			return;
		Category category = getCategory(categoryElement);
		copy.setCategory(category);
		copy = insert(copy);
	}

	private static Category getCategory(INavigationElement<?> element) {
		return element instanceof CategoryElement ? ((CategoryElement) element).getContent() : null;
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
			oldParent.getChildCategories().remove(category);
		if (newParent != null)
			newParent.getChildCategories().add(category);
		category.setCategory(newParent);
		CategoryDao dao = new CategoryDao(Database.get());
		if (oldParent != null)
			oldParent = dao.update(oldParent);
		if (newParent != null)
			newParent = dao.update(newParent);
		category = dao.update(category);
	}

	private static boolean isChild(Category category, Category parent) {
		if (category == null || parent == null)
			return false;
		Category p = category.getCategory();
		while (p != null) {
			if (Objects.equals(p, parent))
				return true;
			p = p.getCategory();
		}
		return false;
	}

	private static void move(ModelElement element, INavigationElement<?> categoryElement) {
		CategorizedDescriptor entity = element.getContent();
		Category category = getCategory(categoryElement);
		Optional<Category> parent = Optional.ofNullable(category);
		entity = Daos.categorized(Database.get(), entity.getModelType()).updateCategory(entity, parent);
		// need to notifiy index updater manually here
		Dataset dataset = CloudUtil.toDataset(entity, category);
		Database.getIndexUpdater().update(dataset, entity.getId());
	}

	private static void copy(CategoryElement element, INavigationElement<?> category) {
		Category parent = getCategory(category);
		Queue<CategoryElement> elements = new LinkedList<>();
		elements.add(element);
		while (!elements.isEmpty()) {
			CategoryElement current = elements.poll();
			Category copy = current.getContent().clone();
			copy.getChildCategories().clear();
			copy.setCategory(parent);
			if (parent == null)
				copy = new CategoryDao(Database.get()).insert(copy);
			else {
				parent.getChildCategories().add(copy);
				copy = new CategoryDao(Database.get()).update(parent);
			}
			for (INavigationElement<?> child : current.getChildren())
				if (child instanceof CategoryElement)
					elements.add((CategoryElement) child);
				else {
					CategorizedEntity modelCopy = copy((ModelElement) child);
					modelCopy.setCategory(copy);
					modelCopy = insert(modelCopy);
				}
			parent = copy;
		}
	}

	private static CategorizedEntity copy(ModelElement element) {
		CategorizedDescriptor descriptor = element.getContent();
		CategorizedEntityDao<?, ?> dao = Daos.categorized(Database.get(), descriptor.getModelType());
		CategorizedEntity entity = dao.getForId(descriptor.getId());
		CategorizedEntity copy = cloneIt(entity);
		if (copy != null)
			copy.setName(copy.getName() + " (copy)");
		return copy;
	}

	private static CategorizedEntity cloneIt(CategorizedEntity entity) {
		try {
			CategorizedEntity clone = (CategorizedEntity) entity.clone();
			DatabaseDir.copyDir(entity, clone);
			return clone;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(CopyPaste.class);
			log.error("failed to clone " + entity, e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends CategorizedEntity> T insert(T entity) {
		Class<T> clazz = (Class<T>) entity.getClass();
		return Daos.base(Database.get(), clazz).insert(entity);
	}

	public static boolean cacheIsEmpty() {
		return cache == null || cache.length == 0;
	}
}

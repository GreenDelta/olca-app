package org.openlca.app.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.database.ActorDao;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.ProjectDao;
import org.openlca.core.database.SourceDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.Actor;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;

import com.google.common.base.Optional;

public class CopyPaste {

	private enum Action {
		NONE, COPY, CUT;
	}

	private static INavigationElement<?>[] cache = null;
	private static Action currentAction = Action.NONE;

	public static void copy(INavigationElement<?> element) {
		copy(new INavigationElement<?>[] { element });
	}

	public static void copy(Collection<INavigationElement<?>> elements) {
		copy(elements.toArray(new INavigationElement<?>[elements.size()]));
	}

	public static void copy(INavigationElement<?>[] elements) {
		if (!isSupported(elements))
			throw new IllegalArgumentException("Elements not supported");
		initialize(Action.COPY, elements);
	}

	public static void cut(INavigationElement<?> element) {
		cut(new INavigationElement<?>[] { element });
	}

	public static void cut(Collection<INavigationElement<?>> elements) {
		cut(elements.toArray(new INavigationElement<?>[elements.size()]));
	}

	public static void cut(INavigationElement<?>[] elements) {
		if (!isSupported(elements))
			throw new IllegalArgumentException("Elements not supported");
		initialize(Action.CUT, elements);
		cut();
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
		if (currentAction == Action.CUT)
			restore();
		cache = elements;
		currentAction = action;
	}

	private static void cut() {
		for (INavigationElement<?> element : cache) {
			element.getParent().getChildren().remove(element);
			Navigator.getInstance().getCommonViewer()
					.refresh(element.getParent());
		}
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
			throw new IllegalArgumentException("Can only paste to same type");
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

	public static boolean canMove(INavigationElement<?> element,
			INavigationElement<?> target) {
		return canMove(new INavigationElement<?>[] { element }, target);
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
			INavigationElement<?> category) {
		Category newParent = getCategory(category);
		Category oldParent = getCategory(element.getParent());
		Category content = element.getContent();
		if (Objects.equals(content, newParent))
			return;
		if (oldParent != null)
			oldParent.getChildCategories().remove(content);
		if (newParent != null)
			newParent.getChildCategories().add(content);
		content.setParentCategory(newParent);
		if (oldParent != null)
			new CategoryDao(Database.get()).update(oldParent);
		if (newParent != null)
			new CategoryDao(Database.get()).update(newParent);
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
		if (entity instanceof Actor)
			return ((Actor) entity).clone();
		else if (entity instanceof Source)
			return ((Source) entity).clone();
		else if (entity instanceof UnitGroup)
			return ((UnitGroup) entity).clone();
		else if (entity instanceof FlowProperty)
			return ((FlowProperty) entity).clone();
		else if (entity instanceof Flow)
			return ((Flow) entity).clone();
		else if (entity instanceof Process)
			return ((Process) entity).clone();
		else if (entity instanceof ProductSystem)
			return ((ProductSystem) entity).clone();
		else if (entity instanceof ImpactMethod)
			return ((ImpactMethod) entity).clone();
		else if (entity instanceof Project)
			return ((Project) entity).clone();
		return null;
	}

	private static void insert(CategorizedEntity entity) {
		if (entity instanceof Actor)
			new ActorDao(Database.get()).insert((Actor) entity);
		else if (entity instanceof Source)
			new SourceDao(Database.get()).insert((Source) entity);
		else if (entity instanceof UnitGroup)
			new UnitGroupDao(Database.get()).insert((UnitGroup) entity);
		else if (entity instanceof FlowProperty)
			new FlowPropertyDao(Database.get()).insert((FlowProperty) entity);
		else if (entity instanceof Flow)
			new FlowDao(Database.get()).insert((Flow) entity);
		else if (entity instanceof Process)
			new ProcessDao(Database.get()).insert((Process) entity);
		else if (entity instanceof ProductSystem)
			new ProductSystemDao(Database.get()).insert((ProductSystem) entity);
		else if (entity instanceof Project)
			new ProjectDao(Database.get()).insert((Project) entity);
	}

	public static boolean cacheIsEmpty() {
		return cache == null || cache.length == 0;
	}
}

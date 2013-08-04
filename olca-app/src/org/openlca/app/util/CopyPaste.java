package org.openlca.app.util;

import java.util.Collection;

import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
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

			ModelType currentModelType = getType(element);
			if (modelType == null)
				modelType = currentModelType;
			else if (currentModelType != modelType)
				return false;
		}
		return true;
	}

	private static ModelType getType(INavigationElement<?> element) {
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
			Navigator.refresh(element.getParent());
		}
	}

	public static void pasteTo(INavigationElement<?> category) {
		if (cacheIsEmpty())
			return;
		if (!canPasteTo(category))
			throw new IllegalArgumentException("Can only paste to same type");

		for (INavigationElement<?> element : cache) {
			paste(element, category);
			Navigator.refresh(element.getParent());
			Navigator.refresh(category);
		}
		if (currentAction == Action.CUT) {
			cache = null;
			currentAction = Action.NONE;
		}
	}

	public static boolean canPasteTo(INavigationElement<?> element) {
		if (cacheIsEmpty())
			return false;
		if (!(element instanceof CategoryElement || element instanceof ModelTypeElement))
			return false;
		return getType(element) == getType(cache[0]);
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

	private static Category getCategory(INavigationElement<?> element) {
		return element instanceof CategoryElement ? ((CategoryElement) element)
				.getContent() : null;
	}

	private static void move(CategoryElement element,
			INavigationElement<?> category) {
		Category newParent = getCategory(category);
		Category oldParent = getCategory(element.getParent());
		Category content = element.getContent();
		if (oldParent != null)
			oldParent.remove(content);
		if (newParent != null)
			newParent.add(content);
		content.setParentCategory(newParent);

		if (oldParent != null)
			new CategoryDao(Database.get()).update(oldParent);
		if (newParent != null)
			new CategoryDao(Database.get()).update(newParent);
	}

	private static void move(ModelElement element,
			INavigationElement<?> category) {
		Optional<Category> parent = Optional.fromNullable(getCategory(category));
		BaseDescriptor descriptor = element.getContent();
		Database.createRootDao(descriptor.getModelType()).updateCategory(
				descriptor, parent);
	}

	private static void copy(CategoryElement element,
			INavigationElement<?> category) {
		// TODO
	}

	private static void copy(ModelElement element,
			INavigationElement<?> category) {
		// TODO
	}

	public static boolean cacheIsEmpty() {
		return cache == null || cache.length == 0;
	}
}

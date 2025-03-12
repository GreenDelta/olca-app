package org.openlca.app.navigation.clipboard;

import java.util.List;
import java.util.Objects;

import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

class Checks {

	private Checks() {
	}

	/// Returns true if the given category is a child of the given parent category.
	/// It recursively checks the category's parent until it reaches the given
	/// parent category or the category has no parent.
	///
	/// @param category the category to check
	/// @param parent the parent category
	static boolean isChildOf(Category category, Category parent) {
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

	static ModelType typeOf(INavigationElement<?> e) {
		if (e instanceof ModelElement me)
			return me.getContent().type;
		if (e instanceof CategoryElement ce)
			return ce.getContent().modelType;
		if (e instanceof ModelTypeElement mte)
			return mte.getContent();
		return null;
	}

	static boolean isValidTarget(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		var type = elems != null && !elems.isEmpty()
				? typeOf(elems.getFirst())
				: null;
		if (type == null)
			return false;
		var targetType = typeOf(target);
		if (targetType != type || elems.contains(target))
			return false;

		if (target instanceof CategoryElement ce) {
			var targetCategory = ce.getContent();
			for (var e : elems) {
				if (e instanceof CategoryElement cat) {
					if (isChildOf(cat.getContent(), targetCategory))
						return false;
				}
			}
			return true;
		}
		return target instanceof ModelTypeElement;
	}

	private static boolean canApply(
			List<INavigationElement<?>> elems, Action action
	) {
		if (elems == null || elems.isEmpty())
			return false;
		var type = typeOf(elems.getFirst());
		if (type == null)
			return false;
		for (var elem : elems) {
			boolean canApply = switch (action) {
				case COPY -> canCopy(elem);
				case CUT -> canCut(elem);
			};
			if (!canApply)
				return false;
			if (typeOf(elem) != type)
				return false;
		}
		return true;
	}

	static boolean canCopy(List<INavigationElement<?>> elems) {
		return canApply(elems, Action.COPY);
	}

	static boolean canCopyTo(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		return canApply(elems, Action.COPY) && isValidTarget(elems, target);
	}

	static boolean canCut(List<INavigationElement<?>> elems) {
		return canApply(elems, Action.CUT);
	}

	static boolean canMoveTo(
			List<INavigationElement<?>> elems, INavigationElement<?> target
	) {
		return canApply(elems, Action.CUT) && isValidTarget(elems, target);
	}

	static boolean canCopy(INavigationElement<?> elem) {
		if (elem == null || elem.getContent() == null)
			return false;
		if (elem instanceof CategoryElement ce) {
			// copying a category is not allowed if
			// it has library content
			return !ce.hasLibraryContent();
		}
		// but copying a model element is always allowed
		return elem instanceof ModelElement;
	}

	public static boolean canCut(INavigationElement<?> e) {
		if (e == null || e.getContent() == null)
			return false;
		if (e instanceof CategoryElement ce) {
			return !ce.hasLibraryContent();
		}
		if (e instanceof ModelElement me) {
			return !me.isFromLibrary();
		}
		return false;
	}
}

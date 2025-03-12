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
	private Action action;

	private NaviClipboard() {
	}

	public static NaviClipboard get() {
		return instance;
	}

	// region: static predicates

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

	private void initialize(
			Action nextAction, List<INavigationElement<?>> elements
	) {

		// in case of a cut operation, we can extend the content
		// if the content is of the same type
		if (nextAction == Action.CUT && this.action == Action.CUT) {
			if (extendContent(elements))
				return;
		}

		// otherwise we restore the content first, if required
		if (this.action == Action.CUT) {
			restore();
		}
		content = elements;
		this.action = nextAction;
	}

	private boolean extendContent(List<INavigationElement<?>> elements) {
		if (elements == null || elements.isEmpty())
			return true; // should not happen
		if (isEmpty()) {
			content = elements;
			return true;
		}
		var currentType = Checks.typeOf(content.getFirst());
		var nextType = Checks.typeOf(elements.getFirst());
		return currentType == nextType;
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
		action = null;
	}

	public boolean canPasteTo(INavigationElement<?> target) {
		return Checks.isValidTarget(content, target);
	}

	private void paste(
			INavigationElement<?> element, INavigationElement<?> target
	) {
		if (action == Action.CUT) {
			if (element instanceof CategoryElement ce)
				move(ce, target);
			else if (element instanceof ModelElement me)
				move(me, target);
		} else if (action == Action.COPY) {
			if (element instanceof CategoryElement ce) {
				CategoryCopy.create(ce, target, this::copyTo);
			} else if (element instanceof ModelElement me) {
				copyTo(me, categoryOf(target));
			}
		}
	}

	private Category categoryOf(INavigationElement<?> element) {
		return element instanceof CategoryElement catElem
				? catElem.getContent()
				: null;
	}

	private void move(CategoryElement element, INavigationElement<?> target) {
		Category newParent = categoryOf(target);
		Category oldParent = categoryOf(element.getParent());
		Category category = element.getContent();

		if (Objects.equals(category, newParent))
			return;
		if (Checks.isChildOf(newParent, category))
			return; // do not create category cycles

		if (oldParent != null) {
			oldParent.childCategories.remove(category);
		}
		if (newParent != null) {
			newParent.childCategories.add(category);
		}
		category.category = newParent;

		var dao = new CategoryDao(Database.get());
		if (oldParent != null) {
			dao.update(oldParent);
		}
		if (newParent != null) {
			dao.update(newParent);
		}
		dao.update(category);
	}

	private void move(ModelElement element, INavigationElement<?> target) {
		RootDescriptor entity = element.getContent();
		Category category = categoryOf(target);
		Optional<Category> parent = Optional.ofNullable(category);
		Daos.root(Database.get(), entity.type).updateCategory(entity, parent);
	}

	private void copyTo(ModelElement e, Category category) {
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

package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.openlca.app.navigation.elements.INavigationElement;
import org.slf4j.LoggerFactory;

/// Checks if the navigator contains disposed elements and rebuilds it when this
/// is the case.
///
/// With hash lookup (`setUseHashlookup(true)`), JFace takes the tree item of an
/// element from an internal hash map instead of searching the tree. If the item
/// of an element that JFace looks up is disposed, a possible method called on
/// that item could fail with "Widget is disposed" or "Argument not valid".
///
/// Normally, this cannot happen, as JFace removes the map entry of an item
/// before it disposes the item. An entry is only left behind when the hash code
/// of its element changed while it was mapped, because the entry then cannot be
/// found and removed anymore (see the hash code notes in `NavigationElement`).
/// So this is a defensive check against "Widget is disposed" errors that
/// occurred in the navigation.
///
/// The check cannot cover the whole map: the map is private, and disposed items
/// are no longer part of the tree. The only way to find them is to look up
/// elements with `testFindItems`. Thus, there is a method for each kind of
/// viewer operation, which checks exactly the elements that this operation
/// looks up; it must be called directly before the operation. The parents of
/// these elements are always checked too: when JFace does not find an element
/// in the map, it walks up its parents to find an item it can expand.
///
/// When a disposed item is found, the input of the viewer is set again. This
/// clears the element map and rebuilds the tree. The expanded elements are
/// restored afterward, as far as they are still in the tree.
final class DisposalCheck {

	private DisposalCheck() {
	}

	/// Call this before a full `refresh()` of the viewer. The refresh itself
	/// does not look up the input in the element map, but JFace restores the
	/// selection after the refresh: it reads the selection from the tree items
	/// before and sets it again after the refresh, which looks up the selected
	/// elements in the map. Thus, the current selection is checked.
	static void beforeRefresh(TreeViewer viewer) {
		repair(viewer, selectionOf(viewer));
	}

	/// Call this before a `refresh(element)` of the viewer. The refresh looks
	/// up the element in the element map and, like a full refresh, restores the
	/// selection afterward (see `beforeRefresh(TreeViewer)`). Thus, the element
	/// and the current selection are checked.
	static void beforeRefresh(TreeViewer viewer, INavigationElement<?> element) {
		var elements = selectionOf(viewer);
		elements.add(element);
		repair(viewer, elements);
	}

	/// Call this before a `reveal(element)` or `setSelection` of the viewer with
	/// the given element (e.g. via `selectReveal` of the navigator). Both only
	/// look up the given element in the element map, also when they replace a
	/// selection: the old selection is removed via the tree items, without
	/// lookups. Thus, only the given element is checked.
	static void beforeReveal(TreeViewer viewer, INavigationElement<?> element) {
		repair(viewer, List.of(element));
	}

	private static List<INavigationElement<?>> selectionOf(TreeViewer viewer) {
		var elements = new ArrayList<INavigationElement<?>>();
		for (var selected : viewer.getStructuredSelection().toList()) {
			if (selected instanceof INavigationElement<?> e) {
				elements.add(e);
			}
		}
		return elements;
	}

	/// Rebuilds the tree if the element map contains a disposed item for one of
	/// the given elements or their parents.
	private static void repair(
		TreeViewer viewer, List<INavigationElement<?>> elements
	) {
		for (var e : elements) {
			if (hasDisposedItems(viewer, e)) {
				rebuild(viewer);
				return;
			}
		}
	}

	private static boolean hasDisposedItems(
		TreeViewer viewer, INavigationElement<?> element
	) {
		for (var e = element; e != null; e = e.getParent()) {
			for (var item : viewer.testFindItems(e)) {
				if (item != null && item.isDisposed())
					return true;
			}
		}
		return false;
	}

	private static void rebuild(TreeViewer viewer) {
		// not an error, as we recover from it; but we log it to see when and
		// whether this still happens
		LoggerFactory.getLogger(DisposalCheck.class)
			.warn("navigation tree contains disposed items; rebuild the tree");
		var expanded = viewer.getExpandedElements();
		viewer.setInput(viewer.getInput());
		if (expanded.length > 0) {
			viewer.setExpandedElements(expanded);
		}
	}
}

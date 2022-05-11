package org.openlca.app.viewers.trees;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

public abstract class TreeCheckStateContentProvider<T> implements ICheckStateProvider, ITreeContentProvider {

	private Set<T> selection = new HashSet<>();

	@Override
	@SuppressWarnings("unchecked")
	public final boolean isChecked(Object element) {
		return getCheckState((T) element) != CheckState.UNCHECKED;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final boolean isGrayed(Object element) {
		return getCheckState((T) element) == CheckState.GRAYED;
	}

	@SuppressWarnings("unchecked")
	private CheckState getCheckState(T element) {
		if (element == null)
			return CheckState.UNCHECKED;
		if (isLeaf(element)) {
			if (isSelected(element))
				return CheckState.CHECKED;
			return CheckState.UNCHECKED;
		}
		var children = getChildren(element);
		if (children.length == 0)
			return CheckState.UNCHECKED;
		var checkedChildren = 0;
		for (var child : children) {
			var selection = getCheckState((T) child);
			if (selection == CheckState.GRAYED)
				return CheckState.GRAYED;
			if (selection == CheckState.CHECKED) {
				checkedChildren++;
			}
		}
		if (checkedChildren == children.length)
			return CheckState.CHECKED;
		if (checkedChildren > 0)
			return CheckState.GRAYED;
		return CheckState.UNCHECKED;
	}

	protected abstract boolean isLeaf(T element);

	private boolean isSelected(T element) {
		return selection.contains(element);
	}

	public final void setSelection(Set<T> selection) {
		this.selection = selection;
	}

	@SuppressWarnings("unchecked")
	protected void setSelection(T element, boolean checked) {
		if (isLeaf(element)) {
			if (checked) {
				selection.add(element);
			} else {
				selection.remove(element);
			}
		} else {
			for (var child : getChildren(element)) {
				setSelection((T) child, checked);
			}
		}
	}

	@Override
	public final Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object[] getChildren(Object parentElement) {
		var element = (T) parentElement;
		if (isLeaf(element))
			return new Object[0];
		var children = childrenOf(element);
		return children.stream()
				.filter(this::isOrContainsLeaf)
				.toArray();
	}

	protected abstract List<T> childrenOf(T element);

	private boolean isOrContainsLeaf(T element) {
		if (isLeaf(element))
			return true;
		for (var child : childrenOf(element))
			if (isOrContainsLeaf(child))
				return true;
		return false;
	}

	@Override
	public final boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object getParent(Object element) {
		return parentOf((T) element);
	}

	protected abstract T parentOf(T element);

	public final Set<T> getSelection() {
		return selection;
	}

	protected void onCheckStateChanged() {
		// subclasses may override
	}

	private enum CheckState {

		CHECKED,

		GRAYED,

		UNCHECKED;

	}

}
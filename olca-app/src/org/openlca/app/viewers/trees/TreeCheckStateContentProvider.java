package org.openlca.app.viewers.trees;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

public abstract class TreeCheckStateContentProvider<T> implements ICheckStateProvider, ITreeContentProvider {

	private Set<T> selection = new HashSet<>();
	// viewer will first call isChecked and then isGrayed; avoiding to find
	// out the state twice for performance reasons
	private CheckState current;

	@Override
	@SuppressWarnings("unchecked")
	public boolean isChecked(Object o) {
		current = CheckState.UNCHECKED;
		if (o == null)
			return false;
		var element = (T) o;
		current = getSelection(element);
		return current != CheckState.UNCHECKED;
	}

	@SuppressWarnings("unchecked")
	private CheckState getSelection(T element) {
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
			var selection = getSelection((T) child);
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

	@Override
	public boolean isGrayed(Object element) {
		return current == CheckState.GRAYED;
	}

	protected abstract boolean isLeaf(T element);

	protected abstract List<T> childrenOf(T element);

	protected abstract T parentOf(T element);

	protected abstract void onCheckStateChanged();

	protected boolean isSelected(T element) {
		return selection.contains(element);
	}

	public void setSelection(Set<T> selection) {
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
	@SuppressWarnings("unchecked")
	public final Object getParent(Object element) {
		return parentOf((T) element);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] getChildren(Object parentElement) {
		var element = (T) parentElement;
		if (isLeaf(element))
			return new Object[0];
		var children = childrenOf(element);
		return children.stream()
				.filter(this::isOrContainsLeaf)
				.toArray();
	}

	private boolean isOrContainsLeaf(T element) {
		if (isLeaf(element))
			return true;
		for (var child : childrenOf(element))
			if (isOrContainsLeaf(child))
				return true;
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public Set<T> getSelection() {
		return selection;
	}

	private enum CheckState {

		CHECKED,

		GRAYED,

		UNCHECKED;

	}

}
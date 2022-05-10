package org.openlca.app.viewers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;

public abstract class SelectionState<T> implements ICheckStateListener {

	private final CheckboxTreeViewer viewer;
	public final List<T> selection = new ArrayList<>();

	public SelectionState(CheckboxTreeViewer viewer) {
		this.viewer = viewer;
	}

	public final void setSelection(T[] selection) {
		// check the selected elements
		viewer.setCheckedElements(selection);
		for (var e : selection) {
			if (isLeaf(e)) {
				updateSelection(e, true);
			}
			updateChildren(e, true);
			updateParent(e);
		}

		// expand the selection
		var expanded = new HashSet<T>();
		for (var element : selection) {
			expanded.add(element);
			var parent = getParent(element);
			while (parent != null) {
				expanded.add(parent);
				parent = getParent(parent);
			}
		}
		viewer.setExpandedElements(expanded.toArray());
	}

	@Override
	public final void checkStateChanged(CheckStateChangedEvent event) {
		viewer.getControl().setRedraw(false);
		@SuppressWarnings("unchecked")
		var element = (T) event.getElement();
		viewer.setGrayed(element, false);
		updateChildren(element, event.getChecked());
		updateParent(element);
		if (isLeaf(element)) {
			updateSelection(element, event.getChecked());
		}
		viewer.getControl().setRedraw(true);
		checkCompletion();
	}

	public final void updateChildren(T element, boolean state) {
		for (var child : getChildren(element)) {
			viewer.setGrayed(child, false);
			viewer.setChecked(child, state);
			if (isLeaf(child)) {
				updateSelection(child, state);
			} else {
				updateChildren(child, state);
			}
		}
	}

	public final void updateParent(T element) {
		var parent = getParent(element);
		if (parent == null)
			return;

		// proof by contradiction
		boolean oneChecked = false;
		boolean allChecked = true;
		for (var child : getChildren(parent)) {
			var isChecked = viewer.getChecked(child);
			var isGrayed = viewer.getGrayed(child);
			if (isChecked || isGrayed) {
				oneChecked = true;
			}
			if (!isChecked) {
				allChecked = false;
			}
		}

		if (allChecked) {
			viewer.setChecked(parent, true);
			viewer.setGrayed(parent, false);
		} else {
			viewer.setGrayChecked(parent, oneChecked);
		}
		updateParent(parent);
	}

	public void updateSelection(T element, boolean selected) {
		if (selected) {
			selection.add(element);
		} else {
			selection.remove(element);
		}
	}

	protected abstract boolean isLeaf(T element);

	protected abstract List<T> getChildren(T element);

	protected abstract T getParent(T element);

	protected abstract void checkCompletion();

}

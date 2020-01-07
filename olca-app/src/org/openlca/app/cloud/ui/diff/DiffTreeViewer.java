package org.openlca.app.cloud.ui.diff;

import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.viewers.AbstractViewer;

abstract class DiffTreeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	DiffNode root;
	private CompareHelper mergeHelper;
	private ActionType action;

	DiffTreeViewer(Composite parent, JsonLoader jsonLoader, ActionType action) {
		super(parent);
		this.mergeHelper = new CompareHelper(jsonLoader, action);
		this.action = action;
		setLabelProvider(action);
	}

	protected void configureViewer(TreeViewer viewer, boolean checkable) {
		viewer.setContentProvider(new ContentProvider());
		viewer.setComparator(new Comparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
	}

	void setLabelProvider(ActionType action) {
		getViewer().setLabelProvider(new LabelProvider(action));
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		mergeHelper.reset();
		if (collection.isEmpty()) {
			root = null;
			super.setInput((Collection<DiffNode>) null);
		} else {
			root = collection.iterator().next();
			super.setInput(collection);
		}
	}

	@Override
	public void setInput(DiffNode[] input) {
		root = input[0];
		super.setInput(input);
	}

	private void onDoubleClick(DoubleClickEvent event) {
		DiffNode selected = getSelected(event);
		boolean isComparison = action == ActionType.COMPARE_AHEAD || action == ActionType.COMPARE_BEHIND;
		boolean merged = mergeHelper.openDiffEditor(selected, isComparison);
		if (merged && !isComparison) {
			getViewer().refresh(selected);
			onMerge(selected);
		}
	}

	protected void onMerge(DiffNode node) {
		// subclasses may override
	}

	private DiffNode getSelected(DoubleClickEvent event) {
		if (event.getSelection().isEmpty())
			return null;
		if (!(event.getSelection() instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.size() > 1)
			return null;
		DiffNode selected = (DiffNode) selection.getFirstElement();
		if (selected.isModelTypeNode())
			return null;
		return selected;
	}

}

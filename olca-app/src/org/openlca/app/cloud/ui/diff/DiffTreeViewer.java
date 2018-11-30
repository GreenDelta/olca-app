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
	private boolean viewMode;

	DiffTreeViewer(Composite parent, JsonLoader jsonLoader) {
		this(parent, jsonLoader, false);
	}

	DiffTreeViewer(Composite parent, JsonLoader jsonLoader, boolean viewMode) {
		super(parent);
		mergeHelper = new CompareHelper(jsonLoader);
		this.viewMode = viewMode;
	}

	protected void configureViewer(TreeViewer viewer, boolean checkable) {
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setComparator(new Comparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		mergeHelper.reset();
		root = collection.iterator().next();
		super.setInput(collection);
	}

	@Override
	public void setInput(DiffNode[] input) {
		root = input[0];
		super.setInput(input);
	}

	private void onDoubleClick(DoubleClickEvent event) {
		DiffNode selected = getSelected(event);
		boolean merged = mergeHelper.openDiffEditor(selected, viewMode);
		if (merged && !viewMode) {
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

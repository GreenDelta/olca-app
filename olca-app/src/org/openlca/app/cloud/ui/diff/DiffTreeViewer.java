package org.openlca.app.cloud.ui.diff;

import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;

abstract class DiffTreeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	DiffNode root;
	private CompareHelper mergeHelper;

	DiffTreeViewer(Composite parent, JsonLoader jsonLoader, Direction direction) {
		super(parent);
		mergeHelper = new CompareHelper(jsonLoader, direction);
	}

	protected void configureViewer(TreeViewer viewer, boolean checkable) {
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setSorter(new Sorter());
		viewer.addDoubleClickListener(this::onDoubleClick);
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
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
		boolean merged = mergeHelper.openDiffEditor(selected);
		if (merged) {
			getViewer().refresh(selected);
			onMerge();
		}
	}

	protected void onMerge() {
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

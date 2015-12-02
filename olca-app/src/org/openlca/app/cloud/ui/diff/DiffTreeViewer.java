package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;

public class DiffTreeViewer extends AbstractViewer<DiffNode, TreeViewer> {

	private DiffNode root;
	private MergeHelper mergeHelper;
	private Runnable onMerge;

	public DiffTreeViewer(Composite parent, Direction direction,
			JsonLoader jsonLoader) {
		super(parent);
		mergeHelper = new MergeHelper(jsonLoader, direction);
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(new Sorter());
		viewer.addDoubleClickListener(this::onDoubleClick);
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		return viewer;
	}

	private void onDoubleClick(DoubleClickEvent event) {
		DiffNode selected = getSelected(event);
		boolean merged = mergeHelper.openDiffEditor(selected);
		if (merged) {
			getViewer().refresh(selected);
			if (onMerge != null)
				onMerge.run();
		}
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
	
	@Override
	public void setInput(Collection<DiffNode> collection) {
		root = collection.iterator().next();
		super.setInput(collection);
		revealConflicts();
	}

	@Override
	public void setInput(DiffNode[] input) {
		root = input[0];
		super.setInput(input);
		revealConflicts();
	}

	private void revealConflicts() {
		List<DiffNode> conflicts = getConflicts();
		for (DiffNode conflict : conflicts)
			getViewer().reveal(conflict);
	}

	public boolean hasConflicts() {
		return !getConflicts().isEmpty();
	}

	public List<DiffNode> getConflicts() {
		List<DiffNode> conflicts = new ArrayList<>();
		Stack<DiffNode> nodes = new Stack<>();
		nodes.addAll(root.children);
		while (!nodes.isEmpty()) {
			DiffNode node = nodes.pop();
			nodes.addAll(node.children);
			if (node.isModelTypeNode())
				continue;
			DiffResult result = (DiffResult) node.content;
			if (result.getType() != DiffResponse.CONFLICT)
				continue;
			if (result.overwriteLocalChanges())
				continue;
			if (result.overwriteRemoteChanges())
				continue;
			if (result.getMergedData() != null)
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

}

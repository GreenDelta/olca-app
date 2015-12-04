package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;

public class CommitDiffViewer extends DiffTreeViewer {

	private List<DiffNode> selected = new ArrayList<>();

	public CommitDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, Direction.LEFT_TO_RIGHT);
	}

	public void setInitialSelection(Set<String> initialSelection) {
		selected = findNodes(initialSelection, root);
		Set<String> expanded = new HashSet<>();
		Tree tree = getViewer().getTree();
		for (DiffNode node : selected) {
			if (!node.isModelNode())
				continue;
			String cId = node.getContent().getDataset().getCategoryRefId();
			if (expanded.contains(cId))
				continue;
			expanded.add(cId);
			getViewer().reveal(node);
		}
		tree.setRedraw(false);
		setChecked(initialSelection, tree.getItems());
		tree.setRedraw(true);
	}

	// can't use setChecked(Object[]) for performance reasons. Original
	// reveals path internally for all elements, which is unnecessary
	// because this is already done in a more efficient way in
	// setInitialSelection
	private void setChecked(Set<String> refIds, TreeItem[] items) {
		for (TreeItem item : items) {
			DiffNode node = (DiffNode) item.getData();
			if (node != null && !node.isModelTypeNode()) {
				String refId = node.getContent().getDataset().getRefId();
				if (refIds.contains(refId))
					item.setChecked(true);
			}
			setChecked(refIds, item.getItems());
		}
	}

	private List<DiffNode> findNodes(Set<String> refIds, DiffNode node) {
		List<DiffNode> elements = new ArrayList<>();
		for (DiffNode child : node.children) {
			if (!child.isModelTypeNode() && child.hasChanged()) {
				String refId = child.getContent().getDataset().getRefId();
				if (refIds.contains(refId))
					elements.add(child);
			}
			elements.addAll(findNodes(refIds, child));
		}
		return elements;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, true);
		viewer.addCheckStateListener((e) -> {
			DiffNode node = (DiffNode) e.getElement();
			if (!isCheckable(node)) {
				if (e.getChecked())
					viewer.setChecked(node, false);
				return;
			}
			if (e.getChecked())
				selected.add(node);
			else
				selected.remove(node);
		});
		return viewer;
	}

	@Override
	public CheckboxTreeViewer getViewer() {
		return (CheckboxTreeViewer) super.getViewer();
	}

	private boolean isCheckable(DiffNode node) {
		if (node.isModelTypeNode())
			return false;
		if (node.getContent().getType() == DiffResponse.NONE)
			return false;
		return true;
	}

	public List<DiffNode> getChecked() {
		return selected;
	}

	public boolean hasChecked() {
		return !getChecked().isEmpty();
	}

}

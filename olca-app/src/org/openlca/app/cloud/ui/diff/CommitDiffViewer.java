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
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;

public class CommitDiffViewer extends DiffTreeViewer {

	private List<DiffNode> selected = new ArrayList<>();
	// The option fixNewElements will prevent the user to uncheck "NEW"
	// elements, used in ReferencesResultDialog
	private boolean fixNewElements;

	public CommitDiffViewer(Composite parent, JsonLoader jsonLoader, boolean fixNewElements) {
		super(parent, jsonLoader);
		this.fixNewElements = fixNewElements;
	}

	public void setInitialSelection(Set<String> initialSelection) {
		selected = findNodes(initialSelection, root);
		Set<String> expanded = new HashSet<>();
		Tree tree = getViewer().getTree();
		for (DiffNode node : selected) {
			if (!node.isModelNode() && !node.isCategoryNode())
				continue;
			String cId = node.getContent().getDataset().categoryRefId;
			if (cId == null)
				cId = node.getModelType().name();
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
				String refId = node.getContent().getDataset().refId;
				// null is used as hack to select all
				if (refIds == null || refIds.contains(refId))
					item.setChecked(true);
			}
			setChecked(refIds, item.getItems());
		}
	}

	public void selectAll() {
		setInitialSelection(null);
	}

	private List<DiffNode> findNodes(Set<String> refIds, DiffNode node) {
		List<DiffNode> elements = new ArrayList<>();
		for (DiffNode child : node.children) {
			if (!child.isModelTypeNode() && child.hasChanged()) {
				String refId = child.getContent().getDataset().refId;
				// null is used as hack to select all
				if (refIds == null || refIds.contains(refId))
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
			if (e.getChecked()) {
				selected.add(node);
			} else {
				if (fixNewElements && node.getContent().local.type == DiffType.NEW) {
					viewer.setChecked(node, true);
					return;
				}
				selected.remove(node);
			}
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

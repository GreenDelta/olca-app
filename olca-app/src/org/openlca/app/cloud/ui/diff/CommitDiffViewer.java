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
import org.openlca.app.util.UI;
import org.openlca.cloud.model.data.FileReference;

public class CommitDiffViewer extends DiffTreeViewer {

	private List<DiffNode> selected = new ArrayList<>();
	// The option fixNewElements will prevent the user to uncheck "NEW"
	// elements, used in ReferencesResultDialog
	private boolean lockNewElements;

	public CommitDiffViewer(Composite parent, JsonLoader jsonLoader, boolean lockNewElements) {
		super(parent, jsonLoader, ActionType.COMMIT);
		this.lockNewElements = lockNewElements;
	}

	public void setSelection(Set<FileReference> initialSelection) {
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

	// // can't use setChecked(Object[]) for performance reasons. Original
	// method
	// // reveals path internally for all elements, which is unnecessary because
	// // this is already done in a more efficient way in setInitialSelection
	private void setChecked(Set<FileReference> ids, TreeItem[] items) {
		for (TreeItem item : items) {
			DiffNode node = (DiffNode) item.getData();
			if (node != null && !node.isModelTypeNode()) {
				FileReference ref = node.getContent().getDataset().asFileReference();
				// null is used as hack to select all
				if (ids == null || ids.contains(ref))
					item.setChecked(true);
			}
			setChecked(ids, item.getItems());
		}
	}

	public void selectAll() {
		setSelection(null);
	}

	private List<DiffNode> findNodes(Set<FileReference> refs, DiffNode node) {
		List<DiffNode> elements = new ArrayList<>();
		for (DiffNode child : node.children) {
			if (!child.isModelTypeNode() && child.getContent().local.tracked && child.hasChanged()) {
				FileReference ref = child.getContent().getDataset().asFileReference();
				// null is used as hack to select all
				if (refs == null || refs.contains(ref))
					elements.add(child);
			}
			elements.addAll(findNodes(refs, child));
		}
		return elements;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, true);
		viewer.addCheckStateListener((e) -> setChecked(viewer, (DiffNode) e.getElement(), e.getChecked(), false));
		UI.gridData(viewer.getTree(), true, true);
		return viewer;
	}

	private void setChecked(CheckboxTreeViewer viewer, DiffNode node, boolean value, boolean selectChildren) {
		DiffResult result = node.getContent();
		if (node.isModelTypeNode() || !result.local.tracked || result.noAction()) {
			viewer.setChecked(node, false);
		} else if (value) {
			selected.add(node);
		} else if (lockNewElements && result.local.type == DiffType.NEW) {
			viewer.setChecked(node, true);
		} else {
			selected.remove(node);
		}
	}

	@Override
	public CheckboxTreeViewer getViewer() {
		return (CheckboxTreeViewer) super.getViewer();
	}

	public List<DiffNode> getChecked() {
		return selected;
	}

	public boolean hasChecked() {
		return !selected.isEmpty();
	}

}

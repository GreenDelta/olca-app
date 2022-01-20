package org.openlca.app.collaboration.ui.viewers.diff;

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
import org.openlca.app.collaboration.ui.ActionType;
import org.openlca.app.collaboration.util.ModelTypeRefIdMap;
import org.openlca.app.util.UI;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;

public class CommitViewer extends DiffNodeViewer {

	private List<DiffNode> selected = new ArrayList<>();
	// The option fixNewElements will prevent the user to uncheck "NEW"
	// elements, used in ReferencesResultDialog
	private boolean lockNewElements;

	public CommitViewer(Composite parent, boolean lockNewElements) {
		super(parent, ActionType.COMMIT);
		this.lockNewElements = lockNewElements;
	}

	public void setSelection(ModelTypeRefIdMap initialSelection) {
		selected = findNodes(initialSelection, root);
		Set<String> expanded = new HashSet<>();
		Tree tree = getViewer().getTree();
		for (DiffNode node : selected) {
			if (!node.isModelNode())
				continue;
			Reference ref = node.contentAsDiffResult().ref();
			String path = ref.type.name() + "/" + ref.category;
			if (expanded.contains(path))
				continue;
			expanded.add(path);
			getViewer().reveal(node);
		}
		tree.setRedraw(false);
		setChecked(initialSelection, tree.getItems());
		tree.setRedraw(true);
	}

	// can't use setChecked(Object[]) for performance reasons. Original method
	// reveals path internally for all elements, which is unnecessary because
	// this is already done in a more efficient way in setInitialSelection
	private void setChecked(ModelTypeRefIdMap models, TreeItem[] items) {
		for (TreeItem item : items) {
			DiffNode node = (DiffNode) item.getData();
			if (node != null && node.isModelNode()) {
				Reference ref = node.contentAsDiffResult().ref();
				// null is used as hack to select all
				if (models == null || models.contains(ref))
					item.setChecked(true);
			}
			setChecked(models, item.getItems());
		}
	}

	public void selectAll() {
		setSelection(null);
	}

	private List<DiffNode> findNodes(ModelTypeRefIdMap models, DiffNode node) {
		List<DiffNode> elements = new ArrayList<>();
		for (DiffNode child : node.children) {
			if (child.isModelNode() && child.hasChanged()) {
				// TODO && child.getContent().local.tracked
				Reference ref = child.contentAsDiffResult().ref();
				// null is used as hack to select all
				if (models == null || models.contains(ref))
					elements.add(child);
			}
			elements.addAll(findNodes(models, child));
		}
		return elements;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		configureViewer(viewer, true);
		viewer.addCheckStateListener((e) -> setChecked(viewer, (DiffNode) e.getElement(), e.getChecked()));
		UI.gridData(viewer.getTree(), true, true);
		return viewer;
	}

	private void setChecked(CheckboxTreeViewer viewer, DiffNode node, boolean value) {
		DiffResult result = node.contentAsDiffResult();
		if (result == null || result.noAction()) {
			// TODO || !result.local.tracked
			viewer.setChecked(node, false);
		} else if (value) {
			selected.add(node);
		} else if (lockNewElements && result.local.type == DiffType.ADDED) {
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

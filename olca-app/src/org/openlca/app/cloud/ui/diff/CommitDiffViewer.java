package org.openlca.app.cloud.ui.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.cloud.JsonLoader;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;

public class CommitDiffViewer extends DiffTreeViewer {

	private List<DiffNode> selected = new ArrayList<>();

	public CommitDiffViewer(Composite parent, JsonLoader jsonLoader) {
		super(parent, jsonLoader, Direction.LEFT_TO_RIGHT);
	}

	public void setInitialSelection(Set<String> initialSelection) {
		List<DiffNode> elements = matchInitialSelection(initialSelection, root);
		DiffNode[] array = elements.toArray(new DiffNode[elements.size()]);
		getViewer().setCheckedElements(array);
		for (DiffNode node : array)
			getViewer().reveal(node);
	}

	private List<DiffNode> matchInitialSelection(Set<String> refIds,
			DiffNode node) {
		List<DiffNode> elements = new ArrayList<>();
		for (DiffNode child : node.children) {
			if (!child.isModelTypeNode() && child.hasChanged()) {
				String refId = child.getContent().getDataset().getRefId();
				if (refIds.contains(refId))
					elements.add(child);
			}
			elements.addAll(matchInitialSelection(refIds, child));
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

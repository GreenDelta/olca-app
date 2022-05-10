package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.SelectionState;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypeRefIdSet;

public class CommitViewer extends DiffNodeViewer {

	private SelectionState<DiffNode> selectionState;
	// The option fixNewElements will prevent the user to uncheck "NEW"
	// elements, used in ReferencesResultDialog
	private boolean lockNewElements; // TODO
	private final Runnable checkCompletion;

	public CommitViewer(Composite parent) {
		this(parent, null);
	}

	public CommitViewer(Composite parent, Runnable checkCompletion) {
		super(parent, false);
		super.setDirection(Direction.LEFT_TO_RIGHT);
		this.checkCompletion = checkCompletion;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		var viewer = new CheckboxTreeViewer(parent, SWT.VIRTUAL | SWT.MULTI
				| SWT.BORDER);
		viewer.setUseHashlookup(true);
		configureViewer(viewer, true);
		UI.gridData(viewer.getTree(), true, true);
		return viewer;
	}

	@Override
	public CheckboxTreeViewer getViewer() {
		return (CheckboxTreeViewer) super.getViewer();
	}

	public void setLockNewElements(boolean value) {
		this.lockNewElements = value;
	}

	@Override
	public final void setDirection(Direction direction) {
		throw new UnsupportedOperationException("Can't change commit direction");
	}

	public void setSelection(TypeRefIdSet initialSelection) {
		var selection = findNodes(initialSelection, root);
		setSelectionState(selection);
	}

	public void selectAll() {
		setSelectionState(Arrays.asList(root));
	}

	private void setSelectionState(List<DiffNode> selection) {
		selectionState = new SelectionState<DiffNode>(getViewer()) {
			@Override
			protected boolean isLeaf(DiffNode element) {
				return element.isModelNode();
			}

			@Override
			protected List<DiffNode> getChildren(DiffNode element) {
				return element.children;
			}

			@Override
			protected DiffNode getParent(DiffNode element) {
				return element.parent;
			}

			@Override
			public void updateSelection(DiffNode element, boolean selected) {
				var result = element.contentAsDiffResult();
				if (result == null || result.noAction()) {
					// TODO || !result.local.tracked
					getViewer().setChecked(element, false);
				} else if (!selected && lockNewElements && result.leftDiffType == DiffType.ADDED) {
					getViewer().setChecked(element, true);
				} else {
					super.updateSelection(element, selected);
				}
			}

			@Override
			protected void checkCompletion() {
				if (checkCompletion != null) {
					checkCompletion.run();
				}
			}
		};
		selectionState.setSelection(selection.toArray(DiffNode[]::new));
	}

	private List<DiffNode> findNodes(TypeRefIdSet models, DiffNode node) {
		var elements = new ArrayList<DiffNode>();
		var typeOfSelection = getTypeOfSelection(models, node);
		if (typeOfSelection == TypeOfSelection.NONE)
			return new ArrayList<>();
		if (typeOfSelection == TypeOfSelection.FULL)
			return Arrays.asList(node);
		for (var child : node.children) {
			elements.addAll(findNodes(models, child));
		}
		return elements;
	}

	private TypeOfSelection getTypeOfSelection(TypeRefIdSet selection, DiffNode node) {
		if (node.isModelNode()) {
			var d = node.contentAsDiffResult();
			if (selection.contains(d.type, d.refId) && node.hasChanged())
				return TypeOfSelection.FULL;
			return TypeOfSelection.NONE;
		}
		var full = 0;
		for (var child : node.children) {
			var typeOfSelection = getTypeOfSelection(selection, child);
			if (typeOfSelection == TypeOfSelection.PARTIAL)
				return TypeOfSelection.PARTIAL;
			if (typeOfSelection == TypeOfSelection.FULL) {
				full++;
			}
		}
		if (full == 0)
			return TypeOfSelection.NONE;
		if (full == node.children.size())
			return TypeOfSelection.FULL;
		return TypeOfSelection.PARTIAL;
	}

	private enum TypeOfSelection {

		FULL,

		PARTIAL,

		NONE;

	}

	public List<DiffNode> getChecked() {
		return selectionState.selection;
	}

	public boolean hasChecked() {
		return !selectionState.selection.isEmpty();
	}

}

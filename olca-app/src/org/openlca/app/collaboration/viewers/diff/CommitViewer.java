package org.openlca.app.collaboration.viewers.diff;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.app.viewers.trees.TreeCheckStateContentProvider;
import org.openlca.git.model.DiffType;
import org.openlca.git.util.TypeRefIdSet;

public class CommitViewer extends DiffNodeViewer {

	// The option lockNewElements will prevent the user to uncheck "NEW"
	// elements, used in ReferencesResultDialog
	private boolean lockNewElements;
	private DiffNodeCheckedContentProvider selectionProvider;
	private final Runnable onCheckStateChanged;

	public CommitViewer(Composite parent) {
		this(parent, null);
	}

	public CommitViewer(Composite parent, Runnable onCheckStateChanged) {
		super(parent, false);
		super.setDirection(Direction.LEFT_TO_RIGHT);
		this.onCheckStateChanged = onCheckStateChanged;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		selectionProvider = new DiffNodeCheckedContentProvider();
		var viewer = CheckboxTreeViewers.create(parent, selectionProvider);
		viewer.setUseHashlookup(true);
		viewer.setLabelProvider(new DiffNodeLabelProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
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

	public void setSelection(TypeRefIdSet initialSelection, DiffNode root) {
		var selection = collectChildren(initialSelection, root);
		selectionProvider.setSelection(selection);
	}

	public void selectAll() {
		setSelection(null, root);
		getViewer().refresh();
		CheckboxTreeViewers.expandGrayed(getViewer());
	}

	// if models is null, select all
	private Set<DiffNode> collectChildren(TypeRefIdSet models, DiffNode node) {
		Set<DiffNode> nodes = new HashSet<>();
		for (var child : node.children) {
			if (child.isModelNode()) {
				var d = child.contentAsDiffResult();
				if (models == null || models.contains(d.type, d.refId)) {
					nodes.add(child);
				}
			} else {
				nodes.addAll(collectChildren(models, child));
			}
		}
		return nodes;
	}

	public Set<DiffNode> getChecked() {
		return selectionProvider.getSelection();
	}

	public boolean hasChecked() {
		return !selectionProvider.getSelection().isEmpty();
	}

	private class DiffNodeCheckedContentProvider extends TreeCheckStateContentProvider<DiffNode> {

		@Override
		protected boolean isLeaf(DiffNode element) {
			return element.isModelNode();
		}

		@Override
		protected List<DiffNode> childrenOf(DiffNode element) {
			return element.children;
		}

		@Override
		protected DiffNode parentOf(DiffNode element) {
			return element.parent;
		}

		@Override
		protected void setSelection(DiffNode element, boolean checked) {
			if (!element.isModelNode()) {
				super.setSelection(element, checked);
				return;
			}
			var result = element.contentAsDiffResult();
			if (result == null || result.noAction()) {
				// TODO || !result.local.tracked
				getViewer().setChecked(element, false);
			} else if (!checked && lockNewElements && result.leftDiffType() == DiffType.ADDED) {
				getViewer().setChecked(element, true);
			} else {
				super.setSelection(element, checked);
			}
		}
		
		@Override
		protected void onCheckStateChanged() {
			if (onCheckStateChanged != null) {
				onCheckStateChanged.run();
			}
		}

	}

}

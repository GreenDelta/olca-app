package org.openlca.app.collaboration.viewers.diff;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.app.viewers.trees.TreeCheckStateContentProvider;
import org.openlca.git.model.TriDiff;
import org.openlca.git.util.ModelRefSet;

public class CommitViewer extends DiffNodeViewer {

	// The option lockedElements will prevent the user to uncheck certain
	// elements
	private ModelRefSet lockedElements;
	private DiffNodeCheckedContentProvider selectionProvider;
	private final Runnable onCheckStateChanged;

	public CommitViewer(Composite parent) {
		this(parent, null);
	}

	public CommitViewer(Composite parent, Runnable onCheckStateChanged) {
		super(parent, false);
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

	public void setLockedElements(ModelRefSet value) {
		this.lockedElements = value;
	}

	public void setSelection(ModelRefSet initialSelection, DiffNode root) {
		var selection = collectChildren(initialSelection, root);
		root.children.stream()
				.filter(DiffNode::isLibrariesNode)
				.forEach(selection::add);
		selectionProvider.setSelection(selection);
	}

	public void selectAll() {
		setSelection(null, root);
		getViewer().refresh();
		CheckboxTreeViewers.expandGrayed(getViewer());
	}

	// if models is null, select all
	private Set<DiffNode> collectChildren(ModelRefSet models, DiffNode node) {
		var nodes = new HashSet<DiffNode>();
		if (node == null)
			return nodes;
		for (var child : node.children) {
			if (child.isLibrariesNode() || child.isLibraryNode()) {
				nodes.add(child);
			} else if (child.content instanceof TriDiff d) {
				if (models == null || models.contains(d)) {
					nodes.add(child);
				}
			}
			nodes.addAll(collectChildren(models, child));
		}
		return nodes;
	}

	public Set<DiffNode> getChecked() {
		return selectionProvider.getSelection().stream()
				.filter(Predicate.not(DiffNode::isLibrariesNode))
				.collect(Collectors.toSet());
	}

	public boolean hasChecked() {
		return !selectionProvider.getSelection().isEmpty();
	}

	private class DiffNodeCheckedContentProvider extends TreeCheckStateContentProvider<DiffNode> {

		@Override
		protected boolean isLeaf(DiffNode element) {
			return (element.content instanceof TriDiff || element.isLibraryNode()) && element.children.isEmpty();
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
		protected boolean isSelectable(DiffNode element) {
			return element.content instanceof TriDiff || element.isLibrariesNode();
		}

		@Override
		protected void setSelection(DiffNode elem, boolean checked) {
			if (!isSelectable(elem)) {
				for (var child : elem.children) {
					setSelection(child, checked);
				}
				return;
			}

			var diff = elem.contentAsTriDiff();
			if (diff == null || diff.noAction()) {
				getViewer().setChecked(elem, false);
				return;
			}

			if (!checked &&
					(lockedElements.contains(diff)
							|| elem.isLibrariesNode()
							|| elem.isLibraryNode())) {
				getViewer().setChecked(elem, true);
				return;
			}

			if (!checked) {
				getSelection().remove(elem);
			} else {
				var added = getSelection().add(elem);
				if (added) {
					setSelection(elem.parent, true);
				}
			}
			for (var child : elem.children) {
				setSelection(child, checked);
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

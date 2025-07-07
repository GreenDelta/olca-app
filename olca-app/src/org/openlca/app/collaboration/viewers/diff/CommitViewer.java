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
import org.openlca.git.repo.OlcaRepository;

public class CommitViewer extends DiffNodeViewer {

	private DiffNodeCheckedContentProvider selectionProvider;
	private final Runnable onCheckStateChanged;

	public CommitViewer(Composite parent, OlcaRepository repo) {
		this(parent, repo, null);
	}

	public CommitViewer(Composite parent, OlcaRepository repo, Runnable onCheckStateChanged) {
		super(parent, repo);
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

	public void setSelection(Set<String> initialSelection, DiffNode root) {
		var selection = collectChildren(initialSelection, root);
		selectionProvider.setSelection(selection);
	}

	public void selectAll() {
		setSelection(null, root);
		getViewer().refresh();
		CheckboxTreeViewers.expandGrayed(getViewer());
	}

	// if models is null, select all
	private Set<DiffNode> collectChildren(Set<String> models, DiffNode node) {
		var nodes = new HashSet<DiffNode>();
		if (node == null)
			return nodes;
		for (var child : node.children) {
			if (child.content instanceof TriDiff d) {
				if (models == null || models.contains(d.path)) {
					nodes.add(child);
				}
			}
			nodes.addAll(collectChildren(models, child));
		}
		return nodes;
	}

	public Set<DiffNode> getChecked() {
		return selectionProvider.getSelection().stream()
				.filter(Predicate.not(DiffNode::isDataPackagesNode))
				.collect(Collectors.toSet());
	}

	public boolean hasChecked() {
		return !selectionProvider.getSelection().isEmpty();
	}

	private class DiffNodeCheckedContentProvider extends TreeCheckStateContentProvider<DiffNode> {

		@Override
		protected boolean isLeaf(DiffNode element) {
			return (element.content instanceof TriDiff || element.isDataPackageNode()) && element.children.isEmpty();
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
			return element.content instanceof TriDiff || element.isDataPackageNode();
		}

		@Override
		protected void setSelection(DiffNode elem, boolean checked) {
			setSelection(elem, checked, false);
		}

		private void setSelection(DiffNode elem, boolean checked, boolean onlyUp) {
			if (!isSelectable(elem) && !onlyUp) {
				for (var child : elem.children) {
					setSelection(child, checked);
				}
				return;
			}
			var diff = elem.contentAsTriDiff();
			if (diff == null || diff.isEqual()) {
				getViewer().setChecked(elem, false);
				return;
			}
			if (!checked) {
				getSelection().remove(elem);
			} else {
				var added = getSelection().add(elem);
				if (added) {
					setSelection(elem.parent, true, true);
				}
			}
			if (onlyUp)
				return;
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

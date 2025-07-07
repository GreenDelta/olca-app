package org.openlca.app.collaboration.viewers.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.collaboration.dialogs.JsonCompareDialog;
import org.openlca.app.collaboration.navigation.actions.ConflictResolutions;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.ConflictResolver.ConflictResolutionType;
import org.openlca.git.actions.ConflictResolver.GitContext;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.TriDiff;
import org.openlca.git.repo.OlcaRepository;

import com.google.gson.JsonObject;

public class ConflictViewer extends DiffNodeViewer {

	private final ConflictResolutions resolutions;
	private final GitContext context;
	private Runnable onMerge;

	public ConflictViewer(Composite parent, OlcaRepository repo, ConflictResolutions resolutions, GitContext context) {
		super(parent, repo);
		this.resolutions = resolutions;
		this.context = context;
	}

	public void setOnMerge(Runnable onMerge) {
		this.onMerge = onMerge;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		var viewer = Trees.createViewer(parent);
		viewer.setLabelProvider(new DiffNodeLabelProvider());
		viewer.setContentProvider(new DiffNodeContentProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
		return viewer;
	}

	@Override
	public void setInput(Collection<DiffNode> collection) {
		super.setInput(collection);
		revealConflicts();
	}

	@Override
	public void setInput(DiffNode[] input) {
		super.setInput(input);
		revealConflicts();
	}

	private void revealConflicts() {
		var conflicts = getConflicts();
		for (var conflict : conflicts) {
			getViewer().reveal(conflict);
		}
	}

	public boolean hasConflicts() {
		return !getConflicts().isEmpty();
	}

	public List<DiffNode> getConflicts() {
		var conflicts = new ArrayList<DiffNode>();
		var nodes = new Stack<DiffNode>();
		nodes.addAll(root.children);
		while (!nodes.isEmpty()) {
			var node = nodes.pop();
			nodes.addAll(node.children);
			if (!node.isModelNode())
				continue;
			var diff = (TriDiff) node.content;
			if (!diff.isConflict())
				continue;
			if (resolutions.contains(diff, context))
				continue;
			conflicts.add(node);
		}
		return conflicts;
	}

	@Override
	protected void openCompareDialog(DiffNode selected, TriDiff diff, JsonNode node) {
		var dialog = JsonCompareDialog.forMerging(node);
		var dialogResult = dialog.open();
		if (dialogResult == JsonCompareDialog.CANCEL)
			return;
		var resolution = toResolution(node, dialogResult);
		resolutions.put(diff, resolution);
		onMerge.run();
		getViewer().refresh(selected);
	}

	@Override
	protected JsonObject getRightJson(TriDiff diff) {
		if (context == GitContext.WORKSPACE) {
			var resolution = resolutions.get(diff, GitContext.LOCAL);
			if (resolution != null && resolution.type == ConflictResolutionType.MERGE) {
				var json = resolution.data.deepCopy();
				RefJson.split(json, diff.type);
				return json;
			}
		}
		return super.getRightJson(diff);
	}

	private ConflictResolution toResolution(JsonNode node, int dialogResult) {
		if (dialogResult == JsonCompareDialog.OVERWRITE || node.hasEqualValues())
			return ConflictResolution.overwrite(context);
		if (dialogResult == JsonCompareDialog.KEEP || node.leftEqualsOriginal())
			return ConflictResolution.keep(context);
		var merged = RefJson.getMergedData(node);
		return ConflictResolution.merge(context, merged);
	}

	@Override
	protected Overlay getOverlay(TriDiff diff) {
		if (!resolutions.contains(diff, context))
			return super.getOverlay(diff);
		var resolution = resolutions.get(diff, context);
		if (resolution != null &&
				(resolution.type == ConflictResolutionType.KEEP || resolution.type == ConflictResolutionType.IS_EQUAL))
			return null;
		if (resolution != null && resolution.type == ConflictResolutionType.MERGE)
			return Overlay.MERGED;
		if (diff.right != null && diff.right.diffType == DiffType.DELETED)
			return Overlay.DELETE_FROM_LOCAL;
		if (diff.left == null || diff.left.diffType == DiffType.DELETED)
			return Overlay.ADD_TO_LOCAL;
		return Overlay.MODIFY_IN_LOCAL;
	}

	public void solveAll(ConflictResolution resolution) {
		resolutions.clear(context);
		getConflicts().stream()
				.map(DiffNode::contentAsTriDiff)
				.forEach(d -> resolutions.put(d, resolution));
	}

}

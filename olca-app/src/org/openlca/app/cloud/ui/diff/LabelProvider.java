package org.openlca.app.cloud.ui.diff;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Labels;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

	private final ActionType action;

	LabelProvider(ActionType action) {
		this.action = action;
	}

	@Override
	public String getText(Object element) {
		if (element == null)
			return null;
		DiffNode node = (DiffNode) element;
		if (node.isModelTypeNode())
			return Labels.modelType((ModelType) node.content);
		DiffResult result = (DiffResult) node.content;
		if (result.remote != null && (action == ActionType.FETCH || action == ActionType.COMPARE_AHEAD))
			return result.remote.name;
		if (result.local != null)
			return result.local.getDataset().name;
		return result.remote.name;
	}

	@Override
	public Image getImage(Object element) {
		if (element == null)
			return null;
		DiffNode node = (DiffNode) element;
		if (node.isModelTypeNode())
			return Images.getForCategory((ModelType) node.content);
		return getImage((DiffResult) node.content);
	}

	private Image getImage(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		Overlay overlay = getOverlay(diff);
		if (dataset.type == ModelType.CATEGORY)
			return Images.getForCategory(dataset.categoryType, overlay);
		return Images.get(dataset.type, overlay);
	}

	private Overlay getOverlay(DiffResult result) {
		if (result.noAction())
			return null;
		if (result.conflict()) {
			if (action == ActionType.COMPARE_AHEAD || action == ActionType.COMPARE_BEHIND)
				return getOverlayForComparisonConflict(result);
			if (result.mergedData != null || result.overwriteLocalChanges || result.overwriteRemoteChanges)
				return getOverlayForMerged(result);
			return Overlay.CONFLICT;
		}
		if (action == ActionType.FETCH || action == ActionType.COMPARE_AHEAD)
			return getOverlayForFetch(result);
		return getOverlayForCommit(result);
	}

	private Overlay getOverlayForMerged(DiffResult result) {
		if (!result.overwriteLocalChanges)
			return Overlay.MERGED;
		if (result.remote.isDeleted())
			return Overlay.DELETE_FROM_LOCAL;
		if (result.local == null || result.local.type == DiffType.DELETED)
			return Overlay.ADD_TO_LOCAL;
		return Overlay.MODIFY_IN_LOCAL;
	}

	private Overlay getOverlayForFetch(DiffResult result) {
		if (result.local == null)
			return Overlay.ADD_TO_LOCAL;
		if (result.remote == null)
			return Overlay.DELETE_FROM_LOCAL;
		if (result.local.type == DiffType.NO_DIFF) {
			if (result.remote.isDeleted())
				return Overlay.DELETE_FROM_LOCAL;
			return Overlay.MODIFY_IN_LOCAL;
		}
		if (result.local.type == DiffType.DELETED)
			return Overlay.ADD_TO_LOCAL;
		if (result.local.type == DiffType.CHANGED)
			return Overlay.MODIFY_IN_LOCAL;
		return null;
	}

	private Overlay getOverlayForCommit(DiffResult result) {
		if (result.local == null)
			return Overlay.DELETE_FROM_REMOTE;
		if (result.remote == null)
			return Overlay.ADD_TO_REMOTE;
		if (result.local.type == DiffType.NO_DIFF) {
			if (result.remote.isDeleted())
				return Overlay.ADD_TO_REMOTE;
			return Overlay.MODIFY_IN_REMOTE;
		}
		if (result.local.type == DiffType.DELETED)
			return Overlay.DELETE_FROM_REMOTE;
		if (result.local.type == DiffType.CHANGED)
			return Overlay.MODIFY_IN_REMOTE;
		return null;
	}

	private Overlay getOverlayForComparisonConflict(DiffResult result) {
		if (action == ActionType.COMPARE_AHEAD) {
			// if remote dataset was unchanged or both deleted, "noAction" would
			// have return null in "getOverlay()" so it must imply a conflict
			if (result.local.type.isOneOf(DiffType.NEW, DiffType.CHANGED, DiffType.DELETED))
				return Overlay.CONFLICT;
			if (result.remote.isDeleted())
				return Overlay.DELETE_FROM_LOCAL;
			return Overlay.MODIFY_IN_LOCAL;
		}
		switch (result.local.type) {
		case NEW:
			if (result.remote.isAdded() || result.remote.isDeleted())
				return Overlay.ADD_TO_REMOTE;
			return Overlay.MODIFY_IN_REMOTE;
		case CHANGED:
			if (result.remote.isDeleted())
				return Overlay.ADD_TO_REMOTE;
			return Overlay.MODIFY_IN_REMOTE;
		case DELETED:
			return Overlay.DELETE_FROM_REMOTE;
		case NO_DIFF:
			if (result.remote.isDeleted())
				return Overlay.ADD_TO_REMOTE;
			return Overlay.MODIFY_IN_REMOTE;
		}
		return null;
	}

}
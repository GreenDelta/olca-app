package org.openlca.app.cloud.ui.diff;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Labels;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

	@Override
	public String getText(Object element) {
		if (element == null)
			return null;
		DiffNode node = (DiffNode) element;
		if (node.isModelTypeNode())
			return Labels.modelType((ModelType) node.content);
		return ((DiffResult) node.content).getDisplayName();
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
		DiffResponse response = result.getType();
		if (response == null)
			return null;
		switch (response) {
		case ADD_TO_LOCAL:
			return Overlay.ADD_TO_LOCAL;
		case ADD_TO_REMOTE:
			return Overlay.ADD_TO_REMOTE;
		case MODIFY_IN_LOCAL:
			return Overlay.MODIFY_IN_LOCAL;
		case MODIFY_IN_REMOTE:
			return Overlay.MODIFY_IN_REMOTE;
		case DELETE_FROM_LOCAL:
			return Overlay.DELETE_FROM_LOCAL;
		case DELETE_FROM_REMOTE:
			return Overlay.DELETE_FROM_REMOTE;
		case CONFLICT:
			if (result.getMergedData() == null && !result.overwriteLocalChanges() && !result.overwriteRemoteChanges())
				return Overlay.CONFLICT;
			return Overlay.MERGED;
		default:
			return null;
		}
	}

}
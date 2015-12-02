package org.openlca.app.cloud.ui.diff;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.Category;
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
			return Images.getIcon(dummyCategory((ModelType) node.content));
		return getImage((DiffResult) node.content);
	}

	private Image getImage(DiffResult diff) {
		Dataset dataset = diff.getDataset();
		ImageType image = null;
		if (dataset.getType() == ModelType.CATEGORY)
			image = Images.getImageType(dummyCategory(dataset
					.getCategoryType()));
		else
			image = Images.getImageType(dataset.getType());
		ImageType overlay = getOverlay(diff);
		if (overlay == null)
			return ImageManager.getImage(image);
		return ImageManager.getImageWithOverlay(image, overlay);
	}

	private ImageType getOverlay(DiffResult result) {
		DiffResponse response = result.getType();
		if (response == null)
			return null;
		switch (response) {
		case ADD_TO_LOCAL:
			return ImageType.OVERLAY_ADD_TO_LOCAL;
		case ADD_TO_REMOTE:
			return ImageType.OVERLAY_ADD_TO_REMOTE;
		case MODIFY_IN_LOCAL:
			return ImageType.OVERLAY_MODIFY_IN_LOCAL;
		case MODIFY_IN_REMOTE:
			return ImageType.OVERLAY_MODIFY_IN_REMOTE;
		case DELETE_FROM_LOCAL:
			return ImageType.OVERLAY_DELETE_FROM_LOCAL;
		case DELETE_FROM_REMOTE:
			return ImageType.OVERLAY_DELETE_FROM_REMOTE;
		case CONFLICT:
			if (result.getMergedData() == null
					&& !result.overwriteLocalChanges()
					&& !result.overwriteRemoteChanges())
				return ImageType.OVERLAY_CONFLICT;
			return ImageType.OVERLAY_MERGED;
		default:
			return null;
		}
	}

	private Category dummyCategory(ModelType type) {
		Category dummy = new Category();
		dummy.setModelType(type);
		return dummy;
	}
}
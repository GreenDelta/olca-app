package org.openlca.app.cloud.ui.commits;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.cloud.model.data.Commit;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.core.model.ModelType;

class LabelProvider extends BaseLabelProvider implements ILabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof Commit)
			return Icon.COMMIT.get();
		if (!(element instanceof FetchRequestData))
			return null;
		FetchRequestData data = (FetchRequestData) element;
		Overlay overlay = null;
		if (data.isAdded())
			overlay = Overlay.ADDED;
		else if (data.isDeleted())
			overlay = Overlay.DELETED;
		if (data.type == ModelType.CATEGORY)
			return Images.getForCategory(data.categoryType, overlay);
		return Images.get(data.type, overlay);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Commit)
			return getCommitText((Commit) element);
		if (element instanceof FetchRequestData)
			return CloudUtil.getFileReferenceText((FetchRequestData) element);
		return null;
	}

	private String getCommitText(Commit commit) {
		String text = commit.user + ": ";
		text += commit.message + " (";
		text += CloudUtil.formatCommitDate(commit.timestamp) + ")";
		return text;
	}

}
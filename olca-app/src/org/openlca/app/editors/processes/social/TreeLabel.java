package org.openlca.app.editors.processes.social;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.ImageType;

class TreeLabel extends BaseLabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col != 0)
			return null;
		return ImageType.FOLDER_SMALL.get();
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (obj instanceof CategoryNode)
			return text((CategoryNode) obj, col);
		if (obj instanceof SocialAspect)
			return text((SocialAspect) obj, col);
		else
			return null;
	}

	private String text(CategoryNode n, int col) {
		if (col == 0 && n.category != null)
			return n.category.getName();
		else
			return null;
	}

	private String text(SocialAspect a, int col) {
		switch (col) {
		case 0:
			return a.indicator != null ? a.indicator.getName() : null;
		case 1:
			return a.rawAmount;
		case 2:
			return a.indicator != null ? a.indicator.unit : null;
		default:
			return null;
		}
	}

}
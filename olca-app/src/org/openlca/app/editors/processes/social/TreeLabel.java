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
		if (obj instanceof CategoryNode)
			return ImageType.INDICATOR_CATEGORY_ICON.get();
		if (obj instanceof SocialAspect)
			return ImageType.INDICATOR_ICON.get();
		else
			return null;
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
			return getRawAmount(a);
		default:
			return null;
		}
	}

	private String getRawAmount(SocialAspect a) {
		if (a == null || a.rawAmount == null)
			return null;
		String t = a.rawAmount;
		if (a.indicator != null && a.indicator.unitOfMeasurement != null) {
			String u = a.indicator.unitOfMeasurement;
			t += " [" + u + "]";
		}
		return t;
	}

}
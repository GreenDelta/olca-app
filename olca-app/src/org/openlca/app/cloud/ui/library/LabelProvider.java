package org.openlca.app.cloud.ui.library;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.cloud.model.LibraryRestriction;
import org.openlca.cloud.model.RestrictionType;
import org.openlca.core.model.ModelType;

class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int column) {
		LibraryRestriction restriction = (LibraryRestriction) element;
		switch (column) {
		case 0:
			if (restriction.dataset.type == ModelType.CATEGORY)
				return Images.getForCategory(restriction.dataset.categoryType);
			return Images.get(restriction.dataset.type);
		case 2:
			return restriction.type == RestrictionType.FORBIDDEN
					? Icon.ERROR.get()
					: Icon.WARNING.get();
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int column) {
		LibraryRestriction restriction = (LibraryRestriction) element;
		switch (column) {
		case 0:
			return CloudUtil.toFullPath(restriction.dataset);
		case 1:
			return restriction.library;
		default:
			return null;
		}
	}

}

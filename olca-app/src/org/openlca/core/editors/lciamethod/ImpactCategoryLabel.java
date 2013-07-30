package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ImpactCategory;

/**
 * The label provider for the impact category table in the LCIA method editor.
 */
class ImpactCategoryLabel implements ITableLabelProvider, ITableFontProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return ImageType.LCIA_ICON.get();
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof ImpactCategory))
			return null;
		ImpactCategory lciaCategory = (ImpactCategory) element;
		switch (columnIndex) {
		case 0:
			return lciaCategory.getName();
		case 1:
			return lciaCategory.getDescription();
		case 2:
			return lciaCategory.getReferenceUnit();
		default:
			return null;
		}
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		return null;
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}
}
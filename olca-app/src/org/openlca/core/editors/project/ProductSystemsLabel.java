package org.openlca.core.editors.project;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.ProductSystem;

/**
 * The label provider for the product system table in the project editor.
 */
class ProductSystemsLabel implements ITableLabelProvider, ITableFontProvider {

	@Override
	public void addListener(final ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		if (columnIndex == 0)
			return ImageType.PRODUCT_SYSTEM_ICON.get();
		return null;
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		if (!(element instanceof ProductSystem))
			return null;
		ProductSystem productSystem = (ProductSystem) element;
		switch (columnIndex) {
		case 0:
			return productSystem.getName();
		default:
			return null;
		}
	}

	@Override
	public Font getFont(final Object element, final int columnIndex) {
		return null;
	}

	@Override
	public boolean isLabelProperty(final Object element, final String property) {
		return false;
	}

	@Override
	public void removeListener(final ILabelProviderListener listener) {
	}
}
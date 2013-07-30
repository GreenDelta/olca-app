package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.core.model.Source;

/**
 * The label provider for sources in the process editor.
 */
class SourceLabelProvider implements ITableLabelProvider, ITableFontProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return ImageType.SOURCE_ICON.get();
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Source))
			return null;
		Source source = (Source) element;
		if (columnIndex == 0)
			return source.getName();
		return null;
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
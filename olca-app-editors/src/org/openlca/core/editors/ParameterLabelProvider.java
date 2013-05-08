package org.openlca.core.editors;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.model.Parameter;

/**
 * The label provider for parameter tables.
 */
class ParameterLabelProvider implements ITableLabelProvider, ITableFontProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Parameter))
			return null;
		Parameter parameter = (Parameter) element;
		switch (columnIndex) {
		case 0:
			return parameter.getName();
		case 1:
			return parameter.getExpression().getFormula();
		case 2:
			return Double.toString(parameter.getExpression().getValue());
		case 3:
			return parameter.getDescription();
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
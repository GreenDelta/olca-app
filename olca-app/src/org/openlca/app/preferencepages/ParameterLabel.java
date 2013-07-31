package org.openlca.app.preferencepages;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.model.Parameter;

/** The label provider of the parameter table. */
class ParameterLabel implements ITableLabelProvider {

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
			Double result = parameter.getExpression().getValue();
			if (result != null)
				return Double.toString(result);
			return null;
		case 3:
			return parameter.getDescription();
		default:
			return null;
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

}

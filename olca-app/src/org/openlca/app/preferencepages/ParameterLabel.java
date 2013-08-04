package org.openlca.app.preferencepages;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.model.Parameter;

/** The label provider of the parameter table. */
class ParameterLabel extends LabelProvider implements ITableLabelProvider {

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

}

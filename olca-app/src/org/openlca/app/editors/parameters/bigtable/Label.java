package org.openlca.app.editors.parameters.bigtable;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;

class Label extends LabelProvider
		implements ITableLabelProvider, ITableColorProvider {

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (col != 1 || !(obj instanceof Param p))
			return null;
		return switch (p.scope()) {
			case GLOBAL -> Images.get(ModelType.PARAMETER);
			case IMPACT -> Images.get(ModelType.IMPACT_CATEGORY);
			case PROCESS -> Images.get(ModelType.PROCESS);
		};
	}

	@Override
	public Color getBackground(Object obj, int col) {
		return null;
	}

	@Override
	public Color getForeground(Object obj, int col) {
		if (!(obj instanceof Param param))
			return null;
		if (col == 1 &&
				param.scope() != ParameterScope.GLOBAL
				&& param.owner == null)
			return Colors.systemColor(SWT.COLOR_RED);
		if (param.evalError && (col == 2 || col == 3))
			return Colors.systemColor(SWT.COLOR_RED);
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (!(obj instanceof Param param))
			return null;
		if (param.parameter == null)
			return " - ";
		Parameter p = param.parameter;
		switch (col) {
		case 0:
			return p.name;
		case 1:
			if (param.scope() == ParameterScope.GLOBAL)
				return M.GlobalParameter;
			if (param.owner == null)
				return "!! missing !!";
			return Labels.name(param.owner);
		case 2:
			return Double.toString(p.value);
		case 3:
			if (p.isInputParameter)
				return null;
			return param.evalError
					? "!! error !! " + p.formula
					: p.formula;
		case 4:
			return !p.isInputParameter || p.uncertainty == null
					? null
					: p.uncertainty.toString();
		case 5:
			return p.description;
		default:
			return null;
		}
	}
}

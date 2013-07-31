package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Unit;

/**
 * The label provider for impact assessment factors in the LCIA method editor.
 */
class ImpactFactorLabel implements ITableLabelProvider, ITableFontProvider {

	private IDatabase database;

	public ImpactFactorLabel(IDatabase database) {
		this.database = database;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Image getColumnImage(Object element, int column) {
		if (column != 0 || !(element instanceof ImpactFactor))
			return null;
		ImpactFactor factor = (ImpactFactor) element;
		Flow flow = factor.getFlow();
		if (flow == null || flow.getFlowType() == null)
			return null;
		switch (flow.getFlowType()) {
		case PRODUCT_FLOW:
			return ImageType.FLOW_PRODUCT.get();
		case WASTE_FLOW:
			return ImageType.FLOW_WASTE.get();
		case ELEMENTARY_FLOW:
			return ImageType.FLOW_SUBSTANCE.get();
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int column) {
		if (!(element instanceof ImpactFactor))
			return null;
		ImpactFactor factor = (ImpactFactor) element;
		switch (column) {
		case FactorTable.FLOW_COLUMN:
			return flowName(factor);
		case FactorTable.CATEGORY_COLUM:
			return categoryPath(factor);
		case FactorTable.PROPERTY_COLUMN:
			return propertyName(factor);
		case FactorTable.UNIT_COLUMN:
			return unitName(factor);
		case FactorTable.VALUE_COLUMN:
			return Numbers.format(factor.getValue());
		case FactorTable.UNCERTAINTY_COLUMN:
			return UncertaintyLabel.get(factor);
		default:
			return null;
		}
	}

	private String flowName(ImpactFactor factor) {
		Flow flow = factor.getFlow();
		if (flow == null)
			return null;
		String text = flow.getName();
		if (flow.getLocation() != null)
			text += " [" + flow.getLocation().getCode() + "]";
		return text;
	}

	private String categoryPath(ImpactFactor factor) {
		Flow flow = factor.getFlow();
		if (flow == null || flow.getCategory() == null)
			return null;
		return CategoryPath.getShort(flow.getCategory());
	}

	private String propertyName(ImpactFactor factor) {
		FlowPropertyFactor propFactor = factor.getFlowPropertyFactor();
		if (propFactor == null || propFactor.getFlowProperty() == null)
			return null;
		return propFactor.getFlowProperty().getName();
	}

	private String unitName(ImpactFactor factor) {
		Unit unit = factor.getUnit();
		if (unit == null)
			return null;
		return unit.getName();
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
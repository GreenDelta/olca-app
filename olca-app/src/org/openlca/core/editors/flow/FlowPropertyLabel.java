package org.openlca.core.editors.flow;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Numbers;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The label provider for the flow property factors in the flow editor.
 */
class FlowPropertyLabel implements ITableLabelProvider, ITableFontProvider {

	private Flow flowInfo;
	private Font boldFont;

	public FlowPropertyLabel(Flow flowInfo, Table table) {
		this.flowInfo = flowInfo;
		boldFont = UI.boldFont(table);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
		if (boldFont != null && !boldFont.isDisposed())
			boldFont.dispose();
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		switch (columnIndex) {
		case FlowPropertyColumn.NAME:
			return ImageType.FLOW_PROPERTY_ICON.get();
		case FlowPropertyColumn.IS_REFERNCE:
			return getReferenceImage(element);
		default:
			return null;
		}
	}

	private Image getReferenceImage(Object element) {
		if (isReferenceProperty(element))
			return ImageType.CHECK_TRUE.get();
		return ImageType.CHECK_FALSE.get();
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof FlowPropertyFactor))
			return null;
		FlowPropertyFactor factor = (FlowPropertyFactor) element;
		switch (columnIndex) {
		case FlowPropertyColumn.NAME:
			return factor.getFlowProperty().getName();
		case FlowPropertyColumn.FACTOR:
			return Numbers.format(factor.getConversionFactor());
		case FlowPropertyColumn.UNIT:
			return getRefUnitText(factor);
		default:
			return null;
		}
	}

	private String getRefUnitText(FlowPropertyFactor factor) {
		try {
			UnitGroup unitGroup = factor.getFlowProperty().getUnitGroup();
			return unitGroup.getReferenceUnit().getName();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(this.getClass());
			log.error("Cannot load unit group", e);
			return null;
		}
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		if (isReferenceProperty(element))
			return boldFont;
		return null;
	}

	private boolean isReferenceProperty(Object element) {
		if (!(element instanceof FlowPropertyFactor))
			return false;
		FlowPropertyFactor factor = (FlowPropertyFactor) element;
		FlowProperty prop = factor.getFlowProperty();
		FlowProperty refProp = flowInfo.getReferenceFlowProperty();
		return prop != null && refProp != null && prop.equals(refProp);
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

}
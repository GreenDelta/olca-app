package org.openlca.app.editors;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.openlca.core.application.Numbers;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;

/**
 * The label provider for the unit table in the unit group editor.
 */
class UnitLabelProvider implements ITableLabelProvider, ITableFontProvider {

	private UnitGroup unitGroup;
	private Font boldFont;

	public UnitLabelProvider(UnitGroup unitGroup, Table table) {
		this.unitGroup = unitGroup;
		boldFont = UI.boldFont(table);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
		if (boldFont != null && !boldFont.isDisposed())
			boldFont.dispose();
	}

	@Override
	public Image getColumnImage(Object element, int column) {
		if (column == 0)
			return ImageType.UNIT_GROUP_ICON.get();
		if (column != 5)
			return null;
		Unit refUnit = unitGroup.getReferenceUnit();
		if (refUnit != null && refUnit.equals(element))
			return ImageType.CHECK_TRUE.get();
		return ImageType.CHECK_FALSE.get();
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Unit))
			return null;
		Unit unit = (Unit) element;
		switch (columnIndex) {
		case 0:
			return unit.getName();
		case 1:
			return unit.getDescription();
		case 2:
			return unit.getSynonyms();
		case 3:
			return Numbers.format(unit.getConversionFactor());
		case 4:
			return getFormulaText(unit);
		default:
			return null;
		}
	}

	private String getFormulaText(Unit unit) {
		Unit refUnit = unitGroup.getReferenceUnit();
		if (refUnit == null)
			return null;
		String amount = "[" + unit.getName() + "]";
		String refAmount = "[" + refUnit.getName() + "]";
		String factor = Numbers.format(unit.getConversionFactor());
		return amount + " = " + factor + " * " + refAmount;
	}

	@Override
	public Font getFont(Object element, int columnIndex) {
		Unit refUnit = unitGroup.getReferenceUnit();
		if (refUnit != null && refUnit.equals(element))
			return boldFont;
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
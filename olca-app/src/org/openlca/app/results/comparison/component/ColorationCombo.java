package org.openlca.app.results.comparison.component;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.results.comparison.display.ColorCellCriteria;
import org.openlca.app.viewers.combo.AbstractComboViewer;

public class ColorationCombo extends AbstractComboViewer<ColorCellCriteria> {

	public ColorationCombo(Composite parent, ColorCellCriteria... values) {
		super(parent);
		setInput(values);
	}

	@Override
	public Class<ColorCellCriteria> getType() {
		return ColorCellCriteria.class;
	}

	@Override
	public void select(ColorCellCriteria value) {
		if (value == null)
			if (isNullable())
				((TableCombo) getViewer().getControl()).select(0);
			else
				super.select(ColorCellCriteria.PRODUCT);
		else
			super.select(value);
	}

}

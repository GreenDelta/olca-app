package org.openlca.app.viewers.combo;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.core.model.AllocationMethod;

public class AllocationCombo extends AbstractComboViewer<AllocationMethod> {

	public AllocationCombo(Composite parent, AllocationMethod... values) {
		super(parent);
		setNullText(M.None);
		setInput(values);
	}

	@Override
	public void select(AllocationMethod value) {
		if (value == null)
			if (isNullable())
				((TableCombo) getViewer().getControl()).select(0);
			else
				super.select(AllocationMethod.NONE);
		else
			super.select(value);
	}

	@Override
	public Class<AllocationMethod> getType() {
		return AllocationMethod.class;
	}

}

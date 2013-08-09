package org.openlca.app.viewers.combo;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.core.model.AllocationMethod;

public class AllocationMethodViewer extends
		AbstractComboViewer<AllocationMethod> {

	public AllocationMethodViewer(Composite parent) {
		super(parent);
		setNullText(Messages.AsDefinedInProcesses);
		setInput(AllocationMethod.values());
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

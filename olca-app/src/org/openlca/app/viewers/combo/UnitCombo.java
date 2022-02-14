package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

public class UnitCombo extends AbstractComboViewer<Unit> {

	public UnitCombo(Composite parent) {
		super(parent);
		setInput(new Unit[0]);
	}

	public void setInput(UnitGroup unitGroup) {
		try {
			if (unitGroup == null) {
				setInput(new Unit[0]);
			} else {
				setInput(unitGroup.units.toArray(new Unit[0]));
			}
		} catch (Exception e) {
			ErrorReporter.on("Loading units failed", e);
		}
	}

	@Override
	public Class<Unit> getType() {
		return Unit.class;
	}

}

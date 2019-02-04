package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitViewer extends AbstractComboViewer<Unit> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public UnitViewer(Composite parent) {
		super(parent);
		setInput(new Unit[0]);
	}

	public void setInput(UnitGroup unitGroup) {
		try {
			List<Unit> units = unitGroup.units;
			setInput(units.toArray(new Unit[units.size()]));
		} catch (Exception e) {
			log.error("Loading flow properties failed", e);
		}
	}

	@Override
	public Class<Unit> getType() {
		return Unit.class;
	}

}

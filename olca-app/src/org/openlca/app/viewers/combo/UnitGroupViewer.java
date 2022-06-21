package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.openlca.util.Strings;

public class UnitGroupViewer extends AbstractComboViewer<UnitGroupDescriptor> {

	public UnitGroupViewer(Composite parent) {
		super(parent);
		setInput(new UnitGroupDescriptor[0]);
	}

	public void setInput(IDatabase db) {
		var groups = new UnitGroupDao(db).getDescriptors();
		groups.sort((u1, u2) -> Strings.compare(u1.name, u2.name));
		setInput(groups.toArray(new UnitGroupDescriptor[0]));
	}

	@Override
	public Class<UnitGroupDescriptor> getType() {
		return UnitGroupDescriptor.class;
	}
}

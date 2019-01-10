package org.openlca.app.viewers.combo;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupViewer extends AbstractComboViewer<UnitGroupDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public UnitGroupViewer(Composite parent) {
		super(parent);
		setInput(new UnitGroupDescriptor[0]);
	}

	public void setInput(IDatabase db) {
		try {
			List<UnitGroupDescriptor> groups = new UnitGroupDao(db)
					.getDescriptors();
			Collections.sort(groups,
					(u1, u2) -> Strings.compare(u1.name, u2.name));
			setInput(groups.toArray(new UnitGroupDescriptor[groups.size()]));
		} catch (Exception e) {
			log.error("Loading unit groups failed", e);
		}
	}

	@Override
	public Class<UnitGroupDescriptor> getType() {
		return UnitGroupDescriptor.class;
	}

}

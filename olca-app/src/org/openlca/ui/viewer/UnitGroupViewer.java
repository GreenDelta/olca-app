package org.openlca.ui.viewer;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupViewer extends AbstractComboViewer<UnitGroupDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public UnitGroupViewer(Composite parent) {
		super(parent);
		setInput(new UnitGroupDescriptor[0]);
	}

	public void setInput(IDatabase database) {
		try {
			List<UnitGroupDescriptor> groups = new UnitGroupDao(
					database).getDescriptors();
			Collections.sort(groups);
			setInput(groups.toArray(new UnitGroupDescriptor[groups.size()]));
		} catch (Exception e) {
			log.error("Loading unit groups failed", e);
		}
	}

}

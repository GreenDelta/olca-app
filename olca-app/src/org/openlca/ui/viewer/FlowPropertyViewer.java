package org.openlca.ui.viewer;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowPropertyViewer extends AbstractComboViewer<BaseDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public FlowPropertyViewer(Composite parent) {
		super(parent);
		setInput(new FlowPropertyDescriptor[0]);
	}

	public void setInput(IDatabase database) {
		try {
			List<BaseDescriptor> properties = new FlowPropertyDao(
					database.getEntityFactory()).getDescriptors();
			Collections.sort(properties);
			setInput(properties.toArray(new BaseDescriptor[properties.size()]));
		} catch (Exception e) {
			log.error("Loading flow properties failed", e);
		}
	}

}

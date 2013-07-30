package org.openlca.app.viewer;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowPropertyViewer extends
		AbstractComboViewer<FlowPropertyDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public FlowPropertyViewer(Composite parent) {
		super(parent);
		setInput(new FlowPropertyDescriptor[0]);
	}

	public void setInput(IDatabase database) {
		try {
			List<FlowPropertyDescriptor> properties = new FlowPropertyDao(
					database).getDescriptors();
			Collections.sort(properties);
			setInput(properties.toArray(new FlowPropertyDescriptor[properties
					.size()]));
		} catch (Exception e) {
			log.error("Loading flow properties failed", e);
		}
	}

}

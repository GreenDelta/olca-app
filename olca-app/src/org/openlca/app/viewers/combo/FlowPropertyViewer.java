package org.openlca.app.viewers.combo;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowPropertyViewer extends
		AbstractComboViewer<FlowPropertyDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public FlowPropertyViewer(Composite parent) {
		super(parent);
		setInput(new FlowPropertyDescriptor[0]);
	}

	public void setInput(IDatabase db) {
		try {
			List<FlowPropertyDescriptor> props = new FlowPropertyDao(
					db).getDescriptors();
			Collections.sort(props,
					(p1, p2) -> Strings.compare(p1.name, p2.name));
			setInput(props.toArray(
					new FlowPropertyDescriptor[props.size()]));
		} catch (Exception e) {
			log.error("Loading flow properties failed", e);
		}
	}

	@Override
	public Class<FlowPropertyDescriptor> getType() {
		return FlowPropertyDescriptor.class;
	}

}

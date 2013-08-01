package org.openlca.app.viewers;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodViewer extends
		AbstractComboViewer<ImpactMethodDescriptor> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public ImpactMethodViewer(Composite parent) {
		super(parent);
		setInput(new ImpactMethodDescriptor[0]);
	}

	public void setInput(IDatabase database) {
		try {
			List<ImpactMethodDescriptor> descriptors = new ImpactMethodDao(
					database).getDescriptors();
			Collections.sort(descriptors);
			setInput(descriptors.toArray(new ImpactMethodDescriptor[descriptors
					.size()]));
		} catch (Exception e) {
			log.error("Loading impact methods failed", e);
		}
	}

}

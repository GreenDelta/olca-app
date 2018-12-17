package org.openlca.app.viewers.combo;

import java.util.Collection;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.db.Cache;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	public ProcessViewer(Composite parent) {
		super(parent);
		setInput(new ProcessDescriptor[0]);
	}

	public void setInput(ProductSystem productSystem) {
		EntityCache cache = Cache.getEntityCache();
		Collection<ProcessDescriptor> descriptors = cache.getAll(
				ProcessDescriptor.class, productSystem.processes).values();
		setInput(descriptors.toArray(new ProcessDescriptor[descriptors.size()]));
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

}

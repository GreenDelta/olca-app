package org.openlca.app.viewers.combo;

import java.util.Collection;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	private EntityCache cache;

	public ProcessViewer(Composite parent, EntityCache cache) {
		super(parent);
		this.cache = cache;
		setInput(new ProcessDescriptor[0]);
	}

	public void setInput(ProductSystem productSystem) {
		Collection<ProcessDescriptor> descriptors = cache.getAll(
				ProcessDescriptor.class, productSystem.getProcesses()).values();
		setInput(descriptors.toArray(new ProcessDescriptor[descriptors.size()]));
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

}

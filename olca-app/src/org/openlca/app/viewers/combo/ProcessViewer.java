package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.Cache;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	private Cache cache;

	public ProcessViewer(Composite parent, Cache cache) {
		super(parent);
		this.cache = cache;
		setInput(new ProcessDescriptor[0]);
	}

	public void setInput(ProductSystem productSystem) {
		List<ProcessDescriptor> descriptors = cache
				.getProcessDescriptors(productSystem.getProcesses());
		setInput(descriptors.toArray(new ProcessDescriptor[descriptors.size()]));
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

}

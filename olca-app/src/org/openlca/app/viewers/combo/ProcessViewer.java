package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	private ProcessDao processDao;

	public ProcessViewer(Composite parent, IDatabase database) {
		super(parent);
		setInput(new ProcessDescriptor[0]);
	}

	public void setInput(ProductSystem productSystem) {
		List<ProcessDescriptor> descriptors = processDao
				.getDescriptors(productSystem.getProcesses());
		setInput(descriptors.toArray(new ProcessDescriptor[descriptors.size()]));
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

}

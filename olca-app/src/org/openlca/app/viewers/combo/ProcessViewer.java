package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	public ProcessViewer(Composite parent) {
		super(parent);
		setInput(new ProcessDescriptor[0]);
	}

	public void setInput(ProductSystem productSystem) {
		List<Process> list = productSystem.getProcesses();
		ProcessDescriptor[] processes = new ProcessDescriptor[list.size()];
		for (int i = 0; i < list.size(); i++)
			processes[i] = Descriptors.toDescriptor(list.get(i));
		setInput(processes);
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

}

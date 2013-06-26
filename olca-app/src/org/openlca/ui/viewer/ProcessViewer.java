package org.openlca.ui.viewer;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

public class ProcessViewer extends AbstractComboViewer<Process> {

	public ProcessViewer(Composite parent) {
		super(parent);
		setInput(new Process[0]);
	}

	public void setInput(ProductSystem productSystem) {
		List<Process> list = productSystem.getProcesses();
		Process[] processes = list.toArray(new Process[list.size()]);
		setInput(processes);
	}

}

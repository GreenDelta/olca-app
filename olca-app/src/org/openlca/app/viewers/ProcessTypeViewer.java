package org.openlca.app.viewers;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.ProcessType;

public class ProcessTypeViewer extends AbstractComboViewer<ProcessType> {

	public ProcessTypeViewer(Composite parent) {
		super(parent);
		setInput(ProcessType.values());
	}

}

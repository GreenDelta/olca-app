package org.openlca.app.processes;

import java.util.Map;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.PedigreeMatrix;
import org.openlca.core.model.PedigreeMatrixRow;

class PedigreeShellFinishHandler implements SelectionListener {

	private boolean justClose = false;
	private PedigreeShell shell;
	private Exchange exchange;

	public PedigreeShellFinishHandler(PedigreeShell shell, Exchange exchange) {
		this.shell = shell;
		this.exchange = exchange;
	}

	public PedigreeShellFinishHandler(PedigreeShell shell) {
		this.shell = shell;
	}

	public void setJustClose(boolean justClose) {
		this.justClose = justClose;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (!justClose && exchange != null) {
			Map<PedigreeMatrixRow, Integer> values = shell.getSelection();
			exchange.setBaseUncertainty(shell.getBaseValue());
			exchange.setPedigreeUncertainty(PedigreeMatrix.toString(values));
		}
		shell.dispose();
	}

}

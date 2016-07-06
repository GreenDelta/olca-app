package org.openlca.app.editors.processes.exchanges;

import java.util.Map;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.PedigreeMatrix;
import org.openlca.core.model.PedigreeMatrixRow;

class PedigreeFinishHandler implements SelectionListener {

	private static final int OK = 0;
	private static final int CANCEL = 1;
	private static final int DELETE = 2;

	private PedigreeShell shell;
	private Exchange exchange;

	private final int type;

	public static PedigreeFinishHandler forOk(PedigreeShell shell,
			Exchange exchange) {
		return new PedigreeFinishHandler(shell, exchange, OK);
	}

	public static PedigreeFinishHandler forCancel(PedigreeShell shell) {
		return new PedigreeFinishHandler(shell, null, CANCEL);
	}

	public static PedigreeFinishHandler forDelete(PedigreeShell shell,
			Exchange exchange) {
		return new PedigreeFinishHandler(shell, exchange, DELETE);
	}

	private PedigreeFinishHandler(PedigreeShell shell, Exchange exchange,
			int type) {
		this.shell = shell;
		this.exchange = exchange;
		this.type = type;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		switch (type) {
		case OK:
			handleOk();
			break;
		case DELETE:
			handleDelete();
			break;
		default:
			break;
		}
		shell.dispose();
	}

	private void handleDelete() {
		if (exchange == null)
			return;
		exchange.setDqEntry(null);
	}

	private void handleOk() {
		if (exchange == null)
			return;
		Map<PedigreeMatrixRow, Integer> values = shell.getSelection();
		exchange.setBaseUncertainty(shell.getBaseValue());
		exchange.setDqEntry(PedigreeMatrix.toString(values));
	}

}

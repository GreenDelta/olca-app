package org.openlca.app.editors.processes.data_quality;

import java.util.function.Consumer;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

class DataQualityFinishHandler implements SelectionListener {

	private DataQualityShell shell;
	private Consumer<DataQualityShell> handleFn;

	DataQualityFinishHandler(DataQualityShell shell, Consumer<DataQualityShell> handleFn) {
		this.shell = shell;
		this.handleFn = handleFn;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (handleFn != null) {
			handleFn.accept(shell);
		}
		shell.dispose();
	}

}

package org.openlca.core.editors.process;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;

class RemoveExchangeAction extends DeleteWithQuestionAction {

	private boolean input;
	private TableViewer viewer;
	private Process process;

	public RemoveExchangeAction(boolean input, Process process) {
		this.input = input;
		this.process = process;
		setId("InputOutputPage.RemoveExchangeAction");
		if (input)
			setText(Messages.Processes_RemoveInputText);
		else
			setText(Messages.Processes_RemoveOutputText);
		setIcons();
	}

	private void setIcons() {
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	public void setViewer(TableViewer viewer) {
		this.viewer = viewer;
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection)
					setEnabled((IStructuredSelection) selection);
			}
		});
	}

	private void setEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			setEnabled(false);
		else {
			Object obj = selection.getFirstElement();
			if (obj instanceof Exchange) {
				boolean b = !obj.equals(process.getQuantitativeReference());
				setEnabled(b);
			}
		}
	}

	@Override
	public void delete() {
		if (viewer == null)
			return;
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) viewer
					.getSelection();
			checkAndDeleteAll(structuredSelection);
		}
	}

	private void checkAndDeleteAll(IStructuredSelection selection) {
		Iterator<?> it = selection.iterator();
		while (it.hasNext()) {
			Object obj = it.next();
			if (obj instanceof Exchange) {
				checkAndDelete((Exchange) obj);
			}
		}
	}

	private void checkAndDelete(Exchange exchange) {
		if (canDelete(exchange)) {
			process.getExchanges().remove(exchange);
			if (input)
				viewer.setInput(process.getInputs());
			else
				viewer.setInput(process.getOutputs());
		}
	}

	private boolean canDelete(Exchange exchange) {
		if (exchange.equals(process.getQuantitativeReference()))
			return false;
		// TODO: no process locking in product systems
		return true;
	}
}
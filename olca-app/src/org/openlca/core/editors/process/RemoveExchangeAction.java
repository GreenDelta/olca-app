package org.openlca.core.editors.process;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.application.wizards.DeleteWizard;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;

class RemoveExchangeAction extends DeleteWithQuestionAction {

	private boolean input;
	private TableViewer viewer;
	private IDatabase database;
	private Process process;

	public RemoveExchangeAction(boolean input, IDatabase database,
			Process process) {
		this.input = input;
		this.database = database;
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
			process.remove(exchange);
			if (input)
				viewer.setInput(process.getInputs());
			else
				viewer.setInput(process.getOutputs());
		}
	}

	private boolean canDelete(Exchange exchange) {
		if (exchange.equals(process.getQuantitativeReference()))
			return false;
		boolean canDelete = true;
		DeleteWizard wizard = new DeleteWizard(database,
				new ExchangeReferenceSearcher(), exchange);
		if (wizard.hasProblems()) {
			canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;
		}
		return canDelete;
	}
}
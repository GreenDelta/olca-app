package org.openlca.app.ilcd_network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.UI;
import org.openlca.ilcd.descriptors.DescriptorList;
import org.openlca.ilcd.descriptors.ProcessDescriptor;
import org.openlca.ilcd.io.NetworkClient;
import org.openlca.ilcd.processes.Process;

/**
 * The wizard page for searching processes in the ILCD network.
 */
public class ProcessSearchPage extends WizardPage {

	private Text text;
	private TableViewer viewer;

	public ProcessSearchPage() {
		super("ILCD-ProcessSearchPage");
		setTitle(Messages.Search);
		setDescription(Messages.ILCD_SearchPageDescription);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(1, false));
		createSearchSection(container);
		createResultSection(container);
	}

	private void createSearchSection(Composite container) {
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		UI.gridData(composite, true, false);
		new ConnectionText(composite);
		new Label(composite, SWT.NONE).setText(Messages.Process);
		text = new Text(composite, SWT.BORDER);
		UI.gridData(text, true, false);
		SearchAction action = new SearchAction();
		text.addTraverseListener(action);
		createButton(composite, Messages.Search, action);
	}

	private void createButton(Composite parent, String text,
			SelectionListener action) {
		Button button = new Button(parent, SWT.NONE);
		UI.gridData(button, false, false).widthHint = 60;
		button.setText(text);
		button.addSelectionListener(action);
	}

	private void createResultSection(Composite container) {
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		UI.gridData(composite, true, true);
		Table table = new Table(composite, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		UI.gridData(table, true, true);
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		viewer = new SearchResultViewer(table);
		viewer.addSelectionChangedListener((event) -> {
			ISelection selection = event.getSelection();
			if (selection == null || selection.isEmpty()) {
				setPageComplete(false);
			} else {
				setPageComplete(true);
			}
		});
	}

	private void runSearch(String term) {
		try {
			NetworkClient client = Preference.createClient();
			client.connect();
			DescriptorList result = client.search(Process.class, term);
			if (result != null && result.getDescriptors() != null) {
				viewer.setInput(result.getDescriptors().toArray());
			}
		} catch (Exception e) {
			Dialog.showError(getShell(),
					Messages.ILCD_SearchFailedMessage + e.getMessage());
		}
	}

	private class SearchAction implements SelectionListener, TraverseListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			runSearch(text.getText());
		}

		@Override
		public void keyTraversed(TraverseEvent e) {
			if (e.detail == SWT.TRAVERSE_RETURN)
				runSearch(text.getText());
		}

	}

	public List<ProcessDescriptor> getSelectedProcesses() {
		List<ProcessDescriptor> processes = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		if (selection != null) {
			Iterator<?> it = selection.iterator();
			while (it.hasNext()) {
				Object element = it.next();
				if (element instanceof ProcessDescriptor) {
					ProcessDescriptor process = (ProcessDescriptor) element;
					processes.add(process);
				}
			}
		}
		return processes;
	}
}

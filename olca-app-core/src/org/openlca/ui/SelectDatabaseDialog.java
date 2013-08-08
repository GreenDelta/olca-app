package org.openlca.ui;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.core.application.App;
import org.openlca.core.database.DatabaseDescriptor;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.resources.ImageType;

public class SelectDatabaseDialog extends FormDialog {

	private ListViewer dbViewer;
	private InputElement selectedDatabase;

	private class InputElement {

		private DatabaseDescriptor descriptor;
		private IDatabaseServer server;

	}

	public SelectDatabaseDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		Composite composite = UI.formBody(form.getForm(), form.getToolkit());

		form.getToolkit().createLabel(composite, "Please select a database");
		dbViewer = new ListViewer(composite, SWT.SINGLE | SWT.BORDER);
		dbViewer.getList().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		dbViewer.setContentProvider(ArrayContentProvider.getInstance());
		dbViewer.setLabelProvider(new DatabaseLabelProvider());
		dbViewer.setInput(getDatabases());
		dbViewer.addSelectionChangedListener(new DatabaseSelectionListener());
	}

	private InputElement[] getDatabases() {
		java.util.List<InputElement> databases = new ArrayList<>();
		for (IDatabaseServer server : App.getDatabaseServers()) {
			if (server.isRunning()) {
				for (DatabaseDescriptor descriptor : server
						.getDatabaseDescriptors()) {
					InputElement element = new InputElement();
					element.descriptor = descriptor;
					element.server = server;
					databases.add(element);
				}
			}
		}
		return databases.toArray(new InputElement[databases.size()]);
	}

	private class DatabaseLabelProvider extends BaseLabelProvider implements
			ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return ImageType.DB_ICON.get();
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof InputElement))
				return null;

			InputElement inputElement = (InputElement) element;
			return inputElement.descriptor.getName() + " on "
					+ inputElement.server.getName(true);
		}

	}

	private class DatabaseSelectionListener implements
			ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			selectedDatabase = Viewers.getFirst(dbViewer.getSelection());
		}

	}

	public IDatabase getSelectedDatabase() {
		if (selectedDatabase != null)
			try {
				return selectedDatabase.server
						.connect(selectedDatabase.descriptor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return null;
	}
}

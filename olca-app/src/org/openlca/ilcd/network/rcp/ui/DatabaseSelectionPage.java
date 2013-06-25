package org.openlca.ilcd.network.rcp.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.database.IDatabase;
import org.openlca.core.resources.ImageType;

/**
 * The wizard page for the selection of a database.
 * 
 * @author Michael Srocka
 * 
 */
public class DatabaseSelectionPage extends WizardPage {

	private IDatabase selectedDatabase;

	public DatabaseSelectionPage() {
		super("DatabaseSelectionPage");
		setTitle("Database Selection");
		setDescription("Please select a database for the import.");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		layout.marginBottom = 10;
		layout.marginTop = 10;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		Table table = new Table(container, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gridData);

		TableViewer databaseViewer = new TableViewer(table);
		databaseViewer.setContentProvider(new ArrayContentProvider());
		databaseViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String text = null;
				if (element instanceof IDatabase) {
					text = ((IDatabase) element).getName();
				}
				return text;
			}

			@Override
			public Image getImage(Object element) {
				return ImageType.DB_ICON.get();
			}
		});
		databaseViewer.setInput(getDatabases());

		databaseViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = event.getSelection();
						if (selection == null || selection.isEmpty()) {
							setPageComplete(false);
						} else {
							Object element = ((IStructuredSelection) selection)
									.getFirstElement();
							if (element instanceof IDatabase) {
								selectedDatabase = (IDatabase) element;
								setPageComplete(true);
							}
						}
					}
				});

	}

	private IDatabase[] getDatabases() {
		IDatabase[] databases = null;
		Navigator navigator = Navigator.getInstance();
		if (navigator != null && navigator.getRoot() != null) {
			databases = navigator.getRoot().collectDatabases();
		}
		return databases != null ? databases : new IDatabase[0];
	}

	public IDatabase getSelectedDatabase() {
		return selectedDatabase;
	}

}

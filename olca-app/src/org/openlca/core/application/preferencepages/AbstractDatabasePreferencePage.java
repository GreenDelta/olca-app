package org.openlca.core.application.preferencepages;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.ui.Question;

/**
 * Abstract preference page for preferences specific for each database. Prepares
 * a database selection combo
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class AbstractDatabasePreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

	private IDatabase database;
	private IDatabase[] databases;
	private Combo dbCombo;
	private boolean isDirty = false;

	public AbstractDatabasePreferencePage() {
		super();
	}

	public AbstractDatabasePreferencePage(final String title) {
		super(title);
	}

	public AbstractDatabasePreferencePage(final String title,
			final ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(final Composite parent) {
		// create composite for database combo
		final Composite databaseComposite = new Composite(parent, SWT.NONE);
		databaseComposite.setLayout(new GridLayout(2, false));
		databaseComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		new Label(databaseComposite, SWT.NONE).setText(Messages.SelectDatabase);
		// create combo viewer for selecting a database
		dbCombo = new Combo(databaseComposite, SWT.READ_ONLY);
		dbCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// add database selection listener
		dbCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no default selection action
			}

			@Override
			public void widgetSelected(final SelectionEvent event) {
				database = databases[dbCombo.getSelectionIndex()];
				// if something has changes
				if (isDirty) {
					if (Question.ask(Messages.Save_Resource,
							Messages.Common_SaveChangesQuestion)) {
						// if yes was selected, save
						save();
					}
				}

				onDatabaseSelection(database);
			}
		});

		final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		if (navigator != null) {
			final NavigationRoot root = navigator.getRoot();
			if (root != null) {
				databases = root.collectDatabases();
				final String[] dbNames = new String[databases.length];

				for (int i = 0; i < databases.length; i++) {
					dbNames[i] = databases[i].getName();
				}

				dbCombo.setItems(dbNames);
			}
		}

		return databaseComposite;
	}

	/**
	 * Invoked when a database was selected
	 * 
	 * @param selectedDatabase
	 *            The selected database
	 */
	protected abstract void onDatabaseSelection(final IDatabase selectedDatabase);

	/**
	 * Saves the changes to the database
	 */
	protected abstract void save();

	/**
	 * Getter of the dirty state
	 * 
	 * @return The dirty state of this page
	 */
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * Setter of the isDirty value
	 * 
	 * @param isDirty
	 *            Indicates the dirty state of this page
	 */
	public void setDirty(final boolean isDirty) {
		this.isDirty = isDirty;
	}
}

/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.ui.UIFactory;

/**
 * Abstract model component wizard page
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelWizardPage extends WizardPage {

	/**
	 * The category id of the model component to be created
	 */
	private String categoryId;

	/**
	 * The database
	 */
	private IDatabase database;

	/**
	 * A {@link Text} widget for the description-field of this actor
	 */
	private Text descriptionText;

	/**
	 * Error message that will be shown if the name-field of this actor is empty
	 */
	private String EMPTY_NAME_ERROR = Messages.Common_PleaseEnterName;

	/**
	 * A {@link Text} widget for the name-field of this source
	 */
	private Text nameText;

	/**
	 * Creates a new wizard page
	 * 
	 * @param pageName
	 *            The name of the page
	 */
	protected ModelWizardPage(final String pageName) {
		super(pageName);
	}

	/**
	 * Checks if one of the necessary fields is empty. If so, an error message
	 * will be shown
	 */
	protected void checkInput() {
		setErrorMessage(null);
		if (nameText.getText().length() == 0) {
			setErrorMessage(EMPTY_NAME_ERROR);
		}
		setPageComplete(getErrorMessage() == null);
	}

	/**
	 * Creates additional contents
	 * 
	 * @param container
	 *            The parent composite
	 */
	protected abstract void createContents(Composite container);

	/**
	 * Getter of the category id
	 * 
	 * @return The category id of the model component to be created
	 */
	protected String getCategoryId() {
		return categoryId;
	}

	/**
	 * Getter of the entered description text (For subclassing purpose)
	 * 
	 * @return The description text
	 */
	protected final String getComponentDescription() {
		return descriptionText != null ? descriptionText.getText() : "";
	}

	/**
	 * Getter of the entered name text (For subclassing purpose)
	 * 
	 * @return The component name
	 */
	protected final String getComponentName() {
		return nameText != null ? nameText.getText() : "";
	}

	/**
	 * Returns the model component and possibly other relevant objects
	 * 
	 * @return A list of objects to be stored in the database
	 */
	protected abstract Object[] getData();

	/**
	 * Getter of the database
	 * 
	 * @return The database
	 */
	protected IDatabase getDatabase() {
		return database;
	}

	/**
	 * Initializes the modify listeners for the necessary fields.
	 */
	protected void initModifyListeners() {
		nameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				checkInput();
			}

		});
	}

	/**
	 * Setter of the category id
	 * 
	 * @param categoryId
	 *            The category id of the model component to be created
	 */
	public void setCategoryId(final String categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public final void createControl(final Composite parent) {
		setErrorMessage(EMPTY_NAME_ERROR);

		// create body
		final Composite container = UIFactory.createContainer(parent);
		setControl(container);

		// create name text
		nameText = UIFactory.createTextWithLabel(container, Messages.Name,
				false);

		// create description text
		descriptionText = UIFactory.createTextWithLabel(container,
				Messages.Description, true);

		// create contents
		createContents(container);

		// init listeners
		initModifyListeners();
	}

	@Override
	public void dispose() {
		super.dispose();
		EMPTY_NAME_ERROR = null;
		nameText = null;
		descriptionText = null;
	}

	/**
	 * Setter of the database
	 * 
	 * @param database
	 *            The database
	 */
	public void setDatabase(final IDatabase database) {
		this.database = database;
	}

}

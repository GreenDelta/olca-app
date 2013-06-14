/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action opens an input dialog for renaming a specific category
 * 
 * @author Sebastian Greve
 * 
 */
public class RenameCategoryAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The id of the action
	 */
	public static final String ID = "org.openlca.core.application.NavigationView.RenameCategoryAction";

	/**
	 * The category to be renamed
	 */
	private final Category category;

	/**
	 * The database to access the category on the data provider
	 */
	private final IDatabase database;

	/**
	 * Creates a new instance
	 * 
	 * @param category
	 *            The category to be renamed
	 * @param database
	 *            The database to access the category on the data provider
	 */
	public RenameCategoryAction(final Category category,
			final IDatabase database) {
		this.category = category;
		this.database = database;
		setId(ID);
		setText(Messages.NavigationView_RenameCategoryText);
		setImageDescriptor(ImageType.CHANGE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.CHANGE_ICON_DISABLED
				.getDescriptor());
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public void task() {
		// open dialog for new name
		final InputDialog dialog = new InputDialog(UI.shell(),
				Messages.NavigationView_RenameCategoryText,
				Messages.NavigationView_RenameCategoryDialogText,
				category.getName(), null);
		final int rc = dialog.open();
		if (rc == Window.OK) {
			// set new name
			category.setName(dialog.getValue());
			try {
				// update category
				database.update(category);
			} catch (final Exception e) {
				log.error("Update category failed", e);
			}
		}
	}

}

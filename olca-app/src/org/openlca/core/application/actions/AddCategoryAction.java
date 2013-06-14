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

import java.util.UUID;

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
 * This action creates a new category and appends it to the specified parent
 * category
 */
public class AddCategoryAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String ID = "org.openlca.core.application.NavigationView.AddCategoryAction";
	private final IDatabase database;
	private final Category parent;

	public AddCategoryAction(final Category parent, final IDatabase database) {
		setId(ID);
		setText(Messages.NavigationView_AddCategoryText);
		setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED.getDescriptor());
		this.parent = parent;
		this.database = database;
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public void task() {
		String categroyName = getDialogValue();
		if (categroyName == null)
			return;
		createCategory(categroyName);
	}

	private String getDialogValue() {
		InputDialog dialog = new InputDialog(UI.shell(),
				Messages.NavigationView_NewCategoryDialogTitle,
				Messages.NavigationView_NewCategoryDialogText,
				Messages.NavigationView_NewCategoryDialogDefault, null);
		int rc = dialog.open();
		if (rc == Window.OK)
			return dialog.getValue();
		return null;
	}

	private void createCategory(String categroyName) {
		Category category = new Category();
		category.setName(categroyName);
		category.setId(UUID.randomUUID().toString());
		category.setParentCategory(parent);
		category.setModelType(parent.getModelType());
		parent.add(category);
		try {
			database.createDao(Category.class).update(parent);
		} catch (final Exception e) {
			log.error("Cannot update database.", e);
		}
	}

}

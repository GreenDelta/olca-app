/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.Error;
import org.openlca.app.UI;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename a category via the navigation tree.
 */
public class RenameCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private INavigationElement<?> element;

	public RenameCategoryAction() {
		setText(Messages.NavigationView_RenameCategoryText);
		setImageDescriptor(ImageType.CHANGE_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof CategoryElement))
			return false;
		CategoryElement e = (CategoryElement) element;
		category = e.getContent();
		this.element = element;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		InputDialog dialog = new InputDialog(UI.shell(),
				Messages.NavigationView_RenameCategoryText,
				Messages.NavigationView_RenameCategoryDialogText,
				category.getName(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (newName == null || newName.trim().isEmpty()) {
			Error.showBox("The category name cannot be emty");
			return;
		}
		doUpdate(newName);
	}

	private void doUpdate(String newName) {
		try {
			category.setName(newName.trim());
			Database.get().createDao(Category.class).update(category);
			Navigator.refresh(element);
		} catch (final Exception e) {
			log.error("Update category failed", e);
		}
	}
}

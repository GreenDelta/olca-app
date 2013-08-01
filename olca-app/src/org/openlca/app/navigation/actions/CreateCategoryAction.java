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
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a new category and appends it to the specified parent
 * category
 */
public class CreateCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category parent;
	private ModelType modelType;
	private INavigationElement<?> parentElement;

	public CreateCategoryAction() {
		setText(Messages.NavigationView_AddCategoryText);
		setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (element instanceof ModelTypeElement) {
			ModelType type = (ModelType) element.getContent();
			this.parent = null;
			this.modelType = type;
			this.parentElement = element;
			return true;
		}
		if (element instanceof CategoryElement) {
			Category category = (Category) element.getContent();
			parent = category;
			modelType = category.getModelType();
			this.parentElement = element;
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (modelType == null)
			return;
		Category category = createCategory();
		if (category == null)
			return;
		try {
			tryInsert(category);
			Navigator.refresh(parentElement);
		} catch (Exception e) {
			log.error("failed to save category", e);
		}
	}

	private void tryInsert(Category category) {
		BaseDao<Category> dao = Database.get().createDao(Category.class);
		if (parent == null)
			dao.insert(category);
		else {
			category.setParentCategory(parent);
			parent.add(category);
			dao.update(parent);
		}
	}

	private Category createCategory() {
		String name = getDialogValue();
		if (name == null || name.trim().isEmpty())
			return null;
		name = name.trim();
		Category category = new Category();
		category.setName(name);
		category.setRefId(UUID.randomUUID().toString());
		category.setModelType(modelType);
		return category;
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

}

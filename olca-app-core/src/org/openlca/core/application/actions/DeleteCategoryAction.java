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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a specified category
 * 
 * @author Sebastian Greve
 * 
 */
public class DeleteCategoryAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The ID of the action
	 */
	public static final String ID = "org.openlca.core.application.NavigationView.DeleteCategoryAction"; //$NON-NLS-1$

	/**
	 * The categories that should be deleted
	 */
	private final Category[] categories;

	/**
	 * The databases to access the categories on the data provider
	 */
	private final IDatabase[] databases;

	/**
	 * The text of the action
	 */
	public final String TEXT = Messages.NavigationView_RemoveCategoryText;

	/**
	 * Creates a new instance
	 * 
	 * @param category
	 *            The category to be deleted
	 * @param database
	 *            The database to access the category on the data provider
	 */
	public DeleteCategoryAction(final Category category,
			final IDatabase database) {
		this.categories = new Category[] { category };
		this.databases = new IDatabase[] { database };
		setId(ID);
		setText(TEXT);
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	/**
	 * Creates a new instance
	 * 
	 * @param categories
	 *            The categories to delete
	 * @param databases
	 *            The databases to access the categories on the data provider
	 */
	public DeleteCategoryAction(final Category[] categories,
			final IDatabase[] databases) {
		this.categories = categories;
		this.databases = databases;
		setId(ID);
		setText(TEXT);
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	@Override
	protected String getTaskName() {
		return categories.length == 1 ? NLS
				.bind(org.openlca.core.application.Messages.DeleteCategoryAction_TaskName,
						categories[0].getName())
				: org.openlca.core.application.Messages.Common_Delete;
	}

	@Override
	public void task() {
		for (int i = 0; i < categories.length; i++) {
			final Category category = categories[i];
			final IDatabase database = databases[i];
			try {
				// load descriptors for category
				final Map<String, Object> properties = new HashMap<>();
				properties.put("categoryId", category.getId());
				final Object[] result = database
						.selectDescriptors(
								Class.forName(category.getComponentClass()),
								properties);
				if (result instanceof IModelComponent[]) {
					for (final IModelComponent comp : (IModelComponent[]) result) {
						// delete found components
						database.delete(comp);
					}
				}
				// remove category from parent
				final Category parent = category.getParentCategory();
				category.setParentCategory(null);
				// delete category
				database.delete(category);
				parent.remove(category);
				// update parent
				database.update(parent);
			} catch (final Exception e) {
				log.error("Task failed", e);
			}
		}
	}
}

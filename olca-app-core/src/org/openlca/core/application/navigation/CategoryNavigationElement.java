/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link AbstractNavigationElement} for categories in the
 * navigator
 * 
 * @author Sebastian Greve
 * 
 */
public class CategoryNavigationElement extends AbstractNavigationElement {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private INavigationElement parent;

	public CategoryNavigationElement(INavigationElement parent,
			Category category) {
		super();
		this.category = category;
		this.parent = parent;
	}

	@Override
	protected void refresh() {
		List<INavigationElement> elements = new ArrayList<>();
		for (Category child : category.getChildCategories()) {
			INavigationElement element = new CategoryNavigationElement(this,
					child);
			elements.add(element);
		}
		IModelComponent[] descriptors = new IModelComponent[0];
		try {
			IDatabase database = getDatabase();
			if (database != null) {
				Map<String, Object> properties = new HashMap<>();
				properties.put("categoryId", category.getId());
				Object[] result = database
						.selectDescriptors(
								Class.forName(category.getComponentClass()),
								properties);
				if (result instanceof IModelComponent[]) {
					descriptors = (IModelComponent[]) result;
				}
			}
		} catch (Exception e1) {
			log.error("Loading descriptors failed", e1);
		}
		for (IModelComponent modelComponent : descriptors) {
			INavigationElement element = new ModelNavigationElement(this,
					modelComponent);
			elements.add(element);
		}
		synchronize(elements);
	}

	/**
	 * Checks if the element can be deleted
	 * 
	 * @return true if the element can be deleted, false otherwise
	 */
	public boolean canBeDeleted() {
		boolean canBeDeleted = true;
		if (category.getComponentClass().equals(category.getId())) {
			canBeDeleted = false;
		} else {
			int i = 0;
			while (canBeDeleted && i < getChildren(false).length) {
				if (getChildren(false)[i] instanceof ModelNavigationElement) {
					canBeDeleted = false;
				} else if (getChildren(false)[i] instanceof CategoryNavigationElement
						&& !((CategoryNavigationElement) getChildren(false)[i])
								.canBeDeleted()) {
					canBeDeleted = false;
				} else {
					i++;
				}
			}
		}
		return canBeDeleted;
	}

	@Override
	public Object getData() {
		return category;
	}

	@Override
	public INavigationElement getParent() {
		return parent;
	}

	@Override
	public String toString() {
		String str = "CategoryNavigationElement [ category = ";
		if (category == null)
			str += "null ]";
		else
			str += category.getName() + "]";
		return str;
	}

}

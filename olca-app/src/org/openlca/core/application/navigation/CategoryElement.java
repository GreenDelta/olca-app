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
import java.util.Collections;
import java.util.List;

import org.openlca.core.application.db.Database;
import org.openlca.core.database.IRootEntityDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Represents categories in the navigation tree.
 */
public class CategoryElement implements INavigationElement {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private INavigationElement parent;

	public CategoryElement(INavigationElement parent, Category category) {
		this.category = category;
		this.parent = parent;
	}

	/**
	 * Returns true if the category is empty: means it has no sub categories or
	 * models inside.
	 */
	public boolean canBeDeleted() {
		// TODO:
		return false;
	}

	@Override
	public List<INavigationElement> getChildren() {
		if (category == null)
			return Collections.emptyList();
		List<INavigationElement> list = new ArrayList<>();
		for (Category child : category.getChildCategories()) {
			list.add(new CategoryElement(this, child));
		}
		addModelElements(list);
		return list;
	}

	private void addModelElements(List<INavigationElement> list) {
		try {
			IRootEntityDao<?> dao = Database.createRootDao(category
					.getModelType());
			if (dao == null)
				return;
			Optional<Category> optional = Optional.fromNullable(category);
			for (BaseDescriptor descriptor : dao.getDescriptors(optional)) {
				list.add(new ModelElement(this, descriptor));
			}
		} catch (Exception e) {
			log.error("failed to get model elements: " + category, e);
		}
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
		String str = "CategoryElement [ category = ";
		if (category == null)
			str += "null ]";
		else
			str += category.getName() + "]";
		return str;
	}

}

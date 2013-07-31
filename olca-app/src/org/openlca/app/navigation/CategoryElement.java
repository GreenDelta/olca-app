/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.core.database.CategorizedEnitityDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Represents categories in the navigation tree.
 */
public class CategoryElement extends NavigationElement<Category> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public CategoryElement(INavigationElement<?> parent, Category category) {
		super(parent, category);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		Category category = getContent();
		log.trace("add category childs for {}", category);
		if (category == null) {
			return Collections.emptyList();
		}
		List<INavigationElement<?>> list = new ArrayList<>();
		for (Category child : category.getChildCategories()) {
			list.add(new CategoryElement(this, child));
		}
		addModelElements(category, list);
		return list;
	}

	private void addModelElements(Category category,
			List<INavigationElement<?>> list) {
		try {
			CategorizedEnitityDao<?, ?> dao = Database.createRootDao(category
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

}

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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.jobs.Jobs;
import org.openlca.core.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the {@link AbstractNavigationElement} for paths on a data
 * provider
 * 
 * @author Sebastian Greve
 * 
 */
public class DatabaseNavigationElement extends AbstractNavigationElement {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * The parent element
	 */
	private final INavigationElement parent;

	/**
	 * Creates a new path navigation element
	 * 
	 * @param database
	 *            The data base of the new element
	 * @param parent
	 *            The parent element
	 */
	public DatabaseNavigationElement(final IDatabase database,
			final INavigationElement parent) {
		super();
		this.database = database;
		this.parent = parent;
	}

	@Override
	protected void refresh() {
		List<INavigationElement> elements = new ArrayList<>();
		try {
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).startJob(
					Messages.PathNavigationElement_LoadingData,
					IProgressMonitor.UNKNOWN);
			for (Category category : database.select(Category.class, "root")
					.getChildCategories()) {
				INavigationElement element = new CategoryNavigationElement(
						DatabaseNavigationElement.this, category);
				elements.add(element);
			}
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).done();
		} catch (Exception e) {
			log.error("Refresh failed", e);
		}
		synchronize(elements);
	}

	@Override
	public Object getData() {
		return database;
	}

	@Override
	public INavigationElement getParent() {
		return parent;
	}

}

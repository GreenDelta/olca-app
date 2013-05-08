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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.core.runtime.PlatformObject;
import org.openlca.core.application.App;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root navigation element of the common viewer of the applications navigator
 * 
 * @author Sebastian Greve
 * 
 */
public class NavigationRoot extends PlatformObject implements
		INavigationElement {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private List<INavigationElement> elements = new ArrayList<>();

	private boolean contains(List<INavigationElement> elements,
			INavigationElement element) {
		boolean contains = false;
		for (INavigationElement e : elements) {
			if (e.getData().equals(element.getData())) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	/**
	 * Get the databases from the navigation tree.
	 */
	public IDatabase[] collectDatabases() {
		List<IDatabase> databases = new ArrayList<>();
		for (INavigationElement element : getChildren(false)) {
			if (element instanceof DataProviderNavigationElement) {
				databases.addAll(getDatabases(element));
			}
		}
		return databases.toArray(new IDatabase[databases.size()]);
	}

	private List<IDatabase> getDatabases(INavigationElement dataProviderElement) {
		List<IDatabase> databases = new ArrayList<>();
		for (INavigationElement element : dataProviderElement
				.getChildren(false)) {
			IDatabase database = element.getDatabase();
			if (database != null) {
				databases.add(database);
			}
		}
		return databases;
	}

	/**
	 * Get the root category element for a specific class on a specific database
	 * 
	 * @param clazz
	 *            The class the root element is needed for
	 * @param database
	 *            The database
	 * @return The root category element for the specified class on the database
	 */
	public CategoryNavigationElement getCategoryRoot(
			Class<? extends IModelComponent> clazz, IDatabase database) {
		INavigationElement[] children = getChildren(false);
		CategoryNavigationElement categoryRootElement = null;
		Queue<INavigationElement> elements = new LinkedList<>();
		for (INavigationElement child : children) {
			elements.add(child);
		}
		while (categoryRootElement == null && !elements.isEmpty()) {
			INavigationElement element = elements.poll();
			if (element instanceof CategoryNavigationElement) {
				if (((Category) element.getData()).getId().equals(
						clazz.getCanonicalName())
						&& database.equals(element.getDatabase())) {
					categoryRootElement = (CategoryNavigationElement) element;
				}
			}
			for (INavigationElement subElement : element.getChildren(false)) {
				elements.add(subElement);
			}
		}
		return categoryRootElement;
	}

	@Override
	public INavigationElement[] getChildren(boolean refresh) {
		if (refresh) {
			log.info("Initialize navigation");
			List<INavigationElement> elements = new ArrayList<>();
			try {
				for (IDatabaseServer provider : App.getDatabaseServers()) {
					INavigationElement element = new DataProviderNavigationElement(
							provider);
					elements.add(element);
				}
			} catch (Exception e) {
				log.error("Reading children from db failed", e);
			}
			List<INavigationElement> toRemove = new ArrayList<>();
			for (INavigationElement element : this.elements) {
				if (!contains(elements, element)) {
					toRemove.add(element);
				}
			}

			for (INavigationElement element : toRemove) {
				this.elements.remove(element);
			}

			for (INavigationElement element : elements) {
				if (!contains(this.elements, element)) {
					this.elements.add(element);
				}
			}
		}
		return this.elements.toArray(new INavigationElement[elements.size()]);
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public IDatabase getDatabase() {
		return null;
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return elements.size() == 0;
	}

}

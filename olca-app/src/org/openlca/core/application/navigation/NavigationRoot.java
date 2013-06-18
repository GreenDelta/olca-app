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

import org.eclipse.core.runtime.PlatformObject;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.DatabaseList;
import org.openlca.core.application.db.DerbyConfiguration;
import org.openlca.core.application.db.MySQLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root element of the navigation tree: shows the database configurations.
 */
public class NavigationRoot extends PlatformObject implements
		INavigationElement {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public List<INavigationElement> getChildren() {
		log.trace("initialize navigation");
		DatabaseList list = Database.getConfigurations();
		List<INavigationElement> elements = new ArrayList<>();
		for (DerbyConfiguration config : list.getLocalDatabases())
			elements.add(new DatabaseElement(config));
		for (MySQLConfiguration config : list.getRemoteDatabases())
			elements.add(new DatabaseElement(config));
		return elements;
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

	@Override
	public Object getData() {
		return null;
	}

}

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

import org.openlca.core.database.IDatabase;

/**
 * Interface for navigation elements in the applications navigator common viewer
 * 
 * @author Sebastian Greve
 * 
 */
public interface INavigationElement {

	/**
	 * Getter of the children navigation element
	 * 
	 * @param refresh
	 *            Indicates whether the common viewer should be updated or not
	 * @return The children navigation element
	 */
	INavigationElement[] getChildren(boolean refresh);

	/**
	 * Get the data the element is representing
	 * 
	 * @return The data hold by the navigation element
	 */
	Object getData();

	/**
	 * Getter of the database of the navigation element. If this element itself
	 * has no database, than the parents will be searched.
	 * 
	 * @return First occurence of a database object in the data field of the
	 *         navigation element parents
	 */
	IDatabase getDatabase();

	/**
	 * Getter of the parent navigation element
	 * 
	 * @return The parent navigation element
	 */
	INavigationElement getParent();

	/**
	 * Returns true if the navigation element does not contain other navigation
	 * elements.
	 */
	boolean isEmpty();

}

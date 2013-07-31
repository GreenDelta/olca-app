/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import org.openlca.app.Messages;

/**
 * Enumeration of GraphLayoutTypes
 * 
 * @author Sebastian Greve
 * 
 */
public enum GraphLayoutType {

	/**
	 * Minimal tree layout
	 */
	MinimalTreeLayout(Messages.Systems_GraphLayoutType_MinimalTree),

	/**
	 * Tree layout
	 */
	TreeLayout(Messages.Systems_GraphLayoutType_Tree);

	/**
	 * The display name
	 */
	private String displayName;

	/**
	 * Creates a new instance
	 * 
	 * @param displayName
	 *            The display name
	 */
	private GraphLayoutType(final String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Getter of the display name
	 * 
	 * @return The display name
	 */
	public String getDisplayName() {
		return displayName;
	}

}

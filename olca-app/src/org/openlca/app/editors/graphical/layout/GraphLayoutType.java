/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.layout;

import org.openlca.app.Messages;


public enum GraphLayoutType {

	MINIMAL_TREE_LAYOUT(Messages.Systems_GraphLayoutType_MinimalTree),

	TREE_LAYOUT(Messages.Systems_GraphLayoutType_Tree);

	private String displayName;

	private GraphLayoutType(final String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

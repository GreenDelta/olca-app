/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.navigation.filters;

import org.openlca.core.model.ProcessType;

/**
 * Filter for filtering unit processes
 * 
 * @author Sebastian Greve
 * 
 */
public final class UnitProcessFilter extends ProcessTypeFilter {

	/**
	 * Default constructor
	 */
	public UnitProcessFilter() {
		super(ProcessType.UnitProcess);
	}

}

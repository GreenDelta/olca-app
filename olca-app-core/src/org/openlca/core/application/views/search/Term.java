/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.search;

/**
 * Interface for a search term
 * 
 * @author Sebastian Greve
 * 
 */
interface Term {

	/**
	 * Checks if the given phrase fulfills the conditions of the term
	 * 
	 * @param phrase
	 *            The phrase to be checked
	 * @return True if the phrase fulfills the conditions of the term, false
	 *         otherwise
	 */
	boolean fulfills(String phrase);

}

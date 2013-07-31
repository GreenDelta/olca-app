/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.search;

/**
 * Implementation of term for the negation operation
 * 
 * @author Sebastian Greve
 * 
 */
class NotTerm implements Term {

	/**
	 * The term to be negated
	 */
	public Term term;

	/**
	 * Creates a new instance
	 * 
	 * @param term
	 *            The term to be negated
	 */
	public NotTerm(final Term term) {
		this.term = term;
	}

	@Override
	public boolean fulfills(final String phrase) {
		return !term.fulfills(phrase);
	}

}

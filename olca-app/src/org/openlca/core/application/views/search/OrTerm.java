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
 * Implementation of term for realising an OR operation
 * 
 * @author Sebastian Greve
 * 
 */
class OrTerm implements Term {

	/**
	 * Left operand of the OR operation
	 */
	private final Term term1;

	/**
	 * Right operand of the OR operation
	 */
	private final Term term2;

	/**
	 * Creates a new instance
	 * 
	 * @param term1
	 *            Left operand of the OR operation
	 * @param term2
	 *            Right operand of the OR operation
	 */
	public OrTerm(final Term term1, final Term term2) {
		this.term1 = term1;
		this.term2 = term2;
	}

	@Override
	public boolean fulfills(final String phrase) {
		return term1.fulfills(phrase) || term2.fulfills(phrase);
	}

}

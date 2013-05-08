/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.wizards;

/**
 * This class represents problems while operating on objects
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class Problem {

	/**
	 * Error means that the operation cannot be processed
	 */
	public static final int ERROR = 0;

	/**
	 * Warning means that the operation can be processed, but other operations
	 * have to be done, too
	 */
	public static final int WARNING = 1;

	/**
	 * The cause of the problem
	 */
	private String cause;

	/**
	 * The displayed text
	 */
	private final String text;

	/**
	 * Type of problem
	 */
	private final int type;

	/**
	 * Creates a new instance
	 * 
	 * @param type
	 *            The type of problem
	 * @param text
	 *            The displayed text
	 */
	protected Problem(final int type, final String text) {
		this.type = type;
		this.text = text;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param type
	 *            The type of problem
	 * @param text
	 *            The displayed text
	 * @param cause
	 *            The cause of the problem
	 */
	protected Problem(final int type, final String text, final String cause) {
		this.type = type;
		this.text = text;
		this.cause = cause;
	}

	/**
	 * Getter of the cause
	 * 
	 * @return The cause of the problem
	 */
	public String getCause() {
		return cause;
	}

	/**
	 * Getter of the text
	 * 
	 * @return The displayed text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Getter of the type
	 * 
	 * @return The type of problem
	 */
	public Integer getType() {
		return type;
	}

	/**
	 * Solves the problem by additional operations (mostly if the problem is a
	 * warning). E.g. An object should be deleted but is referenced. Than the
	 * solution would be to disband the reference, before deleting the object
	 */
	public abstract void solve();

}

/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app;

import org.eclipse.swt.widgets.Control;

/**
 * Implementors of this type are informed when the content of the given UI
 * component changed.
 * 
 * 
 * @author Michael Srocka
 * 
 */
public interface IContentChangedListener {

	/**
	 * Informs the listener that the content of the component changed.
	 * 
	 * @param source
	 *            the control which content changed
	 * @param content
	 *            the new content
	 */
	void contentChanged(Control source, Object content);

}

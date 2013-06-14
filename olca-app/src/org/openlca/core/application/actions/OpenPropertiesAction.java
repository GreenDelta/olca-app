/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.action.Action;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.DataProviderPropertiesWindow;
import org.openlca.core.database.IDatabaseServer;

/**
 * Action to open the data provider properties window for the selected data
 * provider
 * 
 * @author Sebastian Greve
 * 
 */
public class OpenPropertiesAction extends Action {

	/**
	 * The data provider
	 */
	private final IDatabaseServer dataProvider;

	/**
	 * Creates a new instance
	 * 
	 * @param dataProvider
	 *            The data provider
	 */
	public OpenPropertiesAction(final IDatabaseServer dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public String getText() {
		return Messages.Properties;
	}

	@Override
	public void run() {
		new DataProviderPropertiesWindow(dataProvider).open();
	}

}

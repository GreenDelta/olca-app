/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.systems;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ProductSystem;

/**
 * Opens the matrix export dialog.
 */
public class SystemExportAction extends Action {

	private ProductSystem productSystem;
	private IDatabase database;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.EXCEL_ICON.getDescriptor();
	}

	public void setExportData(ProductSystem productSystem, IDatabase database) {
		this.productSystem = productSystem;
		this.database = database;
	}

	@Override
	public String getText() {
		return "Excel export";
	}

	@Override
	public void run() {
		new SystemExportDialog(productSystem, database).open();
	}
}

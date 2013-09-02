/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.outline;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.app.db.Database;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemTreeEditPart extends AbstractTreeEditPart {

	@Override
	public ProductSystem getModel() {
		return (ProductSystem) super.getModel();
	}

	@Override
	protected List<ProcessDescriptor> getModelChildren() {
		List<ProcessDescriptor> descriptors = Database.getCache()
				.getProcessDescriptors(getModel().getProcesses());
		Collections.sort(descriptors, new Comparator<ProcessDescriptor>() {

			@Override
			public int compare(ProcessDescriptor d1, ProcessDescriptor d2) {
				return d1.getName().toLowerCase()
						.compareTo(d2.getName().toLowerCase());
			}

		});
		return descriptors;
	}

}

/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.outline;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

/**
 * The TreeEditPart for a {@link ProductSystemNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemTreeEditPart extends AppAbstractTreeEditPart {

	@Override
	protected List<Process> getModelChildren() {
		final ProductSystem productSystem = (ProductSystem) getModel();
		final Process[] nodes = productSystem.getProcesses();
		final List<Process> nodeList = new ArrayList<>();
		for (final Process p : nodes) {
			nodeList.add(p);
		}
		Collections.sort(nodeList, new Comparator<Process>() {

			@Override
			public int compare(final Process o1, final Process o2) {
				return o1.getName().toLowerCase()
						.compareTo(o2.getName().toLowerCase());
			}

		});
		return nodeList;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

	}
}

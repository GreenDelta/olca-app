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

import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessPart;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;

/**
 * The TreeEditPart for a {@link ProcessNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessTreeEditPart extends AppAbstractTreeEditPart {

	/**
	 * The product system node
	 */
	private final ProductSystemNode node;

	/**
	 * Creates a new instance
	 * 
	 * @param node
	 *            The product system node
	 */
	public ProcessTreeEditPart(final ProductSystemNode node) {
		this.node = node;
	}

	@Override
	protected String getText() {
		final Process process = (Process) getModel();
		String text = process.getName();
		if (process.getLocation() != null) {
			text += " [" + process.getLocation().getName() + "]";
		}
		if (process.getProcessType() == ProcessType.LCI_Result) {
			text += " _S";
		}
		return text;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {

	}

	@Override
	public void setSelected(final int value) {
		super.setSelected(value);
		for (final Object o : node.getPart().getChildren()) {
			if (o instanceof ProcessPart) {
				final ProcessPart p = (ProcessPart) o;
				if (((ProcessNode) p.getModel()).getProcess().getId()
						.equals(((Process) getModel()).getId())) {
					p.setSelected(value);
					break;
				}
			}
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.process;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property sheet for displaying uncertainty and allocation values of an
 * exchange. See {@link ExchangePropertySource} for the sheet entries.
 * 
 * @author Sebastian Greve, Michael Srocka
 * 
 */
public class ExchangePropertiesPage extends PropertySheetPage implements
		PropertyChangeListener, IPropertySourceProvider {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Exchange actualSelection;
	private Process process;

	public ExchangePropertiesPage(Process process) {
		this.process = process;
		process.addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		if (process != null)
			process.removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void init(final IPageSite pageSite) {
		super.init(pageSite);
		setPropertySourceProvider(this);
	}

	@Override
	public IPropertySource getPropertySource(Object object) {
		log.trace("Create property source for {}", object);
		if (object instanceof Exchange)
			return new ExchangePropertySource((Exchange) object, process);
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("distributionType")
				|| evt.getPropertyName().equals("formula")
				|| evt.getNewValue() instanceof AllocationMethod) {
			log.trace("Refresh property sheet", evt.getPropertyName());
			refresh();
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (actualSelection != null)
			actualSelection.removePropertyChangeListener(this);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			if (sel.getFirstElement() instanceof Exchange) {
				actualSelection = (Exchange) sel.getFirstElement();
				actualSelection.addPropertyChangeListener(this);
			}
		}
		super.selectionChanged(part, selection);
	}

}
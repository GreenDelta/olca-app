/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Abstract class for parameterizable model editor with a property sheet page
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ParameterizableModelEditorWithPropertyPage extends
		ParameterizableModelEditor {

	/**
	 * Creates a new instance
	 * 
	 * @param componentTypeName
	 *            The type of the component
	 */
	public ParameterizableModelEditorWithPropertyPage(
			final String componentTypeName) {
		super(componentTypeName);
	}

	/**
	 * Getter of the property sheet page for the editor
	 * 
	 * @return The property sheet page for the editor
	 */
	protected abstract IPropertySheetPage getPropertySheetPage();

	@Override
	protected void pageChange(final int newPageIndex) {
		super.pageChange(newPageIndex);
		final Object page = getActivePageInstance();
		final IViewPart viewPart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(IPageLayout.ID_PROP_SHEET);
		// if property sheet
		if (viewPart != null && viewPart instanceof PropertySheet) {
			final PropertySheetPage propertyPage = (PropertySheetPage) ((PropertySheet) viewPart)
					.getCurrentPage();
			if (page != null && page instanceof PropertyProviderPage) {
				// update with actual selection
				propertyPage.selectionChanged(this,
						((PropertyProviderPage) page).getSelection());
			} else {
				// set empty selection
				propertyPage.selectionChanged(this, new StructuredSelection());
			}

		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class adapter) {
		Object adapt = null;
		if (adapter == IPropertySheetPage.class) {
			adapt = getPropertySheetPage();
		} else {
			adapt = super.getAdapter(adapter);
		}
		return adapt;
	}
}

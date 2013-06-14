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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.views.properties.PropertySheet;

/**
 * Abstract class for handling property page selection providing
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class PropertyProviderPage extends ModelEditorPage {

	/**
	 * The actual selection
	 */
	private ISelection selection;

	/**
	 * Creates a new property provider page
	 * 
	 * @param editor
	 *            The model editor
	 * @param id
	 *            The id of the page
	 * @param title
	 *            The title of the page
	 */
	public PropertyProviderPage(final ModelEditor editor, final String id,
			final String title) {
		super(editor, id, title);
	}

	/**
	 * Getter of the selection-field
	 * 
	 * @return The actual selection
	 */
	public final ISelection getSelection() {
		return selection;
	}

	/**
	 * Setter of the selection-field. Also changes the selection in the property
	 * sheet
	 * 
	 * @param editorPart
	 *            The editor part
	 * @param selection
	 *            The new selection
	 */
	public final void setSelection(final IEditorPart editorPart,
			final ISelection selection) {
		this.selection = selection;
		final IViewPart view = editorPart.getSite().getPage()
				.findView(IPageLayout.ID_PROP_SHEET);
		if (view != null && view instanceof PropertySheet) {
			((PropertySheet) view).selectionChanged(editorPart, selection);
		}
	}

}

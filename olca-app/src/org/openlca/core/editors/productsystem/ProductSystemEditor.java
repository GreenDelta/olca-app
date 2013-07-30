/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem;

import java.io.IOException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.UI;
import org.openlca.app.db.Database;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.editors.ParameterizableModelEditor;
import org.openlca.core.editors.productsystem.graphical.ProductSystemGraphEditor;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form editor for product systems
 * 
 * @author Sebastian Greve
 * 
 */
public class ProductSystemEditor extends ParameterizableModelEditor {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static String ID = "org.openlca.core.editors.productsystem.ProductSystemEditor";

	private ProductSystemGraphEditor graphEditor;
	private int graphEditorPageIndex;
	private ProductSystemInfoPage infoPage;

	public ProductSystemEditor() {
		super(Messages.Systems_FormText);
	}

	/**
	 * Asks the user if the outline should be opened.
	 * 
	 * @return True if the user answered 'Yes', false otherwise
	 */
	private boolean openOutline() {
		final PreferenceStore pfStore = ApplicationProperties
				.getPreferenceStore();
		boolean open = false;
		if (!pfStore.contains("openOutline")) {
			final MessageDialogWithToggle toggle = MessageDialogWithToggle
					.openYesNoQuestion(UI.shell(), Messages.Systems_Outline,
							Messages.Systems_OutlineQuestion,
							Messages.Systems_Remember, false, pfStore,
							"openOutline");
			open = toggle.getReturnCode() == IDialogConstants.YES_ID;
			try {
				pfStore.save();
			} catch (final IOException e) {
				log.error("Saving preference store failed", e);
			}
		} else {
			open = pfStore.getString("openOutline").equals("always");
		}
		return open;
	}

	@Override
	protected void addPages() {
		try {
			infoPage = new ProductSystemInfoPage(this);
			addPage(infoPage);
			graphEditor = new ProductSystemGraphEditor(this, Database.get(),
					(ProductSystem) getModelComponent());
			graphEditorPageIndex = addPage(graphEditor, getEditorInput());
			setPageText(graphEditorPageIndex, Messages.Systems_GraphTab);

		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
		super.addPages();
	}

	@Override
	protected ModelEditorPage[] initPages() {
		return new ModelEditorPage[0];
	}

	@Override
	protected void pageChange(final int newPageIndex) {
		super.pageChange(newPageIndex);
		final ContentOutline outline = (ContentOutline) PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView(IPageLayout.ID_OUTLINE);
		if (outline == null && newPageIndex == graphEditorPageIndex) {
			if (openOutline()) {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().showView(IPageLayout.ID_OUTLINE);
				} catch (final PartInitException e) {
					log.error("Changing page failed", e);
				}
			}

		} else if (newPageIndex == graphEditorPageIndex && outline != null) {
			// set outline of graph editor
			outline.partBroughtToTop(graphEditor);
		} else if (outline != null) {
			if (getActivePageInstance() != null) {
				// set outline null (other active page instances does not have
				// an outline
				outline.partBroughtToTop(getActivePageInstance());
			}
		}
	}

	@Override
	public void dispose() {
		if (PlatformUI.getWorkbench() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage() != null) {
			final ContentOutline outline = (ContentOutline) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.findView(IPageLayout.ID_OUTLINE);
			if (outline != null) {
				// set outline of graph editor
				final IWorkbenchPart activePart = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.getActivePart();
				if (activePart != null && activePart instanceof IEditorPart) {
					outline.partBroughtToTop(activePart);
				} else {
					outline.partBroughtToTop(infoPage);
				}
			}
		}
		graphEditor.dispose();
		graphEditor = null;
		super.dispose();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class adapter) {
		Object adapt = null;
		if (adapter == IContentOutlinePage.class) {
			if (getActivePage() == graphEditorPageIndex) {
				// return the outline of the graph editor
				adapt = graphEditor.getAdapter(adapter);
			}
		} else {
			adapt = super.getAdapter(adapter);
		}
		return adapt;
	}

	/**
	 * Getter of the product system graph editor
	 * 
	 * @return The product system graph editor
	 */
	public ProductSystemGraphEditor getGraphEditor() {
		return graphEditor;
	}

	@Override
	public Object getSelectedPage() {
		Object page = super.getSelectedPage();
		if (page == null && getActivePage() == graphEditorPageIndex) {
			page = graphEditor;
		}
		return page;
	}

}

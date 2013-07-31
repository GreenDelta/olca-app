/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.plugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.openlca.app.search.SearchView;

/**
 * The openLCA perspective
 * 
 * @author Sebastian Greve
 * 
 */
public class Perspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(final IPageLayout layout) {
		final String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		// navigator and search view
		final IFolderLayout folder = layout.createFolder("Others",
				IPageLayout.LEFT, 0.31f, editorArea);
		folder.addView("org.openlca.core.application.navigator");
		folder.addView(SearchView.ID);

		// property sheet place holder
		final IFolderLayout folder2 = layout.createFolder("Bottom",
				IPageLayout.BOTTOM, 0.8f, editorArea);
		folder2.addView(IPageLayout.ID_PROP_SHEET);

		// outline place holder
		layout.addPlaceholder(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.8f,
				editorArea);

		// get wizard shortcuts
		final IExtensionRegistry extensionRegistry = Platform
				.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry
				.getConfigurationElementsFor("org.openlca.core.application.wizardShortcuts");
		for (final IConfigurationElement element : elements) {
			layout.addNewWizardShortcut(element.getAttribute("wizardID"));
		}

	}
}

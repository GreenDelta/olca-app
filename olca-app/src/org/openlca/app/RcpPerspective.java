package org.openlca.app;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.openlca.app.navigation.Navigator;

/**
 * The openLCA perspective
 */
public class RcpPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(final IPageLayout layout) {
		final String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);

		// navigator and search view
		final IFolderLayout folder = layout.createFolder("Others",
				IPageLayout.LEFT, 0.31f, editorArea);
		folder.addView(Navigator.ID);
		// folder.addView(SearchView.ID);

		// property sheet place holder
		final IFolderLayout folder2 = layout.createFolder("Bottom",
				IPageLayout.BOTTOM, 0.8f, editorArea);
		folder2.addView(IPageLayout.ID_PROP_SHEET);

		// outline place holder
		layout.addPlaceholder(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.8f,
				editorArea);

	}
}

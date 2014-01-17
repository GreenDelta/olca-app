package org.openlca.app;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.openlca.app.navigation.Navigator;

public class RcpPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(final IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		IFolderLayout naviLayout = layout.createFolder("Others",
				IPageLayout.LEFT, 0.31f, editorArea);
		naviLayout.addView(Navigator.ID);
		// outline place holder
		layout.addPlaceholder(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.8f,
				editorArea);
	}
}

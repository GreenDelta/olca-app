package org.openlca.app.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IViewLayout;
import org.openlca.app.navigation.Navigator;

public class RcpPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(final IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		IFolderLayout naviFolder = layout.createFolder("Navigation",
				IPageLayout.LEFT, 0.31f, editorArea);
		naviFolder.addView(Navigator.ID);
		IViewLayout naviLayout = layout.getViewLayout(Navigator.ID);
		naviLayout.setCloseable(false);
		naviLayout.setMoveable(false);
		// outline place holder
		layout.addPlaceholder(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.8f,
				editorArea);
	}
}

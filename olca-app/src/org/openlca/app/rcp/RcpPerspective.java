package org.openlca.app.rcp;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.openlca.app.navigation.Navigator;

public class RcpPerspective implements IPerspectiveFactory {

	public final static String ID = "perspectives.standard";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		var editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		var naviFolder = layout.createFolder(
				"Navigation", IPageLayout.LEFT, 0.31f, editorArea);
		naviFolder.addView(Navigator.ID);
		var naviLayout = layout.getViewLayout(Navigator.ID);
		naviLayout.setCloseable(false);
		naviLayout.setMoveable(false);
		// outline place holder
		layout.addPlaceholder(
				IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.8f, editorArea);
	}
}

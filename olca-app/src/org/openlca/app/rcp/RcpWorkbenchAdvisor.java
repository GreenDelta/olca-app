package org.openlca.app.rcp;

import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.openlca.app.logging.Console;
import org.openlca.app.logging.LoggerPreference;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RcpWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "perspectives.standard";

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new RcpWindowAdvisor(configurer);
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		configurer.setSaveAndRestore(false);
		if (LoggerPreference.getShowConsole()) {
			Console.show();
		}
		changeWorkbenchImages();
		disableExtensions();
	}

	/**
	 * There is no official method/extension point for replacing the shared
	 * workbench images. Thus, we access the internal WorkbenchImages class here
	 * and replace some images with our own here.
	 */
	private void changeWorkbenchImages() {
		try {
			WorkbenchImages.declareImage(
					IWorkbenchGraphicConstants.IMG_WIZBAN_IMPORT_WIZ,
					RcpActivator.getImageDescriptor("icons/wizard/import.png"),
					true);
			WorkbenchImages.declareImage(
					IWorkbenchGraphicConstants.IMG_WIZBAN_EXPORT_WIZ,
					RcpActivator.getImageDescriptor("icons/wizard/export.png"),
					true);
			WorkbenchImages.declareImage(
					ISharedImages.IMG_OBJ_FOLDER,
					RcpActivator.getImageDescriptor("icons/folder_open.png"),
					true);
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to patch workbench images", e);
		}
	}

	private void disableExtensions() {

		// disable unwanted preference pages
		// a warning "Invalid preference category path: ..." may show up in the
		// logs because of categories of preference pages that do not exist;
		var prefs = PlatformUI.getWorkbench().getPreferenceManager();
		var nodes = prefs.getElements(PreferenceManager.PRE_ORDER);
		for (var node : nodes) {
			var id = node.getId();
			if (id == null) {
				continue;
			}
			if (node.getId().contains("org.eclipse")) {
				prefs.remove(node);
				node.disposeResources();
			}
		}
	}

}

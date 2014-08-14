package org.openlca.app.rcp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When there is no initial workbench.xmi file in the workspace the search field
 * in the tool bar is shown on the left side (seems to be an error in Eclipse
 * 4.4). In order to fix this we copy a default workbench.xmi into the workspace
 * folder.
 */
class WorkbenchLayout {

	private Logger log = LoggerFactory.getLogger(getClass());

	private WorkbenchLayout() {
	}

	public static void initialize(File workspace) {
		new WorkbenchLayout().apply(workspace);
	}

	private void apply(File workspace) {
		if (!workspace.exists() || !workspace.isDirectory())
			return;
		try {
			log.trace("initialize workbench layout in {}", workspace);
			String sep = File.separator;
			String path = ".metadata" + sep + ".plugins" + sep
					+ "org.eclipse.e4.workbench";
			File dir = new File(workspace, path);
			if (!dir.exists())
				dir.mkdirs();
			File workbenchXmi = new File(dir, "workbench.xmi");
			if (workbenchXmi.exists()) {
				log.trace("{} exists", workbenchXmi);
				return;
			}
			createXmi(workbenchXmi);
		} catch (Exception e) {
			log.error("failed to initialize workbench layout", e);
		}
	}

	private void createXmi(File workbenchXmi) throws Exception {
		log.trace("create {}", workbenchXmi);
		try (InputStream in = getClass().getResourceAsStream(
				"workbench.xmi");
				FileOutputStream out = new FileOutputStream(workbenchXmi)) {
			IOUtils.copy(in, out);
		}
	}
}

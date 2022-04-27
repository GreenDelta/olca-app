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
 * folder when it does not exist. Also, we it is possible to reset the workbench
 * layout by replacing the file in the workspace.
 */
public final class WindowLayout {

	private WindowLayout() {
	}

	static void initialize() {
		Logger log = LoggerFactory.getLogger(WindowLayout.class);
		try {
			File dir = getDir();
			File xmi = new File(dir, "workbench.xmi");
			if (!xmi.exists()) {
				copyXmi();
				return;
			}
			File resetFlag = new File(dir, "_reset_layout");
			if (!resetFlag.exists()) {
				log.trace("window layout file exists {}", xmi);
				return;
			}

			log.info("reset window layout");
			copyXmi();
			if (!resetFlag.delete()) {
				log.warn("failed to delete reset flag; "
						+ "adding shutdown hook: " + resetFlag);
				resetFlag.deleteOnExit();
			}
		} catch (Exception e) {
			log.error("failed to initialize workbench layout", e);
		}
	}

	public static void reset() {
		// note that we cannot simply overwrite the XMI file here
		// as it is automatically saved again with the current
		// layout when the application is closed. instead we
		// create an empty file here that indicates a reset for
		// the next start
		Logger log = LoggerFactory.getLogger(WindowLayout.class);
		File flag = new File(getDir(), "_reset_layout");
		log.info("set reset flag for window layout: {}", flag);
		try {
			flag.createNewFile();
		} catch (Exception e) {
			log.error("failed to create reset flag " + flag, e);
		}
	}

	private static void copyXmi() {
		Logger log = LoggerFactory.getLogger(WindowLayout.class);
		File xmi = new File(getDir(), "workbench.xmi");
		log.info("copy window layout file to {}", xmi);
		try (InputStream in = WindowLayout.class.getResourceAsStream(
				"workbench.xmi");
				FileOutputStream out = new FileOutputStream(xmi)) {
			IOUtils.copy(in, out);
		} catch (Exception e) {
			log.error("failed to copy layout file " + xmi, e);
		}
	}

	private static File getDir() {
		String sep = File.separator;
		String path = ".metadata" + sep + ".plugins" + sep
				+ "org.eclipse.e4.workbench";
		File dir = new File(Workspace.root(), path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
}

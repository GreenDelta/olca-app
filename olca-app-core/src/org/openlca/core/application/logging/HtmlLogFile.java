package org.openlca.core.application.logging;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.WriterAppender;
import org.eclipse.core.runtime.Platform;

/**
 * The configuration of the HTML log file.
 * 
 * @author Michael Srocka
 * 
 */
class HtmlLogFile {

	private HtmlLogFile() {
	}

	public static void create(Logger logger) {
		try {
			File logFile = createLogFile();
			WriterAppender appender = createAppender(logFile);
			logger.addAppender(appender);
		} catch (Exception e) {
			logger.log(Level.ERROR, e.getMessage(), e);
		}
	}

	private static File createLogFile() {
		File workspaceDir = Platform.getLocation().toFile();
		if (!workspaceDir.exists()) {
			workspaceDir.mkdirs();
		}
		return new File(workspaceDir, "log.html");
	}

	private static WriterAppender createAppender(File logFile)
			throws IOException {
		HTMLLayout layout = new HTMLLayout();
		RollingFileAppender app = new RollingFileAppender(layout,
				logFile.getAbsolutePath(), true);
		app.setMaxFileSize("3MB");
		app.setMaxBackupIndex(3);
		return app;
	}

}

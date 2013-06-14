package org.openlca.io.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.slf4j.Logger;

public class UIHelper {

	public static void reportException(Logger log, String message,
			Throwable throwable) {
		log.error(message, throwable);
		Status s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message,
				throwable);
		StatusManager.getManager().handle(s, StatusManager.BLOCK);
	}

	public static void logException(Logger log, String message,
			Throwable throwable) {
		log.error(message, throwable);
		Status s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message,
				throwable);
		StatusManager.getManager().handle(s, StatusManager.SHOW);
	}

	public static void logError(Logger log, String message) {
		log.error(message);
		Status s = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
		StatusManager.getManager().handle(s, StatusManager.SHOW);
	}

}

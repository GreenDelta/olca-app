package org.openlca.app.util;

import java.awt.Desktop.Action;
import java.net.URI;

import org.openlca.commons.Strings;
import org.slf4j.LoggerFactory;

public class Desktop {

	public static void browse(String uri) {
		if (Strings.isBlank(uri))
			return;
		try {
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
				if (desktop.isSupported(Action.BROWSE)) {
					desktop.browse(new URI(uri));
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(Desktop.class)
				.error("Browse URI failed", e);
		}
	}

}

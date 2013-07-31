package org.openlca.app.util;

import java.awt.Desktop.Action;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Desktop {

	private static Logger log = LoggerFactory.getLogger(Desktop.class);

	public static void browse(String uri) {
		try {
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
				if (desktop.isSupported(Action.BROWSE)) {
					desktop.browse(new URI(uri));
				}
			}
		} catch (Exception e) {
			log.error("Browse URI failed", e);
		}
	}
	
}

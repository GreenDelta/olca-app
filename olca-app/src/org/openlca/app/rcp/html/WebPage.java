package org.openlca.app.rcp.html;

import javafx.scene.web.WebEngine;

public interface WebPage {
	/**
	 * Get the URL to the HTML page.
	 */
	String getUrl();

	/**
	 * Is executed when the page is ready in the browser.
	 */
	void onLoaded(WebEngine webkit);
}

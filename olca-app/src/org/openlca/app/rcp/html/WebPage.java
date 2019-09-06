package org.openlca.app.rcp.html;

import javafx.scene.web.WebEngine;

/**
 * We will remove the JavaFX webviews. Do not implement this interface anymore.
 */
@Deprecated
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

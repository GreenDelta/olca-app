package org.openlca.ui;

import org.openlca.ui.html.IHtmlResource;

/**
 * Interface for an HTML page used in the SWT browser widget.
 */
public interface HtmlPage {

	/**
	 * Get the html-resource.
	 */
	IHtmlResource getResource();

	/**
	 * Is executed when the page is ready in the browser.
	 */
	void onLoaded();

}

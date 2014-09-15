package org.openlca.app.rcp.html;

/**
 * Interface for an HTML page used in the SWT browser widget.
 */
public interface HtmlPage {

	/**
	 * Get the URL to the HTML page.
	 */
	String getUrl();

	/**
	 * Is executed when the page is ready in the browser.
	 */
	void onLoaded();

}

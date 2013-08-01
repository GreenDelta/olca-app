package org.openlca.app.components;

import java.awt.Desktop;
import java.net.URI;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A notification pop-up with a link which can be opened with the respective
 * desktop application (e.g. a web-browser or MS Excel).
 * 
 * @author Michael Srocka
 * 
 */
public class NotificationLink extends AbstractNotificationPopup {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * the message of the pop-up
	 */
	private final String message;

	/**
	 * the title of the pop-up
	 */
	private final String title;

	/**
	 * the linked URL (e.g. a file, folder, or web-resource)
	 */
	private final URI uri;

	/**
	 * Creates the pop-up with a linked notification.
	 * 
	 * @param title
	 *            the title of the pop-up
	 * @param message
	 *            the message of the pop-up
	 * @param uri
	 *            the linked URI (e.g. a file, folder, or web-resource)
	 */
	public NotificationLink(final String title, final String message,
			final URI uri) {
		super(Display.getCurrent());
		this.title = title;
		this.message = message;
		this.uri = uri;
	}

	@Override
	protected void createContentArea(final Composite composite) {
		composite.setLayout(new GridLayout(1, true));

		// create the link
		final Link link = new Link(composite, SWT.WRAP);
		link.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		link.setText("<a>" + message + "</a>");
		link.setBackground(composite.getBackground());

		link.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {

				// try to open the URL with a desktop-app.
				try {
					if (Desktop.isDesktopSupported()) {
						final Desktop desktop = Desktop.getDesktop();
						desktop.browse(uri);
					}
				} catch (final Exception e) {
					log.error("Open URL with desktop-app failed", e);
				}
			}
		});
	}

	@Override
	protected String getPopupShellTitle() {
		return title;
	}

	// @Override
	// protected Image getPopupShellImage(int maximumHeight) {
	// // Use createResource to use a shared Image instance of the
	// ImageDescriptor
	// return (Image) Activator.getImageDescriptor("/icons/information.png")
	// .createResource(Display.getDefault());
	// }

}

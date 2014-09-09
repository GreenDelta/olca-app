package org.openlca.app.util;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pop-up for error messages.
 */
public class ErrorPopup extends Popup {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ErrorPopup(String message) {
		this(null, message);
	}

	public ErrorPopup(String title, String message) {
		super(title, message);
		defaultTitle(Messages.ErrorPopupTitle);
		popupShellImage(ImageType.ERROR_ICON);
	}

	@Override
	protected void makeLink(Composite composite) {
		Hyperlink hyperlink = new Hyperlink(composite, SWT.NONE);
		hyperlink.setText(Messages.ErrorPopupMessage);
		hyperlink.setForeground(composite.getDisplay().getSystemColor(
				SWT.COLOR_BLUE));
		hyperlink.addHyperlinkListener(new LinkActivation());
	}

	private class LinkActivation extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent evt) {
			try {
				File workspaceDir = Platform.getLocation().toFile();
				File logFile = new File(workspaceDir, "log.html");
				Desktop.browse(logFile.toURI().toString());
			} catch (Exception e) {
				log.error("Writing file failed", e);
			}
		}
	}

	public static void show(final String message) {
		show(null, message);
	}

	public static void show(final String title, final String message) {
		new ErrorPopup(title, message).show();
	}

}

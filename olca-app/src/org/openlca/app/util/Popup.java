package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.opal.notifier.Notifier;
import org.eclipse.nebula.widgets.opal.notifier.NotifierColorsFactory.NotifierTheme;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.rcp.images.Icon;

/**
 * A pop-up for messages. For specialized versions look at inheriting classes
 * like {@link InformationPopup}.
 */
class Popup {

	private String message;
	private String title;
	private Icon icon = Icon.INFO;

	public Popup() {

	}

	private Popup(String message) {
		this.message = message;
	}

	Popup(String title, String message) {
		this(message);
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	Popup defaultTitle(String t) {
		this.title = t;
		return this;
	}

	Popup popupShellImage(Icon icon) {
		this.icon = icon;
		return this;
	}

	public Image getPopupShellImage() {
		return icon.get();
	}

	/**
	 * Override this to add something below the label.
	 */
	protected void makeLink(Composite composite) {
	}

	void show() {
		UIJob job = new UIJob("Open popup") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Display display = getDisplay();
				if (display == null || display.isDisposed())
					return Status.CANCEL_STATUS;
				Notifier.notify(icon.get(), title, message,
						NotifierTheme.GRAY_THEME);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}

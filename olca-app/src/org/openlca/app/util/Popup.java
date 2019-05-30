package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.rcp.images.Icon;
import org.openlca.util.Strings;

/**
 * A pop-up for messages. For spezialized versions look at inheriting classes
 * like {@link InformationPopup}.
 */
class Popup {

	private String message;
	private String title;
	private Icon icon = Icon.INFO;
	private String defaultTitle = "Popup";

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
		this.defaultTitle = t;
		return this;
	}

	Popup popupShellImage(Icon icon) {
		this.icon = icon;
		return this;
	}

	private String getDefaultTitle() {
		return defaultTitle;
	}

	public Image getPopupShellImage() {
		return icon.get();
	}

	private void createLabel(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setText(Strings.cut(message, 500));
		label.setBackground(composite.getBackground());
	}

	/**
	 * Override this to add something below the label.
	 * 
	 * @param composite
	 */
	protected void makeLink(Composite composite) {
	}

	/**
	 * Default implementation just calls makeLink.
	 */
	private void makeLink(PopupImpl popupImpl, Composite composite) {
		makeLink(composite);
	}

	void show() {
		UIJob job = new UIJob("Open popup") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Display display = getDisplay();
				if (display == null || display.isDisposed())
					return Status.CANCEL_STATUS;
				new PopupImpl(display).open();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private class PopupImpl extends AbstractNotificationPopup {

		private PopupImpl(Display display) {
			super(display);
		}

		@Override
		protected String getPopupShellTitle() {
			if (title != null)
				return title;
			return getDefaultTitle();
		}

		@Override
		protected void initializeBounds() {
			// Georg: Workaround for a sizing bug in the extended implementation
			super.initializeBounds();
			Point currentSize = getShell().getSize();
			Point currentLoc = getShell().getLocation();

			Point newSize = getShell().computeSize(400, SWT.DEFAULT);

			int widthDiff = newSize.x - currentSize.x;
			int heightDiff = newSize.y - currentSize.y;
			Point newLoc = new Point(currentLoc.x - widthDiff, //
					currentLoc.y - heightDiff);

			getShell().setLocation(newLoc);
			getShell().setSize(newSize);
		}

		@Override
		protected Image getPopupShellImage(int maximumHeight) {
			return Popup.this.getPopupShellImage();
		}

		@Override
		protected void createContentArea(Composite composite) {
			composite.setLayout(new GridLayout(1, true));
			createLabel(composite);
			makeLink(this, composite);
		}

	}
}

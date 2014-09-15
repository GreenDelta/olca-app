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
import org.openlca.app.rcp.ImageType;
import org.openlca.util.Strings;

/**
 * A pop-up for messages. For spezialized versions look at inheriting classes
 * like {@link InformationPopup}.
 */
public class Popup {

	private String message;
	private String title;
	private ImageType imageType = ImageType.INFO_ICON;
	private String defaultTitle = "Popup";

	public Popup() {

	}

	public Popup(String message) {
		this.message = message;
	}

	public Popup(String title, String message) {
		this(message);
		this.title = title;
	}

	public Popup message(String m) {
		this.message = m;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public Popup title(String t) {
		this.title = t;
		return this;
	}

	public Popup defaultTitle(String t) {
		this.defaultTitle = t;
		return this;
	}

	public Popup popupShellImage(ImageType imageType) {
		this.imageType = imageType;
		return this;
	}

	protected String getDefaultTitle() {
		return defaultTitle;
	}

	public Image getPopupShellImage() {
		return imageType.get();
	}

	protected void createLabel(Composite composite) {
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
	 * 
	 * @param popupImpl
	 * @param composite
	 */
	protected void makeLink(PopupImpl popupImpl, Composite composite) {
		makeLink(composite);
	}

	public void show() {
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

	public class PopupImpl extends AbstractNotificationPopup {

		public PopupImpl(Display display) {
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

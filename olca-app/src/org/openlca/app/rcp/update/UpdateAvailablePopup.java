package org.openlca.app.rcp.update;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.InformationPopup;
import org.openlca.util.Strings;

public class UpdateAvailablePopup extends InformationPopup {

	private VersionInfo versionInfo;
	private Job updateCallback;

	public UpdateAvailablePopup(VersionInfo versionInfo, Job updateCallback2) {
		super("New openLCA version available", "openLCA "
				+ versionInfo.getVersion() + " available. "
				+ "Update now (your data will be migrated):");
		this.versionInfo = versionInfo;
		this.updateCallback = updateCallback2;
	}

	@Override
	protected void createLabel(Composite composite) {
		Composite subComp = new Composite(composite, SWT.NONE);
		subComp.setLayout(new GridLayout(2, false));
		subComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		new Label(subComp, SWT.NONE).setImage(ImageType.LOGO_64_32.get());

		Label label = new Label(subComp, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		label.setText(Strings.cut(getMessage(), 500));

		label.setBackground(composite.getBackground());
	}

	@Override
	protected void makeLink(PopupImpl popupImpl, Composite composite) {
		Hyperlink hyperlink = new Hyperlink(composite, SWT.NONE);
		hyperlink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		hyperlink.setBackgroundImage(ImageType.LOGO_64_32.get());
		hyperlink.setText("Download update to openLCA "
				+ versionInfo.getVersion());
		hyperlink.setForeground(composite.getDisplay().getSystemColor(
				SWT.COLOR_BLUE));
		hyperlink.addHyperlinkListener(new LinkActivation(popupImpl));
	}

	private class LinkActivation extends HyperlinkAdapter {
		private PopupImpl popupImpl;

		public LinkActivation(PopupImpl popupImpl) {
			this.popupImpl = popupImpl;
		}

		@Override
		public void linkActivated(HyperlinkEvent evt) {
			updateCallback.schedule();
			this.popupImpl.close();
		}
	}
}

package org.openlca.app.db;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.database.DatabaseDescriptor;
import org.openlca.core.database.IDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dialog for controlling the updates of a database. The dialog gets a set of
 * database descriptors which need an update. If the user clicks on 'ok' the
 * updates are done and the descriptors are set as 'up-to-date'.
 */
public class DatabaseUpdateDialog extends Dialog {

	private FormToolkit toolkit;
	private Label headerLabel;
	private DatabaseDescriptor[] descriptors;
	private ImageHyperlink[] labels;
	private IDatabaseServer server;
	private ProgressBar progressBar;
	private ScrolledComposite scroll;

	public DatabaseUpdateDialog(Shell parentShell, IDatabaseServer server,
			List<DatabaseDescriptor> descriptors) {
		super(parentShell);
		setBlockOnOpen(true);
		this.toolkit = new FormToolkit(parentShell.getDisplay());
		this.server = server;
		this.descriptors = descriptors.toArray(new DatabaseDescriptor[0]);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.DatabaseUpdate_Title);
		toolkit.adapt(parent);
		Composite container = (Composite) super.createDialogArea(parent);
		toolkit.adapt(container);
		UI.gridLayout(container, 1);
		createHeaderLabel(container);
		createContent(container);
		createProgressBar(container);
		return container;
	}

	private void createHeaderLabel(Composite container) {
		headerLabel = UI.formLabel(container, toolkit,
				Messages.DatabaseUpdate_Message);
		final Font bold = UI.boldFont(headerLabel);
		headerLabel.setFont(bold);
		headerLabel.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				bold.dispose();
			}
		});
	}

	private void createProgressBar(Composite container) {
		progressBar = new ProgressBar(container, SWT.SMOOTH);
		UI.gridData(progressBar, true, false);
		progressBar.setMaximum(descriptors.length * 2);
		progressBar.setSelection(0);
		progressBar.setVisible(false);
	}

	private void createContent(Composite container) {
		if (descriptors == null)
			return;
		scroll = new ScrolledComposite(container, SWT.V_SCROLL | SWT.BORDER);
		toolkit.adapt(scroll);
		UI.gridData(scroll, true, true);
		UI.gridLayout(scroll, 1);
		Composite client = toolkit.createComposite(scroll);
		scroll.setContent(client);
		UI.gridData(client, true, true);
		UI.gridLayout(client, 1);
		createLinks(client);
		scroll.setMinSize(client.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
	}

	private void createLinks(Composite client) {
		labels = new ImageHyperlink[descriptors.length];
		for (int i = 0; i < descriptors.length; i++) {
			DatabaseDescriptor d = descriptors[i];
			ImageHyperlink link = new ImageHyperlink(client, SWT.TOP);
			UI.gridData(link, true, false);
			link.setImage(ImageType.NOT_OK_ICON.get());
			link.setText(d.getName() + " (" + d.getVersion() + ")");
			toolkit.adapt(link);
			labels[i] = link;
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 400);
	}

	@Override
	public boolean close() {
		if (toolkit != null)
			toolkit.dispose();
		return super.close();
	}

	@Override
	protected void okPressed() {
		progressBar.setVisible(true);
		headerLabel.setText(Messages.DatabaseUpdate_Running);
		int done = 0;
		for (int i = 0; i < descriptors.length; i++) {
			scroll.showControl(labels[i]);
			labels[i].getParent().update();
			progressBar.setSelection(++done);
			UpdateJob job = new UpdateJob(descriptors[i], server);
			getShell().getDisplay().syncExec(job);
			labels[i].setText(descriptors[i].getName() + " ("
					+ descriptors[i].getVersion() + ")");
			progressBar.setSelection(++done);
			if (job.isFailed())
				labels[i].setImage(ImageType.ERROR_ICON.get());
			else
				labels[i].setImage(ImageType.OK_CHECK_ICON.get());
			labels[i].update();
		}
		super.okPressed();
	}

	private class UpdateJob implements Runnable {

		private DatabaseDescriptor descriptor;
		private IDatabaseServer server;
		private boolean failed = false;

		public UpdateJob(DatabaseDescriptor descriptor, IDatabaseServer server) {
			this.descriptor = descriptor;
			this.server = server;
		}

		@Override
		public void run() {
			try {
				server.update(descriptor);
				Thread.sleep(500);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("Update failed for " + descriptor, e);
				failed = true;
			}
		}

		public boolean isFailed() {
			return failed;
		}

	}

}

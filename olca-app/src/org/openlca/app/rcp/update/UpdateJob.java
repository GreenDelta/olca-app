package org.openlca.app.rcp.update;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.rcp.PlatformUtils;
import org.openlca.app.util.UI;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateJob extends UIJob {

	private static final Logger log = LoggerFactory.getLogger(UpdateJob.class);
	private Updater updater;
	private VersionInfo newVersion;

	public UpdateJob(Updater updater, VersionInfo versionInfo) {
		super("openLCA update");
		this.updater = updater;
		this.newVersion = versionInfo;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			new ContinueUpdateNonModalDialog(UI.shell()).open();
		} catch (Exception e) {
			log.error("Executing external updater failed ", e);
			return Status.CANCEL_STATUS;
		}
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	protected void executeInstaller() throws Exception {

		String updaterJar = findUpdaterJar();
		File javaExecutable = PlatformUtils.findJavaExecutable();

		log.debug("Found java executable for update: {}", javaExecutable);
		if (javaExecutable == null) {
			throw new RuntimeException("Cannot execute installer: "
					+ "Java executable not found.");
		}

		log.debug("Executing installer...");
		Process proc = updater.runInNewJVM(javaExecutable, updaterJar);
		log.debug("Executed {}", proc);
	}

	protected String findUpdaterJar() throws IOException {
		log.debug("Creating updater jar");
		Bundle updaterBundle = Platform.getBundle("org.openlca.updater");
		if (updaterBundle == null) {
			throw new RuntimeException(
					"Updater bundle not found - cannot update.");
		}
		final String bundleJarPath = PlatformUtils
				.getBundleJarPath("org.openlca.updater");
		return bundleJarPath;
	}

	/**
	 * Class to create a non-modal dialog modeled after MessageDialog's
	 * internals.
	 */
	public class ContinueUpdateNonModalDialog extends MessageDialog {

		public ContinueUpdateNonModalDialog(Shell shell) {
			super(shell, "Upgrade to openLCA version "
					+ newVersion.getVersion(), null,
					"Ready to install new version, "
							+ "close application and start now?",
					MessageDialog.QUESTION_WITH_CANCEL, new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.CANCEL_LABEL }, 0);
			setBlockOnOpen(false);
			int style = SWT.NONE;
			style &= SWT.SHEET;
			setShellStyle(shell.getStyle() | style);
		}

		@Override
		protected void buttonPressed(final int buttonId) {
			super.buttonPressed(buttonId);
			new Job("Update launcher") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						if (buttonId == MessageDialog.OK) {
							executeInstaller();

							log.info("Quitting for update...");
							quitWorkbenchFromJob();
						} else {
							log.info("Aborting update because user declined.");
						}
					} catch (Exception e) {
						log.error("Launching updater failed");
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}

				protected void quitWorkbenchFromJob() {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							PlatformUI.getWorkbench().close();
						}
					});
				}
			}.schedule();

		}

	}
}

package org.openlca.app.rcp.update;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.openlca.app.Messages;
import org.openlca.app.rcp.PlatformUtils;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.update.Updater.UnzipRequest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class UpdateCheckAndPrepareJob extends Job {

	public static final String DEFAULT_SERVER_ROOT = "http://h2051742.stratoserver.net/updatesite/";
	public static final String UPDATE_SITE_PROPERTY = "org.openlca.core.updatesite";

	private static final String WINDOWS_BINARY = "openLCA.exe";
	private static final String LINUX_BINARY = "openLCA";
	private static final String MAC_BINARY = "Contents/MacOS/openLCA";

	private static final String[] DEFAULT_PATHS_TO_DELETE_BEFORE_UPDATE = {
			// openLCA.exe should be first - that way the updater pops it up
			// directly if openLCA is still running when the updater executes.
			WINDOWS_BINARY, LINUX_BINARY, MAC_BINARY, "plugins", "p2",
			"configuration", "features" };

	private List<String> pathsToDeleteBeforeUpdate = new ArrayList<>(
			Arrays.asList(DEFAULT_PATHS_TO_DELETE_BEFORE_UPDATE));

	private boolean forceCheck;

	public UpdateCheckAndPrepareJob() {
		super(Messages.OpenLCAUpdateCheck);
	}

	public boolean isForceCheck() {
		return forceCheck;
	}

	/**
	 * Check even if a check isn't due.
	 * 
	 * @param forceCheck
	 */
	public void setForceCheck(boolean forceCheck) {
		this.forceCheck = forceCheck;
	}

	private static final Logger log = LoggerFactory
			.getLogger(UpdateCheckAndPrepareJob.class);

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// if (!FeatureFlag.AUTOMATIC_UPDATES.isEnabled()) {
		// log.trace("Automatic updates not enabled.");
		// return Status.OK_STATUS;
		// }
		if (!isSelfUpdatingInstallation()) {
			log.debug("No update checks, not a self-updating installation");
			return Status.OK_STATUS;
		}
		if (!UpdatePreference.isUpdateEnabled()) {
			log.debug("No update checks: Update checks disabled in prefs");
			return Status.OK_STATUS;
		}
		monitor.beginTask("Check for update", 1);
		try {
			if (isForceCheck() || updateCheckDue()) {
				updateCheck(monitor);
				storeNewUpdateCheckTS();
			}
			monitor.done();
		} catch (InterruptedException ie) {
			log.debug("interrupted... canceling");
			return Status.CANCEL_STATUS;
		} catch (IntermittentConnectionFailure icf) {
			log.debug("Update check failed", icf);
			String causeMsg = icf.getCause() != null ? (" and " + icf
					.getCause().getMessage()) : "";
			log.info("Update check failed because "
					+ "of connection problems: {} {}", icf.getMessage(),
					causeMsg);
		} catch (Exception e) {
			log.error("Update check failed", e);
			return Status.CANCEL_STATUS;
		}
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	public List<String> getPathsToDelete() {
		return pathsToDeleteBeforeUpdate;
	}

	private boolean isSelfUpdatingInstallation() {
		boolean retval = false;
		try {
			File installRoot = PlatformUtils.getInstallRoot();
			if (installRoot == null)
				return false;
			if (PlatformUtils.isWindows()
					&& !(new File(installRoot, "singleuserinstall.mrk")
							.exists())) {
				// windows multi-user install not self-updating
				retval = false;
			} else {
				try {
					File accessRightsTestFile = File.createTempFile(
							"testaccessrights", "tmp", installRoot);
					try (FileOutputStream fileOutputStream = new FileOutputStream(
							accessRightsTestFile)) {
						fileOutputStream.write(5);
						retval = true;
					} finally {
						try {
							accessRightsTestFile.delete();
						} catch (Exception e) {
							// ignore
						}
					}
				} catch (Exception e) {
					log.info("Not a self-updating installation: "
							+ "No write access to install root {}: {}",
							installRoot, e.getMessage());

				}
			}
		} catch (Exception e) {
			log.debug("Self updating installation check failed", e);
		}
		log.debug("Self updating platform check result: {}", retval);
		return retval;
	}

	private void updateCheck(final IProgressMonitor monitor) throws Exception,
			IntermittentConnectionFailure {
		monitor.beginTask("Update check", 3);
		UpdateCheckService updateCheckService = new UpdateCheckService();
		final VersionInfo versionInfo = updateCheckService
				.loadNewestVersionFromServer(getUpdateSite());
		monitor.worked(2);

		if (versionInfo != null
				&& RcpActivator.getDefault().getBundle() != null) {
			Version currVersion = RcpActivator.getDefault().getBundle()
					.getVersion();
			Version newestVersion;
			try {
				newestVersion = new Version(versionInfo.getVersion());
			} catch (Exception e) {
				throw new RuntimeException("Incompatible version string in "
						+ versionInfo, e);
			}

			if (newestVersion.compareTo(currVersion) > 0) {
				askUserAndPotentiallyDownloadAndPrepareUpdate(monitor,
						versionInfo);
			}
		}
		monitor.done();
	}

	protected void askUserAndPotentiallyDownloadAndPrepareUpdate(
			final IProgressMonitor monitor, final VersionInfo versionInfo) {

		Job updateCallback = new Job("Update downloader") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Download and preparation of update", 4);
				try {
					log.info("Preparing update... start download of new version in background");
					File tempFile = File.createTempFile("openLCAdownload",
							"temp");
					UpdateService updateService = new UpdateService();
					monitor.subTask("Downloading new version");
					updateService.downloadToFileWithProgress(
							versionInfo.getDownloadUrl(), tempFile);
					log.debug("Got new version");
					monitor.worked(2);
					if (!monitor.isCanceled()) {
						Updater updater = prepareUpdater(tempFile, versionInfo);
						monitor.worked(1);
						if (!monitor.isCanceled()) {
							new UpdateJob(updater, versionInfo).schedule();
						}

					} else {
						log.info("Aborting update because monitor was canceled");
					}
				} catch (Exception e) {
					log.error("Update download failed: " + e.getMessage());
				}
				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};

		log.debug("Showing update available popup");
		new UpdateAvailablePopup(versionInfo, updateCallback).show();

	}

	private String getUpdateSite() {
		String property = System.getProperty(UPDATE_SITE_PROPERTY);
		if (Strings.isNullOrEmpty(property)) {
			return DEFAULT_SERVER_ROOT;
		}
		if (!property.endsWith("/")) {
			property = property.trim() + "/";
		}
		return property.trim();
	}

	protected void storeNewUpdateCheckTS() {
		UpdatePreference
				.setLastUpdateCheckSecs(System.currentTimeMillis() / 1000);
	}

	protected boolean updateCheckDue() {
		long lastCheck = UpdatePreference.getLastUpdateCheckSecs();
		long passedTime = System.currentTimeMillis() / 1000 - lastCheck;
		if (passedTime >= UpdatePreference.getUpdateRythmSecs()) {
			String msg = "never checked before";
			if (lastCheck > 0) {
				msg = "last check " + (passedTime / 84600) + " days and "
						+ (passedTime % 84600) + " seconds ago";
			}
			log.info("Update check needed: {}", msg);
			return true;
		}
		log.debug("No update check needed, {} days and {} "
				+ "secs passed since last", passedTime / 84600,
				passedTime % 84600);
		return false;
	}

	public Updater prepareUpdater(File appZip, VersionInfo newAppVersionInfo)
			throws Exception {
		File installDir = PlatformUtils.getInstallRoot();
		log.debug("Found install dir: {}", installDir);
		if (!PlatformUtils.checkInstallPath(installDir)
		// make sure this is not an eclipse installation
		// to avoid deletion of dev (or other) environment
		) {
			throw new RuntimeException("Cannot update: "
					+ "Install root cannot be determined.");
		}

		int levelsToStripWhenUnzipping = determineLevelsToStripWhenUnzipping();

		List<String> pathsToIgnore = new ArrayList<>();
		pathsToIgnore.add("openLCA.ini");
		pathsToIgnore.add("Contents/MacOS/openLCA.ini");

		List<String> pathsToDelete = new ArrayList<>();
		pathsToDelete.addAll(getPathsToDelete());

		List<UnzipRequest> unzipRequests = new ArrayList<>();
		unzipRequests.add(new UnzipRequest(installDir,
				appZip.getAbsolutePath(), levelsToStripWhenUnzipping));

		Updater updater = new Updater(getBinaryToRun(), unzipRequests,
				pathsToDelete, pathsToIgnore);

		runPreUpdateHooks(updater, newAppVersionInfo);
		return updater;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void runPreUpdateHooks(Updater updater,
			VersionInfo newAppVersionInfo) {
		log.debug("Exec preUpdateHooks");
		BundleContext bundleContext = RcpActivator.getDefault().getBundle()
				.getBundleContext();

		ServiceReference[] hookRefs = null;
		try {
			hookRefs = bundleContext.getAllServiceReferences(
					PreUpdateHook.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// should only happen with a specific filter
			log.error("Could not get PreUpdateHook services", e);
		}

		if (hookRefs == null) {
			log.debug("No {} services found in framework", PreUpdateHook.class);
		} else {
			for (ServiceReference ref : hookRefs) {
				Object service = null;
				try {
					service = bundleContext.getService(ref);
					if (!(service instanceof PreUpdateHook)) {
						log.warn("Service reference {} "
								+ "not assignable to a PreUpdateHook", ref);
					} else {
						PreUpdateHook hook = (PreUpdateHook) service;
						hook.customizeUpdater(updater, newAppVersionInfo);
					}
				} catch (Exception e) {
					log.warn("Problem executing the pre-update hook {}, "
							+ "attempting to continue nevertheless", ref);
				} finally {
					bundleContext.ungetService(ref);
				}
			}
		}

	}

	private String getBinaryToRun() throws Exception {
		String binary;
		if (PlatformUtils.isWindows()) {
			binary = WINDOWS_BINARY;
		} else if (PlatformUtils.isMac()) {
			binary = MAC_BINARY;
		} else {
			binary = LINUX_BINARY;
		}
		return new File(PlatformUtils.getInstallRoot(), binary)
				.getAbsolutePath();
	}

	private int determineLevelsToStripWhenUnzipping() {
		if (PlatformUtils.isMac()) {
			// was different at first, now same as for windows
			return 1;
		}
		return 1;
	}

}

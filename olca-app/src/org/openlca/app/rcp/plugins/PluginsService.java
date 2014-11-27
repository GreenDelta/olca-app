package org.openlca.app.rcp.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.openlca.app.rcp.PlatformUtils;
import org.openlca.app.rcp.RcpActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;

public class PluginsService {
	private static final Logger log = LoggerFactory
			.getLogger(PluginsService.class);

	public static final String UPDATE_SITE_CORE_PROPERTY = "org.openlca.core.updatesite";
	public static final String UPDATE_SITE_PLUGIN_INSTALLER_PROPERTY = "org.openlca.plugin.installer.updatesite";
	public static final String DEFAULT_SERVER_ROOT = "http://nexus.openlca.org/updatesite";

	private static final ObjectMapper mapper = new ObjectMapper();

	public PluginListWrapper loadPluginsFromServer() throws Exception {
		try {
			Client c = createWSClient();
			WebResource r2 = c.resource(getUpdateSite() + "plugins.json");
			try (InputStream s = r2.get(InputStream.class)) {
				log.debug("Inputstream for plugins.json null? " + (s == null));
				PluginListWrapper plugins = mapper.readValue(s,
						PluginListWrapper.class);
				log.debug("Plugins: {}", plugins);
				log.info("Loaded plugins from server");
				return plugins;
			}
		} catch (ClientHandlerException che) {
			log.debug("Server connection", che);
			throw new Exception("Update Server connection failed: "
					+ che.getMessage());
		} catch (Exception e) {
			log.debug("Server plugin loading", e);
			throw new Exception("Could not load openLCA plugins: "
					+ e.getMessage());
		}
	}

	/**
	 * Finds installed plugins by searching for plugins offered by the plugin
	 * server in paths relative to this plugin's installation.
	 * <p>
	 * Assumes that this plugin is installed in the openLCA directory, either
	 * underneath <code>dropins</code> or underneath <code>plugins</code>. Then
	 * searches all jars that reside directly underneath dropins or plugins and
	 * matches their <code>symbolicName</code> with the plugins available from
	 * the plugin server. If a match is found it is analyzed for a possibility
	 * to update.
	 * 
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public PluginListWrapper getInstalledPlugins() {
		ArrayList<Bundle> allOpenlcaPluginBundles = findAllOpenlcaPluginBundles();
		log.debug("all olca plugins: {}", allOpenlcaPluginBundles);

		PluginListWrapper retval = new PluginListWrapper();
		for (Bundle b : allOpenlcaPluginBundles) {
			Plugin p = new Plugin();
			p.setSymbolicName(b.getSymbolicName());
			Version bundleVersion = b.getVersion();
			p.setInstalledVersion(//
			bundleVersion == null || bundleVersion == Version.emptyVersion //
			? null : bundleVersion.toString());
			retval.getPlugins().add(p);
		}

		return retval;
	}

	public PluginListWrapper mergePluginInfo(
			PluginListWrapper installedPlugins,
			PluginListWrapper availablePlugins, String targetOpenLcaVersion) {

		HashMap<String, Plugin> installedMap = new HashMap<>();
		for (Plugin p : installedPlugins.getPlugins()) {
			installedMap.put(p.getSymbolicName(), p);
		}

		HashMap<String, Plugin> mergedAvailPlugins = new HashMap<>();
		for (Plugin p : availablePlugins.getPlugins()) {
			if (Strings.isNullOrEmpty(p.getSymbolicName())) {
				log.debug("Ingoring plugin without symbolic name, name: {}",
						p.getName());
				continue;
			}
			if (!Strings.isNullOrEmpty(p.getMinOpenLcaVersion())) {
				if (isNewer(p.getMinOpenLcaVersion(), targetOpenLcaVersion)) {
					log.debug("Found new version of {}, "
							+ "but needs openLCA version {}. " + "Ignoring.",
							p.getSymbolicName(), p.getMinOpenLcaVersion());
					continue;
				}
			}

			Plugin alreadyContainedPlugin = mergedAvailPlugins.get(p
					.getSymbolicName());
			if (alreadyContainedPlugin == null
					|| isNewer(p.getVersion(),
							alreadyContainedPlugin.getVersion())) {
				Plugin toAdd = installedMap.get(p.getSymbolicName());
				if (toAdd == null) {
					toAdd = p;
				} else {
					toAdd.setDescription(p.getDescription());
					toAdd.setDownloadUrl(p.getDownloadUrl());
					toAdd.setName(p.getName());
					toAdd.setVersion(p.getVersion());
					toAdd.setInstallable(p.isInstallable());
					toAdd.setImage(p.getImage());
					for (Dependency dependency : p.getDependencies()) {
						Dependency dep = new Dependency();
						dep.setSymbolicName(dependency.getSymbolicName());
						dep.setVersion(dependency.getVersion());
						toAdd.getDependencies().add(dep);
					}
				}
				mergedAvailPlugins.put(toAdd.getSymbolicName(), toAdd);
			}
		}

		HashMap<String, Plugin> installedButNotAvailable = new HashMap<>(
				installedMap);
		for (Plugin p : mergedAvailPlugins.values()) {
			installedButNotAvailable.remove(p.getSymbolicName());
		}
		PluginListWrapper retval = new PluginListWrapper();
		retval.getPlugins().addAll(mergedAvailPlugins.values());
		retval.getPlugins().addAll(installedButNotAvailable.values());
		return retval;
	}

	public void downloadPlugins(PluginListWrapper plugins, File toZip)
			throws Exception {
		if (plugins.getPlugins().isEmpty())
			return;
		try {
			try (ZipOutputStream zipOut = new ZipOutputStream(
					new FileOutputStream(toZip))) {
				for (Plugin p : plugins.getPlugins()) {
					downloadPlugin(p, zipOut);
				}
			}
		} catch (Exception e) {
			log.debug("Downloading plugins failed", e);
			throw new Exception("Downloading plugins for update failed: "
					+ e.getMessage());
		}
	}

	public static PluginListWrapper retainUpdateablePlugins(
			PluginListWrapper mergedPluginInfo) {
		PluginListWrapper retval = new PluginListWrapper();
		for (Plugin p : mergedPluginInfo.getPlugins()) {
			// if it's installed and a new version is available
			if ((!Strings.isNullOrEmpty(p.getInstalledVersion()))
					&& isNewer(p.getVersion(), p.getInstalledVersion())) {
				retval.getPlugins().add(p);
			}
		}
		return retval;
	}

	/**
	 * Can be passed a list created by
	 * {@link #retainUpdateablePlugins(PluginListWrapper)} to determine the jars
	 * to delete.
	 * 
	 * @param updateablePlugins
	 * @return absolute paths
	 */
	public List<String> determinePluginFilesToDeleteBeforeUpdate(
			PluginListWrapper updateablePlugins) {
		List<String> retval = new ArrayList<>();

		for (Plugin p : updateablePlugins.getPlugins()) {
			String bundleJarPath;
			try {
				bundleJarPath = PlatformUtils.getBundleJarPath(p
						.getSymbolicName());
				if (uninstallableJarOrWarn(p.getSymbolicName(), p.getVersion(),
						bundleJarPath)) {
					retval.add(new File(bundleJarPath).getAbsolutePath());
				}
			} catch (IOException e) {
				if (!log.isDebugEnabled()) {
					log.info("Cannot uninstall {} correctly, "
							+ "archive cannot be determined: {}", p,
							e.getMessage());
				} else {
					log.debug("Cannot uninstall {} correctly, "
							+ "archive cannot be determined", p, e);
				}
			}
		}
		return retval;
	}

	private void checkDependencies(Plugin p, PluginListWrapper availablePlugins)
			throws Exception {
		List<Plugin> alreadyChecked = new ArrayList<>();
		Queue<Plugin> toCheck = new LinkedList<>();
		toCheck.add(p);
		while (!toCheck.isEmpty()) {
			Plugin current = toCheck.poll();
			alreadyChecked.add(current);
			for (Dependency d : current.getDependencies()) {
				Plugin plugin = availablePlugins.get(d);
				if (plugin == null)
					throw new Exception("Plugin " + current.getSymbolicName()
							+ " is missing required dependency "
							+ d.getSymbolicName() + " (" + d.getVersion() + ")");
				if (!alreadyChecked.contains(plugin)
						&& !toCheck.contains(plugin))
					toCheck.add(plugin);
			}
		}

	}

	public void installOrUpdatePlugin(Plugin p) throws Exception {
		PluginListWrapper availablePlugins = loadPluginsFromServer();

		checkDependencies(p, availablePlugins);

		File targetDir = getDropinsDir();

		File downloadedPluginFile = downloadPlugin(p, targetDir);

		// download successful - can remove old version
		uninstall(p.getSymbolicName(), p.getInstalledVersion(),
				downloadedPluginFile);

		for (Dependency dependency : p.getDependencies()) {
			// this has to be loaded again to be sure to have the current
			// installed plugins in it (additional plugins may be installed
			// meanwhile)
			PluginListWrapper installedPlugins = getInstalledPlugins();
			Plugin plugin = availablePlugins.get(dependency);
			if (!installedPlugins.getPlugins().contains(plugin))
				installOrUpdatePlugin(plugin);
		}
	}

	// impacts of closing output stream not clear, maybe used later on
	public File downloadPlugin(Plugin p, Object targetDirOrZipOut)
			throws Exception {
		if (Strings.isNullOrEmpty(p.getDownloadUrl())) {
			throw new Exception(
					"Plugin is missing download URL, cannot download");
		}
		URL url;
		try {
			url = new URL(p.getDownloadUrl());
		} catch (MalformedURLException mue) {
			log.debug("Plugin's downloadUrl partial: {}, "
					+ "interpreting as relative now.", mue.getMessage());
			url = new URL(new URL(getUpdateSite()), p.getDownloadUrl());
		}
		String fileName = new File(url.getPath()).getName();
		log.debug("Plugin File to download: {}", fileName);

		File file = null;
		try {
			OutputStream outputStream = null;
			if (targetDirOrZipOut instanceof File) {
				file = new File((File) targetDirOrZipOut, fileName);
				if (file.exists()) {
					log.warn("Trying to overwrite existing plugins file {}",
							file);
					if (!file.canWrite()) {
						throw new Exception("Cannot overwrite existing file "
								+ file);
					}
				}
				outputStream = new FileOutputStream(file);
			} else if (targetDirOrZipOut instanceof ZipOutputStream) {
				ZipOutputStream zipOut = (ZipOutputStream) targetDirOrZipOut;
				zipOut.putNextEntry(new ZipEntry(fileName));
				outputStream = zipOut;
			} else {
				throw new IllegalArgumentException(
						"downloadPlugin can only download "
								+ "to File or ZipOutputStream, but got "
								+ targetDirOrZipOut.getClass());
			}

			downloadNewFile(outputStream, url.toString());
		} catch (IOException e) {
			// clean up
			try {
				if (file != null) {
					file.delete();
				}
			} catch (Exception e2) {
				log.debug("Exception while deleting the downloaded file "
						+ "after exception", e2);
			}
			throw e;
		}
		return file;
	}

	/**
	 * Tries to delete a bundle under dropins. Returns true to indicate that
	 * after next restart, bundle will probably have disappeared.
	 * 
	 * @param symbolicName
	 * @param installedVersion
	 * @param doNotUninstallThis
	 * @return
	 * @throws Exception
	 */
	public boolean uninstall(String symbolicName, String installedVersion,
			File doNotUninstallThis) throws Exception {
		boolean successful = false;
		for (Bundle b : findAllOpenlcaPluginBundles()) {
			log.debug("searching bundle {}, current {}", symbolicName, b);
			if (b.getSymbolicName().equals(symbolicName)) {
				if ((b.getVersion().equals(Version.emptyVersion) && installedVersion == null)
						|| (b.getVersion() != null && b.getVersion().toString()
								.equals(installedVersion))) {
					// found the bundle
					String bundleJarPath = PlatformUtils.getBundleJarPath(b
							.getSymbolicName());
					if (uninstallableJarOrWarn(symbolicName, installedVersion,
							bundleJarPath)) {
						File bundleJar = new File(bundleJarPath);
						if (doNotUninstallThis != null
								&& bundleJar.equals(doNotUninstallThis)) {
							log.debug("Not uninstalling because "
									+ "sanity check found match {}=={}",
									bundleJar, doNotUninstallThis);
						} else {
							if (!bundleJar.delete()) {
								log.info("Could not delete plugin Jar: {}, "
										+ "trying with deleteOnExit()",
										bundleJar);
								bundleJar.deleteOnExit();
							}
							// now it should be set
							successful = true;
						}
					}

				}
			}
		}
		return successful;
	}

	private boolean uninstallableJarOrWarn(String symbolicName, Object version,
			String bundleJarPath) {
		boolean retval = false;

		if (Strings.isNullOrEmpty(bundleJarPath)) {
			log.info("Cannot determine "
					+ "jar location of plugin {} version {}, " + ".",
					symbolicName, version);
		} else {
			File bundleJar = new File(bundleJarPath);
			File parentFile = bundleJar.getParentFile();
			if (!bundleJar.exists()) {
				log.info("Found jar does not exist in file system: {}. "
						+ "Plugin not uninstalled", bundleJar);
			} else if (parentFile == null
					|| !parentFile.getAbsolutePath().contains("dropins")) {
				log.info("Found jar not underneath 'dropins' folder: {}, "
						+ "plugin not uninstalled.");
			} else {
				retval = true;
			}
		}

		return retval;
	}

	protected void downloadNewFile(final OutputStream fileOutputStream,
			String downloadUrl) throws Exception {

		Client c = createWSClient();
		log.debug("Downloading {}", downloadUrl);
		WebResource r2 = c.resource(downloadUrl);
		final InputStream s = r2.get(InputStream.class);
		log.debug("Inputstream for {} s null? {}", downloadUrl, (s == null));
		if (s == null) {
			throw new Exception("Could not install plugin, "
					+ "download failed: No data returned.");
		}
		ByteStreams.copy(new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return s;
			}
		}, new OutputSupplier<OutputStream>() {
			@Override
			public OutputStream getOutput() throws IOException {
				return fileOutputStream;
			}
		});
	}

	private File getDropinsDir() throws Exception {
		Location installLocation = Platform.getInstallLocation();
		if (installLocation == null)
			return null;
		log.trace("install location: {}", installLocation.getURL());
		File installDir = new File(installLocation.getURL().getFile());
		if (!installDir.exists()) {
			log.warn("install folder {} does not exist", installDir);
			return null;
		}
		log.trace("has write access {}", installDir.canWrite());
		File dropinsDir = new File(installDir, "dropins");
		if (!dropinsDir.exists())
			dropinsDir.mkdirs();
		return dropinsDir;
	}

	public ArrayList<Bundle> findAllOpenlcaPluginBundles() {
		ArrayList<Bundle> retval = new ArrayList<>();
		for (Bundle b : RcpActivator.getDefault().getBundle()
				.getBundleContext().getBundles()) {

			try {
				Object olcaPluginVersion = b.getHeaders().get(
						"Openlca-PluginVersion");
				if (olcaPluginVersion instanceof String
						|| olcaPluginVersion instanceof Number) {
					Integer version = Integer.parseInt(olcaPluginVersion
							.toString());
					if (version == 1) {
						retval.add(b);
					} else {
						log.warn("Unknown Openlca-PluginVersion {} on {}",
								olcaPluginVersion, b.getSymbolicName());
					}
				}
			} catch (NumberFormatException nfe) {
				log.debug("Unmatching openLCA plugin: " + b.getSymbolicName()
						+ ", version not parseable.");
			}
		}
		return retval;
	}

	private String getUpdateSite() {
		String property = System
				.getProperty(UPDATE_SITE_PLUGIN_INSTALLER_PROPERTY);
		if (Strings.isNullOrEmpty(property)) {
			property = System.getProperty(UPDATE_SITE_CORE_PROPERTY);
		}
		if (Strings.isNullOrEmpty(property)) {
			property = DEFAULT_SERVER_ROOT;
		}
		if (!property.endsWith("/")) {
			property = property.trim() + "/";
		}
		return property.trim();
	}

	public Client createWSClient() {
		return ApacheHttpClient.create();
	}

	/**
	 * Check point separated version strings. Empty or <code>null</code> input
	 * leads to <code>false</code> return.
	 * 
	 * @param newCandidate
	 * @param oldVersion
	 * @return
	 */
	public static boolean isNewer(String newCandidate, String oldVersion) {
		if (Strings.isNullOrEmpty(newCandidate)
				|| Strings.isNullOrEmpty(oldVersion)) {
			return false;
		}
		// for newness a place that exists in oldVersion has to be bigger or
		// equal in bigger in new.
		// Check from front to back, one has to be bigger.
		Splitter versionSplitter = Splitter.on('.').trimResults();
		Iterator<String> newParts = versionSplitter.split(newCandidate)
				.iterator();
		Iterator<String> oldParts = versionSplitter.split(oldVersion)
				.iterator();
		boolean newnessConfirmed = false;
		boolean oldnessConfirmed = false;
		while (newParts.hasNext() && oldParts.hasNext()) {
			String newNum = newParts.next();
			String oldNum = oldParts.next();
			try {
				int newInt = Integer.parseInt(newNum);
				int oldInt = Integer.parseInt(oldNum);
				if (newInt > oldInt) {
					newnessConfirmed = true;
					break;
				} else if (newInt < oldInt) {
					log.debug("New version determined to be older "
							+ "than old version: {}, {}.", newCandidate,
							oldVersion);
					oldnessConfirmed = true;
					break;
				} // else equal: continue
			} catch (NumberFormatException nfe) {
				if (newNum.equals(oldNum)) {
					// allow textual parts if they need not be diffed
					continue;
				}
				log.debug("Cannot determine newness of {} over {}, "
						+ "version not composed of numbers.", newCandidate,
						oldVersion);
				break;
			}
		}
		if (!oldnessConfirmed) {
			if (newParts.hasNext() && !oldParts.hasNext()) {
				newnessConfirmed = true;
			}
		}
		return newnessConfirmed;
	}

	public String getOpenlcaVersion() {
		for (Bundle b : RcpActivator.getDefault().getBundle()
				.getBundleContext().getBundles()) {
			if ("olca-app".equals(b.getSymbolicName())) {
				return b.getVersion().toString();
			}
		}
		return "0.0.0";
	}
}

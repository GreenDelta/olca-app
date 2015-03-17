package org.openlca.app.rcp.plugins;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.openlca.app.App;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class PluginService {

	public final static String BASE_URL = "http://www.openlca.org/files/openlca";
	private final static Logger log = LoggerFactory
			.getLogger(PluginService.class);
	private final static JsonLoader jsonLoader = new JsonLoader();
	private final static BundleService bundleService = new BundleService();
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static String PLUGINS_DIRECTORY = "plugins";

	public List<Plugin> getAvailablePlugins() {
		String json = jsonLoader.getPluginsJson();
		try {
			List<Plugin> plugins = mapper.readValue(json, new PluginList());
			for (Plugin plugin : plugins)
				setStatus(plugin);
			return plugins;
		} catch (Exception e) {
			log.error("Error parsing plugins json", e);
			return Collections.emptyList();
		}
	}

	private void setStatus(Plugin plugin) {
		Bundle installed = Platform.getBundle(plugin.getSymbolicName());
		if (installed == null)
			return;
		plugin.setInstalled(true);
		plugin.setCurrentVersion(installed.getVersion().toString());
		if (compare(plugin.getVersion(), installed.getVersion().toString()) > 0)
			plugin.setUpdateable(true);
	}

	public void install(Plugin plugin) {
		download(plugin);
		uninstall(plugin, plugin.getVersion());
		plugin.setRestartNecessary(true);
		plugin.setInstalled(true);
		plugin.setCurrentVersion(plugin.getVersion());
		plugin.setUpdateable(false);
	}

	private boolean download(Plugin plugin) {
		try {
			URL url = getDownloadUrl(plugin);
			Path temp = Files.createTempFile("olca-plugin", ".jar");
			IOUtils.copy(url.openStream(), Files.newOutputStream(temp));
			copyToDropins(temp, plugin.getFileName());
			return true;
		} catch (IOException e) {
			log.error("Could not download plugin " + plugin.getDisplayName(), e);
			return false;
		}
	}

	private URL getDownloadUrl(Plugin plugin) {
		try {
			return new URL(BASE_URL + "/" + App.getVersion() + "/"
					+ PLUGINS_DIRECTORY + "/" + plugin.getFileName());
		} catch (MalformedURLException e) {
			log.error("Error constructing download url", e);
			return null;
		}
	}

	public boolean copyLocalFile(Path from, Plugin plugin) {
		if (plugin.isInstalled() && !plugin.isUpdateable())
			return false;
		boolean success = copyToDropins(from, plugin.getFileName());
		if (!success)
			return false;
		plugin.setRestartNecessary(true);
		if (plugin.isInstalled())
			plugin.setUpdated(true);
		else
			plugin.setInstalled(true);
		return true;
	}

	private boolean copyToDropins(Path from, String fileName) {
		try {
			File dropins = getDropinsDirectory();
			Path to = new File(dropins, fileName).toPath();
			IOUtils.copy(Files.newInputStream(from), Files.newOutputStream(to));
			return true;
		} catch (IOException e) {
			log.error("Could not copy file", e);
			return false;
		}
	}

	private File getDropinsDirectory() {
		Location installationLocation = Platform.getInstallLocation();
		if (installationLocation == null)
			return null;
		File installation = new File(installationLocation.getURL().getFile());
		if (!installation.exists())
			return null;
		File dropinsDirectory = new File(installation, "dropins");
		if (!dropinsDirectory.exists())
			dropinsDirectory.mkdirs();
		return dropinsDirectory;
	}

	public void uninstall(Plugin plugin) {
		uninstall(plugin, null);
		plugin.setRestartNecessary(true);
		plugin.setInstalled(false);
		plugin.setCurrentVersion(null);
		plugin.setUpdateable(false);
	}

	private void uninstall(Plugin plugin, String versionToKeep) {
		Bundle[] bundles = bundleService.getBundles(plugin.getSymbolicName());
		for (Bundle bundle : bundles)
			if (bundle.getVersion() == null)
				bundleService.delete(bundle);
			else if (!bundle.getVersion().toString().equals(versionToKeep))
				bundleService.delete(bundle);
	}

	private int compare(String v1, String v2) {
		int[] version1 = parseVersion(v1);
		int[] version2 = parseVersion(v2);
		for (int i = 0; i < version1.length; i++) {
			int result = Integer.compare(version1[i], version2[i]);
			if (result != 0)
				return result;
		}
		return 0;
	}

	private int[] parseVersion(String v) {
		int[] versions = new int[3];
		if (v == null)
			return versions;
		String[] splitted = v.split("\\.");
		for (int i = 0; i < splitted.length && i < 3; i++)
			versions[i] = Integer.parseInt(splitted[i]);
		return versions;
	}

	public void update(Plugin plugin) {
		install(plugin);
		plugin.setUpdated(true);
	}

	private class PluginList extends TypeReference<List<Plugin>> {

	}

}

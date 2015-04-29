package org.openlca.app.rcp.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.openlca.app.rcp.RcpActivator;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BundleService {

	private static final Logger log = LoggerFactory
			.getLogger(BundleService.class);
	private static final String MANIFEST_PATH = "/META-INF/MANIFEST.MF";

	public Bundle[] getBundles(String symbolicName) {
		Bundle[] bundles = RcpActivator.getDefault().getBundle()
				.getBundleContext().getBundles();
		List<Bundle> matches = new ArrayList<>();
		for (Bundle bundle : bundles)
			if (bundle.getSymbolicName().equals(symbolicName))
				matches.add(bundle);
		return matches.toArray(new Bundle[matches.size()]);
	}

	public void delete(Bundle bundle) {
		File jar = getBundleJar(bundle);
		if (jar != null)
			if (!jar.delete())
				jar.deleteOnExit();
	}

	private File getBundleJar(Bundle bundle) {
		try {
			URL location = FileLocator.resolve(FileLocator.find(bundle,
					new Path(MANIFEST_PATH), null));
			String path = getBundlePath(MANIFEST_PATH, location);
			if (path == null)
				return null;
			return new File(path);
		} catch (IOException e) {
			log.error("Error getting bundle jar file", e);
			return null;
		}
	}

	private String getBundlePath(String manifestPath, URL location) {
		String protocol = location.getProtocol();
		String path = location.getPath();
		if (protocol.equals("jar") && path.startsWith("file:"))
			path = path.substring("file:".length());
		else if (!protocol.equals("file"))
			return null;
		if (path.endsWith(manifestPath))
			path = path.substring(0, path.length() - manifestPath.length());
		if (path.endsWith("!"))
			path = path.substring(0, path.length() - 1);
		if (path.startsWith("/"))
			path = path.substring(1);
		return path;
	}

	public String[] getSymbolicNameAndVersion(File jar) {
		try (JarInputStream jarStream = new JarInputStream(new FileInputStream(
				jar))) {
			Manifest manifest = jarStream.getManifest();
			String symbolicName = getValue(manifest, "Bundle-SymbolicName")
					.trim();
			if (symbolicName.contains(";"))
				symbolicName = symbolicName.substring(0,
						symbolicName.indexOf(";"));
			String version = getValue(manifest, "Bundle-Version");
			return new String[] { symbolicName, version };
		} catch (Exception e) {
			log.error("Error parsing jar manifest", e);
			return new String[2];
		}
	}

	private String getValue(Manifest manifest, String key) {
		return manifest.getMainAttributes().getValue(key);
	}

}

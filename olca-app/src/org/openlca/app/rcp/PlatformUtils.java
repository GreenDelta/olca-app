package org.openlca.app.rcp;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PlatformUtils {

	private static final Logger log = LoggerFactory
			.getLogger(PlatformUtils.class);

	public static File getInstallRoot() throws Exception {
		Location installLocation = Platform.getInstallLocation();
		if (installLocation == null)
			return null;
		log.trace("get install location {}", installLocation.getURL());
		File installDir = new File(installLocation.getURL().getFile());
		return installDir;
	}

	/**
	 * Expects the bundle to have a META-INF/MANIFEST.MF set and to exist.
	 * 
	 * @param symbolicName
	 * @return
	 * @throws IOException
	 */
	public static String getBundleJarPath(String symbolicName)
			throws IOException {
		String MANIFESTPath = "/META-INF/MANIFEST.MF";
		URL pluginManifLoc = FileLocator
				.resolve(FileLocator.find(Platform.getBundle(symbolicName),
						new Path(MANIFESTPath), null));
		return getBundleJarPathInternal(MANIFESTPath, pluginManifLoc);
	}

	protected static String getBundleJarPathInternal(String MANIFESTPath,
			URL pluginManifLoc) {
		String protocol = pluginManifLoc.getProtocol();
		String path = pluginManifLoc.getPath();
		if (protocol.equals("file")) {
			// ok, can use like this
		} else if (protocol.equals("jar") && path.startsWith("file:")) {
			path = path.substring("file:".length());
		} else {
			log.warn("Plugin protocol not file but {}", protocol);
			throw new RuntimeException(
					"Cannot determine path - protocol indicates non-local URL.");
		}

		if (path.endsWith(MANIFESTPath)) {
			path = path.substring(0, path.length() - MANIFESTPath.length());
		}
		if (path.endsWith("!")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	public static boolean checkInstallPath(File installDir) {
		// need two points!
		int points = 0;
		if (new File(installDir, "p2").exists()) {
			points++;
		}
		if (new File(installDir, ".eclipseproduct").exists()) {
			points++;
		}
		if (new File(installDir, "configuration").exists()) {
			points++;
		}
		if (new File(installDir, "openLCA").exists()) {
			points++;
		}
		if (new File(installDir, "openLCA.exe").exists()) {
			points++;
		}
		if (new File(installDir, "artifacts.xml").exists()) {
			points++;
		}
		if (new File(installDir, "plugins").exists()) {
			points++;
		}
		log.debug("Checked found installation path, {} points for {}", points,
				installDir);
		return points > 2;
	}

	/**
	 * Finds the appropriate java executable for window-system execution (javaw
	 * on windows, but java on mac).
	 * 
	 * @return
	 */
	public static File findJavaExecutable() {
		String vm = System.getProperty("eclipse.vm");
		if (Strings.isNullOrEmpty(vm)) {
			throw new RuntimeException(
					"Cannot determine JVM to use for updater: "
							+ "System property missing.");
		}
		File javaExecutable = null;
		File vmFile = new File(vm);
		// look for a javaw or javaw.exe in a bin directory above
		// the current file, or JavaVM.framework/Home/bin/java in a Frameworks
		// directory (Mac)
		File candDir = vmFile.getParentFile();
		outerWhile: while (candDir != null) {
			for (String binary : new String[] { // windows (path varies):
			"javaw.exe", "bin/javaw.exe",
					// linux (eclipse.vm set to java executable):
					"java",
					// Mac OS:
					"JavaVM.framework/Home/bin/java" }) {
				File javaCand = new File(candDir, binary);
				if (javaCand.exists()) {
					javaExecutable = javaCand;
					break outerWhile;
				}
			}

			candDir = candDir.getParentFile();
		}
		return javaExecutable;
	}

	public static boolean hasOpenLCAExecutable(String installDir) {
		return new File(installDir, "openLCA").exists()
				|| new File(installDir, "openLCA.exe").exists();
	}

	public static boolean isWindows() {
		boolean winLocal = false;
		String osName = System.getProperty("os.name");
		if (osName != null && osName.toLowerCase().contains("win")) {
			winLocal = true;
		}
		return winLocal;
	}

	public static boolean isMac() {
		boolean macLocal = false;
		String osName = System.getProperty("os.name");
		if (osName != null && osName.toLowerCase().contains("mac")) {
			macLocal = true;
		}
		return macLocal;
	}

}

package org.openlca.app.preferences;

import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;

/**
 * A utility class for downloading the native libraries of a module to a given
 * folder.
 */

// TODO: this is currently a copy of the dev branch of the olca-native package
// and should be removed when this is stable there
public class LibDownload {

	public enum Repo {
		GITHUB, MAVEN
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final File targetDir;
	private final Module module;
	private final Repo repo;

	/**
	 * Creates a new download.
	 *
	 * @param repo      the repository from which the libraries should be
	 *                  downloaded
	 * @param module    the module for which the libraries should be downloaded
	 * @param targetDir the target directory where the libraries should be
	 *                  extracted to
	 */
	public LibDownload(Repo repo, Module module, File targetDir) {
		this.targetDir = targetDir;
		this.module = module;
		this.repo = repo;
	}

	public static void fetch(Repo repo, Module module, File targetDir) {
		try {
			new LibDownload(repo, module, targetDir).run();
		} catch (Exception e) {
			throw new RuntimeException("Download failed", e);
		}
	}

	public void run() throws Exception {

		try {
			var dir = libFolderOf(targetDir);
			if (!dir.exists()) {
				Files.createDirectories(dir.toPath());
			}

			var path = getUrl();
			log.info("fetch jar from {}", path);
			URL url = new URL(path);

			// download the jar/zip into a temporary file
			var zip = Files.createTempFile(
				"olca-native-" + NativeLib.VERSION + "-", ".zip");
			log.info("download jar to {}", zip);
			try (var in = url.openStream()) {
				Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
			}

			// extract and delete the jar/zip
			extractJar(dir, zip.toFile());
			Files.delete(zip);

		} catch (Exception e) {
			log.error("failed to download and extract native libraries", e);
			throw e;
		}
	}

	private String getUrl() {
		var baseName = "olca-native-" + module.toString() + "-"
				+ os() + "-" + arch();
		if (repo == Repo.MAVEN) {
			return "https://repo1.maven.org/maven2/org/openlca/"
				+ baseName + "/" + NativeLib.VERSION + "/" + baseName
				+ "-" + NativeLib.VERSION + ".jar";
		} else {
			return "https://github.com/GreenDelta/olca-native/releases/download/v"
				+ NativeLib.VERSION + "/" + baseName + ".zip";
		}
	}

	private void extractJar(File dir, File jar) throws IOException {
		try (var zip = new ZipFile(jar)) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				var e = entries.nextElement();
				if (e.isDirectory())
					continue;
				var target = new File(dir, e.getName());
				// we do this to skip the sub-directories
				target = new File(dir, target.getName());
				try (var in = zip.getInputStream(e)) {
					Files.copy(in, target.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static File libFolderOf(File rootDir) {
		var path = String.join(
				File.separator, "olca-native", NativeLib.VERSION, arch());
		return new File(rootDir, path);
	}

	private static String arch() {
		var arch = System.getProperty("os.arch");
		if (arch == null)
			return "x64";
		var lower = arch.trim().toLowerCase();
		return lower.startsWith("aarch") || lower.startsWith("arm")
				? "arm64"
				: "x64";
	}

	private static String os() {
		var os = System.getProperty("os.name");
		if (os == null)
			return "win";
		var lower = os.trim().toLowerCase();
		if (lower.startsWith("linux"))
			return "linux";
		if (lower.startsWith("mac"))
			return "macos";
		return "win";
	}
}


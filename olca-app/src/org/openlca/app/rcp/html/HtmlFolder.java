package org.openlca.app.rcp.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.openlca.app.App;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class HtmlFolder {

	private static final Logger log = LoggerFactory.getLogger(HtmlFolder.class);

	public static File getDir(Bundle bundle) {
		File htmlDir = new File(App.getWorkspace(), "html");
		return new File(htmlDir, bundle.getSymbolicName());
	}

	public static void initialize(Bundle bundle, String zipPath) {
		if (!isValid(bundle) || zipPath == null)
			return;
		Version version = getWorkspaceVersion(bundle);
		if (Objects.equals(version, bundle.getVersion())) {
			log.trace("HTML folder for {} up-to-date");
			return;
		}
		log.trace("initialize html folder {} for {}", zipPath, bundle);
		try {
			extractFolder(bundle, zipPath);
		} catch (Exception e) {
			log.error("failed to extract HTML folder " + zipPath, e);
		}
	}

	private static boolean isValid(Bundle bundle) {
		if (bundle == null) {
			log.error("invalid bundle: NULL");
			return false;
		}
		if (bundle.getSymbolicName() == null) {
			log.error("invalid bundle: no symbolic name");
			return false;
		}
		if (bundle.getVersion() == null) {
			log.error("invalid bundle: no version");
			return false;
		}
		return true;
	}

	private static Version getWorkspaceVersion(Bundle bundle) {
		File versionFile = new File(getDir(bundle), ".version");
		if (!versionFile.exists())
			return null;
		try {
			byte[] bytes = Files.readAllBytes(versionFile.toPath());
			String version = new String(bytes, "utf-8");
			return Version.parseVersion(version);
		} catch (Exception e) {
			log.error("failed to read HTML folder version", e);
			return null;
		}
	}

	private static void extractFolder(Bundle bundle, String zipPath)
			throws Exception {
		File dir = getDir(bundle);
		if (dir.exists())
			FileUtils.deleteDirectory(dir);
		dir.mkdirs();
		writeVersion(bundle);
		InputStream zipStream = FileLocator.openStream(bundle,
				new Path(zipPath), false);
		File zipFile = new File(dir, "@temp.zip");
		try (FileOutputStream out = new FileOutputStream(zipFile)) {
			IOUtils.copy(zipStream, out);
		}
		ZipUtil.unpack(zipFile, dir);
		if (!zipFile.delete())
			zipFile.deleteOnExit();
	}

	private static void writeVersion(Bundle bundle) throws Exception {
		File versionFile = new File(getDir(bundle), ".version");
		String version = bundle.getVersion().toString();
		Files.write(versionFile.toPath(), version.getBytes("utf-8"));
	}

	public static String getUrl(Bundle bundle, String page) {
		File file = getFile(bundle, page);
		if (file == null)
			return null;
		try {
			URL url = file.toURI().toURL();
			return url.toString();
		} catch (Exception e) {
			log.error("failed to get URL for page " + bundle + "/" + page, e);
			return null;
		}
	}

	public static File getFile(Bundle bundle, String page) {
		if (!isValid(bundle))
			return null;
		File file = new File(getDir(bundle), page);
		if (!file.exists()) {
			log.error("the requested file {} does not exist", file);
			return null;
		}
		return file;
	}

}

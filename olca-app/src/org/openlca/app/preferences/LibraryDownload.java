package org.openlca.app.preferences;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openlca.app.App;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.util.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryDownload implements Runnable {

	private Logger log = LoggerFactory.getLogger(LibraryDownload.class);
	private String error;

	public static void open() {
		boolean b = Question.ask("Download calculation libraries",
				"You can download additional libraries to make"
						+ " the calculation faster. However, this currently"
						+ " only improves the calculation speed of product"
						+ " systems with sparse matrices in the quick"
						+ " calculation. Also, some of these libraries are"
						+ " licensed under the GNU General Public License v2"
						+ " (see https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)."
						+ " By downloading these libraries you accept the terms and"
						+ " conditions of this license. Do you want to download"
						+ " these additional calculation libraries?");
		if (!b)
			return;
		LibraryDownload download = new LibraryDownload();
		App.runWithProgress("Download native libraries", download, () -> {
			if (download.error != null) {
				MsgBox.error("Download failed", download.error);
			} else {
				MsgBox.info("Download finished", "Note that you need to "
						+ "restart openLCA in order to use "
						+ "the downloaded libraries.");
			}
		});
	}

	@Override
	public void run() {

		// test if we can write in the installation folder
		File installDir = App.getInstallLocation();
		if (!Files.isWritable(installDir.toPath())) {
			error = "No write access in installation folder "
					+ installDir.getAbsolutePath();
			return;
		}

		// test if we have an URL for the OS
		String os = getOS();
		if (os == null) {
			MsgBox.error("Could not identify OS",
					"Could not indentify the URL for the "
							+ "following operating system: " + OS.get());
			return;
		}

		// download & extract the package
		String path = "https://github.com/msrocka/olca-rust/releases/"
				+ "download/v1.0.0/olcar_withumf_1.0.0_"
				+ os + "_2019-10-07.zip";
		try {
			URL url = new URL(path);
			try (InputStream in = url.openStream()) {
				Path temp = Files.createTempFile("olcar_1.0.0_", ".zip");
				log.info("download native libraries from {} to {}", path, temp);
				Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
				extract(temp.toFile());
			}
		} catch (Exception e) {
			log.error("failed to download native libraries from " + path, e);
			error = "Failed to download native libaries: " + e.getMessage();
		}
	}

	private void extract(File temp) throws Exception {
		File dir = App.getInstallLocation();
		log.info("extract library package {}", temp);
		try (ZipFile zip = new ZipFile(temp)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();
				if (e.isDirectory())
					continue;
				File target = new File(dir, e.getName());
				if (target.exists()) {
					log.info("file {} already exists", target);
					continue;
				}
				try (InputStream in = zip.getInputStream(e)) {
					log.info("copy library {}", target);
					Files.copy(in, target.toPath());
				}
			}
		}
	}

	private static String getOS() {
		switch (OS.get()) {
		case WINDOWS:
			return "windows";
		case LINUX:
			return "linux";
		case MAC:
			return "macos";
		default:
			return null;
		}
	}

}

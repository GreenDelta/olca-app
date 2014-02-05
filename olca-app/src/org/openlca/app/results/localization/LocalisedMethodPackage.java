package org.openlca.app.results.localization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * A zip-file containing a localised impact assessment method serialised as JSON
 * text.
 */
public class LocalisedMethodPackage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private File zipFile;

	public LocalisedMethodPackage(File zipFile) {
		this.zipFile = zipFile;
	}

	@SuppressWarnings("resource")
	// impacts of closing resources not clear
	public LocalisedImpactMethod read() {
		try {
			ZipFile zip = new ZipFile(zipFile);
			ZipEntry entry = zip.getEntry("entry");
			InputStream in = zip.getInputStream(entry);
			InputStreamReader reader = new InputStreamReader(in, "utf-8");
			Gson gson = new Gson();
			LocalisedImpactMethod m = gson.fromJson(reader,
					LocalisedImpactMethod.class);
			return m;
		} catch (Exception e) {
			log.error("failed to read method from package", e);
			return null;
		}
	}

	public void write(LocalisedImpactMethod method) {
		try (FileOutputStream fos = new FileOutputStream(zipFile);
				ZipOutputStream zos = new ZipOutputStream(fos)) {
			ZipEntry entry = new ZipEntry("entry");
			zos.putNextEntry(entry);
			Gson gson = new Gson();
			byte[] bytes = gson.toJson(method).getBytes("utf-8");
			zos.write(bytes);
		} catch (Exception e) {
			log.error("failed to store method package", e);
		}
	}

}

package org.openlca.app.db;

import java.io.File;
import java.nio.file.Files;

import org.openlca.app.rcp.RcpActivator;
import org.zeroturnaround.zip.ZipUtil;

public enum DbTemplate {

	EMPTY("db_templates/empty.zolca"),

	UNITS("db_templates/units.zolca"),

	FLOWS("db_templates/flows.zolca");

	private final String resourcePath;

	DbTemplate(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	/**
	 * Extracts the database template into the give directory. The name of the
	 * given directory will be the name of the database.
	 */
	public void extract(File dir) {
		try (var in = RcpActivator.getStream(resourcePath)) {
			if (!dir.exists()) {
				Files.createDirectories(dir.toPath());
			}
			ZipUtil.unpack(in, dir);
		} catch (Exception e) {
			throw new RuntimeException(
				"failed to extract database template " + name() + " to " + dir, e);
		}
	}

}

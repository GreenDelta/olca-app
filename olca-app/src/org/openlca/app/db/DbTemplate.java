package org.openlca.app.db;

import java.io.File;
import java.io.InputStream;

import org.openlca.app.rcp.RcpActivator;
import org.zeroturnaround.zip.ZipUtil;

enum DbTemplate {

	EMPTY("db_templates/empty.zolca"),

	UNITS("db_templates/units.zolca"),

	FLOWS("db_templates/flows.zolca");

	private final String resourcePath;

	private DbTemplate(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	/**
	 * Extracts the database template into the give directory. The name of the
	 * given directory will be the name of the database.
	 */
	public void extract(File dir) throws Exception {
		if (!dir.exists())
			dir.mkdirs();
		try (InputStream in = RcpActivator.getStream(resourcePath)) {
			ZipUtil.unpack(in, dir);
		}
	}

}

package org.openlca.app.db;

import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;

import org.eclipse.osgi.util.NLS;
import org.openlca.app.M;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.commons.Res;
import org.openlca.util.Dirs;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public enum DbTemplate {

	EMPTY("db_templates/empty.zolca"),

	UNITS("db_templates/units.zolca"),

	FLOWS("db_templates/flows.zolca");

	private final String resourcePath;

	DbTemplate(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	/// Extracts the database template into the given directory. The name of the
	/// given directory will be the name of the database. If the extraction
	/// fails, a directory that was created by this method is deleted again, so
	/// that no incomplete database is left behind. The returned error contains
	/// a message that can be shown to the user.
	public Res<Void> extract(File dir) {
		if (dir == null)
			return Res.error("no target folder for the database provided");
		boolean created = !dir.exists();
		try (var in = RcpActivator.getStream(resourcePath)) {
			if (in == null)
				return Res.error("database template not found: " + resourcePath);
			Files.createDirectories(dir.toPath());
			ZipUtil.unpack(in, dir);
			return Res.ok();
		} catch (Exception e) {
			if (created) {
				deleteQuietly(dir);
			}
			return Res.error(messageOf(e, dir), e);
		}
	}

	/// Returns a message for the user. Missing write permissions are the most
	/// common reason; note that the zip library wraps the IO exceptions in its
	/// own exceptions, so we look for them in the causes.
	private static String messageOf(Exception e, File dir) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof AccessDeniedException ade) {
				var path = ade.getFile() != null ? ade.getFile() : dir.getPath();
				return NLS.bind(M.NoWriteAccessToFolder, path);
			}
		}
		return M.FailedToCreateDatabase + ": " + e.getMessage();
	}

	private static void deleteQuietly(File dir) {
		try {
			Dirs.delete(dir);
		} catch (Exception e) {
			LoggerFactory.getLogger(DbTemplate.class)
				.warn("failed to delete incomplete database folder {}", dir, e);
		}
	}

}

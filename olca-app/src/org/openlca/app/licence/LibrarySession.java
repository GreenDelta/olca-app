package org.openlca.app.licence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import com.google.gson.stream.JsonReader;
import org.openlca.app.M;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.license.License;
import org.openlca.license.access.LicenseStatus;
import org.openlca.license.access.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openlca.license.access.LicenseStatus.*;

public class LibrarySession {

	private static final String SESSION_EXTENSION = ".session";
	static Logger log = LoggerFactory.getLogger(LibrarySession.class);

	/**
	 * Lazily creates a session directory.
	 */
	private static File getSessionDir() {
		var dir = new File(Workspace.root(), "sessions");
		if (!dir.exists()) {
			try {
				Files.createDirectories(dir.toPath());
			} catch (IOException e) {
				var log = LoggerFactory.getLogger(LibrarySession.class);
				log.error("failed to create sessions directory: " + dir, e);
				return null;
			}
		}
		return dir;
	}

	public static boolean isValid(String libraryName) {
		var libDir = Workspace.getLibraryDir();
		var lib = libDir.getLibrary(libraryName).orElse(null);
		if (lib == null)
			return false;

		var license = License.of(lib.folder()).orElse(null);

		// no license found, the library can be read without session
		if (license == null)
			return true;

		var session = retrieveSession(libraryName).orElse(null);

		try {
			LicenseStatus status;
			if (session != null) {
				status = license.status(lib.folder(), session);
				if (status != VALID)
					return handleInvalid(libraryName, status);
			} else {
				var credentials = AuthenticationDialog.promptCredentials(libraryName);
				if (credentials == null)
					return false;
				status = license.status(lib.folder(), credentials);
				if (status != VALID)
					return handleInvalid(libraryName, status);
				session = license.createSession(credentials);
			}
		} catch (IOException | IllegalArgumentException e) {
			MsgBox.error("Failed to open the library.");
			return false;
		}
		if (session == null)
			return false;
		return storeSession(session, libraryName);
	}

	private static boolean storeSession(Session session, String library) {
		var dir = getSessionDir();
		var json = session.toJson();
		var file = new File(dir, library + SESSION_EXTENSION);

		try (var fos = new FileOutputStream(file)) {
			fos.write(json.getBytes());
		} catch (IOException e) {
			log.error("Error while creating the session file: " + file, e);
			return false;
		}
		return true;
	}

	private static boolean handleInvalid(String library, LicenseStatus status) {
		if (status == VALID)
			return false;

		if (status == WRONG_USER || status == WRONG_PASSWORD) {
			if (removeSession(library)) {
				MsgBox.error("The session credentials are not valid. Please log "
						+ "again.");
			}
			return isValid(library);
		} else {
			MsgBox.error("The " + library + " library cannot be opened: "
					+ Message.of(status));
			return false;
		}
	}

	private static File sessionOf(String library) {
		var dir = getSessionDir();
		return new File(dir, library + SESSION_EXTENSION);
	}

	public static boolean removeSession(String library) {
		var json = sessionOf(library);
		if (json.exists())
			return json.delete();
		return false;
	}

	public static Optional<Session> retrieveSession(String library) {
		var json = sessionOf(library);
		if (!json.exists())
			return Optional.empty();

		try (var reader = new JsonReader(new FileReader(json))) {
			return Optional.of(Session.fromJson(reader));
		} catch (IOException e) {
			log.error("Failed to retrieve the session of the following library: "
					+ library, e);
			return Optional.empty();
		}
	}

	private static class Message {

		public static String of(LicenseStatus status) {
			return switch (status) {
				case VALID -> M.LicenseValid;
				case UNTRUSTED -> M.LicenseUntrusted;
				case EXPIRED -> M.LicenseExpired;
				case NOT_YET_VALID -> M.LicenseNotYetValid;
				case CORRUPTED -> M.LicenseCorrupted;
				case WRONG_PASSWORD -> M.LicenseWrongPassword;
				case WRONG_USER -> M.LicenseWrongUser;
			};
		}

	}

}

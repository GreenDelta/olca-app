package org.openlca.app.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.openlca.app.App;
import org.openlca.app.editors.libraries.LibraryEditor;
import org.openlca.app.licence.LibrarySession;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryPackage;
import org.openlca.core.library.reader.LibReader;
import org.openlca.core.library.reader.LibReaderRegistry;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.license.License;
import org.openlca.license.certificate.CertificateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openlca.app.licence.LibrarySession.retrieveSession;
import static org.openlca.license.Licensor.JSON;


public final class Libraries {

	private static final Logger log = LoggerFactory.getLogger(Libraries.class);

	private Libraries() {
	}

	/**
	 * After checking the validity of the license library, tries to create a
	 * library reader for the given library in sync with the currently active
	 * database. Returns an empty option if this fails or if the license is
	 * invalid.
	 */
	public static Optional<LibReader> readerOf(String libId) {
		var libDir = Workspace.getLibraryDir();
		var lib = libDir.getLibrary(libId).orElse(null);
		return lib != null
				? readerOf(lib)
				: Optional.empty();
	}

	/**
	 * After checking the validity of the license library, tries to create a
	 * library reader for the given library in sync with the currently active
	 * database. Returns an empty option if this fails or if the license is
	 * invalid.
	 */
	public static Optional<LibReader> readerOf(Library lib) {
		if (lib == null)
			return Optional.empty();

		var db = Database.get();
		if (db == null)
			return Optional.empty();

		var builder = LibReader.of(lib, db)
				.withSolver(App.getSolver());

		var license = License.of(lib.folder());
		if (license.isPresent()) {
			var credentials = retrieveSession(lib.name()).orElse(null);
			if (credentials == null) {
				log.error("Error while retrieving the library credentials of "
						+ lib.name());
				return Optional.empty();
			}
			builder.withDecryption(() -> license.get().getDecryptCipher(credentials));
		}

		return Optional.of(builder.create());
	}

	/**
	 * Returns the library readers for the currently active database that are
	 * needed to run a calculation. Returns an empty option if this fails or
	 * when no libraries with matrices are mounted to that database.
	 */
	public static Optional<LibReaderRegistry> readersForCalculation() {
		var libs = forCalculation();
		if (libs.isEmpty())
			return Optional.empty();

		var readers = new ArrayList<LibReader>();
		libs.get().forEach(lib -> readerOf(lib).ifPresent(readers::add));

		return readers.isEmpty()
				? Optional.empty()
				: Optional.of(LibReaderRegistry.of(readers));
	}

	public static Optional<Set<Library>> forCalculation() {
		var db = Database.get();
		if (db == null)
			return Optional.empty();
		var mounted = db.getLibraries()
				.stream()
				.map(libId -> Workspace.getLibraryDir().getLibrary(libId))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		var libs = new HashSet<Library>();
		var queue = new ArrayDeque<>(mounted);
		var handled = new HashSet<String>();

		while (!queue.isEmpty()) {
			var lib = queue.poll();
			if (handled.contains(lib.name()))
				continue;
			handled.add(lib.name());
			if (lib.hasMatrices()) {
				libs.add(lib);
			}
			for (var dep : lib.getDirectDependencies()) {
				if (handled.contains(dep.name())
						|| queue.contains(dep))
					continue;
				queue.add(dep);
			}
		}
		return Optional.of(libs);
	}

	public static boolean checkValidity(RootDescriptor d) {
		if (d.isFromLibrary())
			return LibrarySession.isValid(d.library);

		if (d instanceof ProductSystemDescriptor) {
			return checkValidity();
		}
		return true;
	}

	public static boolean checkValidity() {
		var libraries = Libraries.forCalculation()
				.orElse(Collections.emptySet());
		for (var library : libraries) {
			if (!LibrarySession.isValid(library.name()))
				return false;
		}
		return true;
	}

	public static void fillExchangesOf(Process process) {
		fill(process, (db, lib) -> {
			var exchanges = lib.getExchanges(TechFlow.of(process), db);
			var qref = process.quantitativeReference;
			if (qref != null) {
				process.quantitativeReference = exchanges.stream()
						.filter(e -> Objects.equals(qref.flow, e.flow)
								& qref.isInput == e.isInput)
						.findFirst()
						.orElse(null);
			}
			process.exchanges.clear();
			process.exchanges.addAll(exchanges);
		});
	}

	public static void fillFactorsOf(ImpactCategory impact) {
		fill(impact, (db, lib) -> {
			var factors = lib.getImpactFactors(Descriptor.of(impact), db);
			impact.impactFactors.addAll(factors);
		});
	}

	private static void fill(RootEntity e, BiConsumer<IDatabase, LibReader> fn) {
		if (e == null || !e.isFromLibrary())
			return;
		var db = Database.get();
		if (db == null)
			return;
		var lib = readerOf(e.library).orElse(null);
		if (lib == null)
			return;
		fn.accept(db, lib);
	}

	public static Library importFromFile(File file) {
		if (file == null)
			return null;
		var info = LibraryPackage.getInfo(file);
		if (info == null) {
			MsgBox.error(file.getName() + " is not a valid library package.");
			return null;
		}
		var libDir = Workspace.getLibraryDir();
		LibraryPackage.unzip(file, libDir);
		return libDir.getLibrary(info.name()).orElse(null);
	}

	public static Library importFromUrl(String url) {
		try (var stream = new URL(url).openStream()) {
			return importFromStream(stream);
		} catch (IOException e) {
			MsgBox.error("Error trying to resolve library url", e);
			return null;
		}
	}

	public static Library importFromStream(InputStream stream) {
		var file = (Path) null;
		var library = (Library) null;
		try {
			file = Files.createTempFile("olca-library", ".zip");
			Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
			library = importFromFile(file.toFile());
			return library;
		} catch (IOException e) {
			log.error("Error copying library from stream", e);
			return null;
		} finally {
			if (file != null && file.toFile().exists()) {
				try {
					Files.delete(file);
				} catch (IOException e) {
					log.trace("Error deleting tmp file", e);
				}
			}
		}
	}


	public static Optional<CertificateInfo> getLicense(File folder) {
		var file = new File(folder, JSON);
		if (!file.exists())
			return Optional.empty();
		try (var reader = new JsonReader(new FileReader(file))) {
			var gson = new Gson();
			var mapType = new TypeToken<License>() {}.getType();
			License license = gson.fromJson(reader, mapType);

			var certBytes = license.certificate().getBytes();
			var certificate = CertificateInfo.of(new ByteArrayInputStream(certBytes));
			return Optional.of(certificate);
		} catch (IOException e) {
			var log = LoggerFactory.getLogger(LibraryEditor.class);
			log.error("failed to open the license.", e);
			return Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}

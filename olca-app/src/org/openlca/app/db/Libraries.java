package org.openlca.app.db;

import org.openlca.app.App;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.reader.LibReader;
import org.openlca.core.library.reader.LibReaderRegistry;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class Libraries {

	private Libraries() {
	}

	/**
	 * Tries to create a library reader for the library with the given ID (name)
	 * in sync with the currently active database. Returns an empty option if
	 * this fails.
	 */
	public static Optional<LibReader> readerOf(String libId) {
		var libDir = Workspace.getLibraryDir();
		var lib = libDir.getLibrary(libId).orElse(null);
		return lib != null
				? readerOf(lib)
				: Optional.empty();
	}

	/**
	 * Tries to create a library reader for the given library in sync with the
	 * currently active database. Returns an empty option if this fails.
	 */
	public static Optional<LibReader> readerOf(Library lib) {
		if (lib == null)
			return Optional.empty();
		var db = Database.get();
		if (db == null)
			return Optional.empty();
		var r = LibReader.of(lib, db)
				.withSolver(App.getSolver())
				//.withDecryption() // TODO: resolve a possible decryption
				.create();
		return Optional.of(r);
	}

	/**
	 * Returns the library readers for the currently active database that are
	 * needed to run a calculation. Returns an empty option if this fails or
	 * when no libraries with matrices are mounted to that database.
	 */
	public static Optional<LibReaderRegistry> forCalculation() {
		var db = Database.get();
		if (db == null)
			return Optional.empty();
		var mounted = db.getLibraries()
				.stream()
				.map(libId -> Workspace.getLibraryDir().getLibrary(libId))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		var readers = new ArrayList<LibReader>();
		var queue = new ArrayDeque<>(mounted);
		var handled = new HashSet<String>();

		while (!queue.isEmpty()) {
			var lib = queue.poll();
			if (handled.contains(lib.name()))
				continue;
			handled.add(lib.name());
			if (lib.hasMatrices()) {
				readerOf(lib).ifPresent(readers::add);
			}
			for (var dep : lib.getDirectDependencies()) {
				if (handled.contains(dep.name())
						|| queue.contains(dep))
					continue;
				queue.add(dep);
			}
		}

		return readers.isEmpty()
				? Optional.empty()
				: Optional.of(LibReaderRegistry.of(readers));
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
}

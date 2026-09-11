package org.openlca.app.navigation.actions.libraries;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ModelReferences;
import org.openlca.core.library.Library;
import org.openlca.core.library.Unmounter;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.TypedRefId;
import org.openlca.license.License;

/// Checks if a library can be removed (unmounted) from a database and which
/// [Unmounter.Retention] options are available for that removal.
class UnmounterOptionCheck {

	private UnmounterOptionCheck() {
	}

	/// Checks if the given library can be removed from the given database and
	/// returns the retention options that are available for the removal. If the
	/// library cannot be removed, an error message is shown and an empty
	/// option is returned.
	static Optional<EnumSet<Unmounter.Retention>> run(
		Library library, IDatabase db
	) {
		if (library == null || db == null)
			return Optional.empty();

		// a library that is a dependency of another library that is mounted
		// to this database cannot be removed
		if (isDependencyOfMountedLibrary(library, db)) {
			MsgBox.error("Cannot remove library from database",
				"The library is a dependency of another library "
					+ "mounted to this database.");
			return Optional.empty();
		}

		// check if data sets of the library are used (referenced) by data
		// sets that are not from that library
		boolean used = isUsed(library.name(), db);
		boolean signed = License.of(library.folder()).isPresent();

		if (signed) {
			// a signed library can only be removed when it is not used as
			// the keep options would copy the licensed data into the database
			if (used) {
				MsgBox.error("Cannot remove library from database",
					"The library is signed and used by data sets in this database.");
				return Optional.empty();
			}
			return Optional.of(EnumSet.of(Unmounter.Retention.KEEP_NONE));
		}

		return Optional.of(used
			? EnumSet.of(
				Unmounter.Retention.KEEP_USED,
				Unmounter.Retention.KEEP_ALL)
			: EnumSet.of(
				Unmounter.Retention.KEEP_NONE,
				Unmounter.Retention.KEEP_ALL));
	}

	/// Returns true if the given library is a direct dependency of another
	/// library that is mounted to the given database. As all dependencies of a
	/// mounted library are mounted too, it is enough to check the direct
	/// dependencies of the mounted libraries here.
	private static boolean isDependencyOfMountedLibrary(
		Library library, IDatabase db
	) {
		var lib = library.name();
		var libDir = Workspace.getLibraryDir();
		for (var id : db.getLibraries()) {
			if (Objects.equals(id, lib))
				continue;
			var other = libDir.getLibrary(id).orElse(null);
			if (other == null)
				continue;
			for (var dep : other.getDirectDependencies()) {
				if (Objects.equals(dep.name(), lib))
					return true;
			}
		}
		return false;
	}

	/// Returns true if there is at least one data set in the database that is
	/// not from the given library but references a data set of that library.
	private static boolean isUsed(String lib, IDatabase db) {
		return new UsageCheck(lib, ModelReferences.scan(db)).scan(db);
	}

	private static final class UsageCheck {

		private final String lib;
		private final ModelReferences refs;

		private UsageCheck(String lib, ModelReferences refs) {
			this.lib = lib;
			this.refs = refs;
		}

		private boolean scan(IDatabase db) {
			var used = new AtomicBoolean(false);
			for (var type : ModelType.values()) {
				if (type == ModelType.CATEGORY)
					continue;
				for (var d : Daos.root(db, type).getDescriptors()) {
					if (!Objects.equals(lib, d.library))
						continue;
					// the library is used if any data set d from that library
					// is used from another data set u that is not from that
					// library
					var ref = new TypedRefId(d.type, d.refId);
					refs.iterateUsages(ref, u -> {
						if (!Objects.equals(lib, u.library)) {
							used.set(true);
							return false;
						}
						return true;
					});
					if (used.get())
						return true;
				}
			}
			return used.get();
		}
	}
}

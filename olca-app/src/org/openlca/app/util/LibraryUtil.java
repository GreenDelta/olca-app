package org.openlca.app.util;

import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class LibraryUtil {

	private LibraryUtil() {
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

	private static void fill(RootEntity e, BiConsumer<IDatabase, Library> fn) {
		if (e == null || !e.isFromLibrary())
			return;
		var db = Database.get();
		var lib = Workspace.getLibraryDir()
			.getLibrary(e.library)
			.orElse(null);
		if (db == null || lib == null)
			return;
		fn.accept(db, lib);
	}

}

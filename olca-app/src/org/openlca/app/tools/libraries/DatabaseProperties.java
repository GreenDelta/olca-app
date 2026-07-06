package org.openlca.app.tools.libraries;

import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProviderType;
import org.openlca.util.Databases;

/// Collects database properties for a possible library export
record DatabaseProperties(
	IDatabase db,
	boolean hasResultProviderLinks,
	boolean hasLibraryProcesses,
	boolean hasInventory,
	boolean hasImpacts,
	boolean hasUncertainty,
	DQSystem flowDqs
) {

	static DatabaseProperties of(IDatabase db) {
		if (db == null)
			throw new IllegalArgumentException("No database provided");

		if (hasResultProviderLinks(db)) {
			return new DatabaseProperties(db, true, false, false, false, false, null);
		}

		boolean hasLibraryProcesses = false;
		boolean hasInventory = false;
		for (var d : db.getDescriptors(Process.class)) {
			hasInventory = true;
			if (Strings.isNotBlank(d.library)) {
				hasLibraryProcesses = true;
				break;
			}
		}

		boolean hasImpacts = false;
		for (var d : db.getDescriptors(ImpactCategory.class)) {
			if (Strings.isBlank(d.library)) {
				hasImpacts = true;
				break;
			}
		}

		return new DatabaseProperties(
			db,
			false,
			hasLibraryProcesses,
			hasInventory,
			hasImpacts,
			Databases.hasUncertaintyData(db),
			Databases.getCommonFlowDQS(db).orElse(null));
	}

	private static boolean hasResultProviderLinks(IDatabase db) {
		var sql = "select f_default_provider from tbl_exchanges where "
			+ "default_provider_type = " + ProviderType.RESULT
			+ " and f_default_provider <> 0";
		var found = new AtomicBoolean(false);
		NativeSql.on(db).query(sql, _ -> {
			found.set(true);
			return false;
		});
		return found.get();
	}
}

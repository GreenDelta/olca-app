package org.openlca.app.db;

import org.openlca.core.database.IDatabase;

public interface IDatabaseConfiguration {

	IDatabase connect();

	String getName();

	boolean isLocal();

}

package org.openlca.app.db;

import org.openlca.core.database.IDatabase;

public interface DatabaseConfig {

	IDatabase connect();

	String name();

	boolean isEmbedded();

}

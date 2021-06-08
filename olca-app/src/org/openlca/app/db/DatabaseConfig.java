package org.openlca.app.db;

import java.io.File;

import org.openlca.core.database.IDatabase;

public interface DatabaseConfig {

	IDatabase connect(File databaseFolder);

	String name();

	boolean isEmbedded();

}

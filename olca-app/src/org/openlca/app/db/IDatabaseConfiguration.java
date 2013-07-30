package org.openlca.app.db;

import org.openlca.core.database.IDatabase;

public interface IDatabaseConfiguration {

	public IDatabase createInstance() throws Exception;

	public String getName();

	public boolean isLocal();

}

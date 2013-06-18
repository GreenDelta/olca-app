package org.openlca.core.application.db;

import org.openlca.core.database.IDatabase;

public interface IDatabaseConfiguration {

	public IDatabase createInstance() throws Exception;

	public String getName();

	public boolean isLocal();

}

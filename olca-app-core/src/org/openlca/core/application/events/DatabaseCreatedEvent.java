package org.openlca.core.application.events;

import org.openlca.core.database.IDatabase;

public class DatabaseCreatedEvent {

	private IDatabase database;

	public DatabaseCreatedEvent(IDatabase database) {
		this.database = database;
	}

	public IDatabase getDatabase() {
		return database;
	}

}

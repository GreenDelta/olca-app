package org.openlca.app.events;


public class DatabaseEvent {

	public final String database;
	public final Type type;

	public DatabaseEvent(String database, Type type) {
		this.database = database;
		this.type = type;
	}
	
	public static enum Type implements TypeEnum {
		CREATE, DELETE, ACTIVATE, CLOSE;
	}

}

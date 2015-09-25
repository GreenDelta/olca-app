package org.openlca.app.events;


public class DatabaseEvent {

	private String database;
	private Type type;

	public DatabaseEvent(String database, Type type) {
		this.database = database;
		this.type = type;
	}

	public String getDatabase() {
		return database;
	}

	public Type getType() {
		return type;
	}
	
	public boolean isOneOf(Type... types) {
		if (types == null)
			return false;
		for (Type type : types)
			if (this.type == type)
				return true;
		return false;
	}
	
	public static enum Type {
		CREATE, DELETE, ACTIVATE, CLOSE;
	}

}

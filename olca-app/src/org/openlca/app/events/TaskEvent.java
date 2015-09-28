package org.openlca.app.events;

public class TaskEvent {
	
	public final Type type;
	
	public TaskEvent(Type type) {
		this.type = type;
	}
	
	public static enum Type implements TypeEnum {
		
		IMPORT_STARTED, IMPORT_STOPPED, PASTE_STARTED, PASTE_STOPPED;
		
	}

}

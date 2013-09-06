package org.openlca.app;

public class Event {

	private String message;
	private Object source;

	public Event(String message, Object source) {
		this.message = message;
		this.source = source;
	}

	public String getMessage() {
		return message;
	}

	public Object getSource() {
		return source;
	}

}

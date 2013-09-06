package org.openlca.app;

import java.util.Objects;

public class Event {

	private String message;
	private Object source;

	public Event(String message, Object source) {
		this.message = message;
		this.source = source;
	}

	public boolean match(String message) {
		return Objects.equals(message, this.message);
	}

	public String getMessage() {
		return message;
	}

	public Object getSource() {
		return source;
	}

}

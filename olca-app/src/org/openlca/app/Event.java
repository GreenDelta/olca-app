package org.openlca.app;

import java.util.Objects;

public class Event {

	public final String id;
	public final Object sender;

	public Event(String id, Object sender) {
		this.id = id;
		this.sender = sender;
	}

	public boolean matches(String id) {
		return Objects.equals(id, this.id);
	}
}

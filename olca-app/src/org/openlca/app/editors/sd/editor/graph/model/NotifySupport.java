package org.openlca.app.editors.sd.editor.graph.model;

import java.util.ArrayList;
import java.util.List;

public interface NotifySupport {

	Notifier notifier();

	static void on(NotifySupport host, Runnable listener) {
		if (host != null && listener != null) {
			host.notifier().add(listener);
		}
	}

	static void off(NotifySupport host, Runnable listener) {
		if (host != null && listener != null) {
			host.notifier().remove(listener);
		}
	}

	class Notifier {

		private final List<Runnable> listeners = new ArrayList<>();

		public void add(Runnable listener) {
			listeners.add(listener);
		}

		public void remove(Runnable listener) {
			listeners.remove(listener);
		}

		public void fire() {
			for (var r : listeners) {
				r.run();
			}
		}

	}

}

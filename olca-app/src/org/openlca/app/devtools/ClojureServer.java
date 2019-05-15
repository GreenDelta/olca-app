package org.openlca.app.devtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public final class ClojureServer {

	private ClojureServer() {
	}

	public static void start(int port) {
		Logger log = LoggerFactory.getLogger(ClojureServer.class);
		log.info("start Clojure REPL server at port {}", port);
		try {
			IFn require = Clojure.var("clojure.core", "require");
			require.invoke(Clojure.read("clojure.tools.nrepl.server"));
			IFn server = Clojure.var("clojure.tools.nrepl.server", "start-server");
			server.invoke(Clojure.read(":port"), Clojure.read(Integer.toString(port)));
			log.info("Clojure REPL server started; run `lein repl :connect {}`"
					+ " to connect with Leiningen", port);
		} catch (Exception e) {
			log.error("failed to start Clojure REPL server at " + port, e);
		}
	}

}

package org.openlca.app.devtools;

import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaScript {

	public static void eval(String script) {
		try {
			StringBuilder scriptBuilder = new StringBuilder();
			Properties properties = new Properties();
			properties.load(JavaScript.class
					.getResourceAsStream("bindings.properties"));

		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(JavaScript.class);
			log.error("failed to initialize the JavaScript engine", e);
			new ScriptEngineManager()
					.getEngineByName("nashorn");
		}

	}

	public static void main(String[] args) {
		String script = "var Process = Java.type('org.openlca.core.model.Process');"
				+ "var process = new Process();"
				+ "process.setName('my process');"
				+ "print(process.getName());";

		ScriptEngine engine = new ScriptEngineManager()
				.getEngineByName("nashorn");
		try {
			engine.eval(script);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

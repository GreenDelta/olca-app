package org.openlca.app.devtools.js;

import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Properties;

public class JavaScript {

	public static void eval(String script) {
		try {
			String fullScript = prependTypeDeclarations(script);
			Bindings bindings = createBindings();
			ScriptEngine engine = new ScriptEngineManager()
					.getEngineByName("nashorn");
			engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
			engine.eval(fullScript);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(JavaScript.class);
			log.error("failed to evaluate JavaScript", e);
		}
	}

	private static Bindings createBindings() {
		Bindings bindings = new SimpleBindings();
		bindings.put("log", LoggerFactory.getLogger(JavaScript.class));
		bindings.put("app", App.class);
		if (Database.get() != null)
			bindings.put("db", Database.get());
		return bindings;
	}

	private static String prependTypeDeclarations(String script) throws IOException {
		StringBuilder builder = new StringBuilder();
		Properties properties = new Properties();
		properties.load(JavaScript.class
				.getResourceAsStream("bindings.properties"));
		properties.forEach((name, fullName) -> {
			builder.append("var ").append(name)
					.append(" = Java.type('").append(fullName)
					.append("');\n");
		});
		builder.append(script);
		return builder.toString();
	}
}

package org.openlca.core.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for retrieving the arguments from the command line or the ini-file
 * that are passed into an Eclipse application.
 */
public class EclipseCommandLine {

	private EclipseCommandLine() {
	}

	public static boolean hasArg(String arg) {
		return getArgs().containsKey(arg);
	}

	public static String getArg(String arg) {
		return getArgs().get(arg);
	}

	public static Map<String, String> getArgs() {
		String text = System.getProperty("eclipse.commands");
		return getArgs(text);
	}

	public static Map<String, String> getArgs(String text) {
		try {
			return tryGetArgs(text);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(EclipseCommandLine.class);
			log.error("Get args failed", e);
			return new HashMap<>();
		}
	}

	private static Map<String, String> tryGetArgs(String text)
			throws IOException {
		Map<String, String> map = new HashMap<>();
		StringReader reader = new StringReader(text);
		BufferedReader buffer = new BufferedReader(reader);
		String line = null;
		String param = null;
		while ((line = buffer.readLine()) != null) {
			if (line.startsWith("-")) {
				param = line.substring(1).trim();
				map.put(param, "");
			} else if (param != null) {
				map.put(param, line.trim());
				param = null;
			}
		}
		return map;
	}

}

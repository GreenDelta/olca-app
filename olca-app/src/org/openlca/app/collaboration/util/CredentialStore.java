package org.openlca.app.collaboration.util;

import java.util.HashMap;
import java.util.Map;

import org.openlca.util.Strings;

public class CredentialStore {

	private static Map<String, Map<String, String>> passwords = new HashMap<>();
	private static Map<String, String> usernames = new HashMap<>();

	public static String getUsername(String url) {
		return usernames.get(url);
	}


	public static String getPassword(String url, String username) {
		return passwords.getOrDefault(url, new HashMap<>()).get(username);
	}

	public static void put(String url, String username, String password) {
		if (Strings.nullOrEmpty(username))
			return;
		usernames.put(url, username);
		passwords.computeIfAbsent(url, u -> new HashMap<>()).put(username, password);
	}

	public static void clearPassword(String url, String username) {
		if (Strings.nullOrEmpty(username))
			return;
		var list = passwords.get(url);
		if (list == null)
			return;
		list.remove(username);
		if (list.isEmpty()) {
			passwords.remove(url);
		}
	}

	public static void clear(String url) {
		passwords.remove(url);
		usernames.remove(url);
	}

}

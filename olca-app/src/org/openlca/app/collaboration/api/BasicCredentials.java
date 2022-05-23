package org.openlca.app.collaboration.api;

import org.openlca.app.db.Repository;
import org.openlca.app.util.Input;

public record BasicCredentials(String username, String password, Integer token) {

	public static BasicCredentials prompt() {
		var username = Repository.get() != null && Repository.get().user != null
				? Repository.get().user.getName()
				: Input.promptString("Username", "Please enter your username", "");
		if (username == null)
			return null;
		var password = Input.promptPassword("Password", "Please enter your password", "");
		if (password == null)
			return null;
		var token = Input.prompt("Token", "Please enter your token", "", v -> Integer.parseInt(v), v -> {
			try {
				if (v == null || v.length() != 6)
					return "Token must be a 6 digit number";
				Integer.parseInt(v);
			} catch (NumberFormatException e) {
				return "Token must be a 6 digit number";
			}
			return null;
		});
		return new BasicCredentials(username, password, token);
	}

}

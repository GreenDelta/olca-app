package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.dialogs.TokenDialog;
import org.openlca.app.util.Input;
import org.openlca.util.Strings;

public class InMemoryCredentialSupplier implements CredentialSupplier {

	private String username;
	private String password;

	@Override
	public String username() {
		if (Strings.nullOrEmpty(username)) {
			username = Input.promptString("Enter username", "Please enter your username to proceed", "");
		}
		return username;
	}

	@Override
	public String password() {
		if (password == null) {
			password = Input.promptString("Enter password", "Please enter your password to proceed", "");
		}
		return password;
	}

	@Override
	public Integer token() {
		return new TokenDialog().open();
	}

}

package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.ui.dialogs.TokenDialog;
import org.openlca.app.util.Input;

public class InMemoryCredentialSupplier implements CredentialSupplier {

	private String username;
	private String password;

	@Override
	public String username() {
		if (username == null) {
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

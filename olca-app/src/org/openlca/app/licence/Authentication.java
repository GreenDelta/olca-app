package org.openlca.app.licence;

public class Authentication {

	public static boolean login(String library) {
		var credentials = AuthenticationDialog.promptCredentials(library);
		return credentials != null;
	}

}

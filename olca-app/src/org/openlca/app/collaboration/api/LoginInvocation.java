package org.openlca.app.collaboration.api;

import java.util.HashMap;

import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Repository;
import org.openlca.util.Strings;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a web service call to login
 */
class LoginInvocation {

	private static final String PATH = "public/login";
	String baseUrl;
	GitCredentialsProvider credentials;

	String execute() throws WebRequestException {
		var response = _execute(credentials.token);
		if (response.getStatus() != Status.OK.getStatusCode())
			return null;
		var result = response.getEntity(String.class);
		var repo = Repository.get();
		if ("tokenRequired".equals(result)) {
			repo.useTwoFactorAuth(true);
			var auth = AuthenticationDialog.promptToken(credentials.user, credentials.password);
			if (auth == null)
				return null;
			response = _execute(auth.token);
		} else if (Strings.nullOrEmpty(credentials.token) && repo != null) {
			repo.useTwoFactorAuth(false);
		}
		for (var cookie : response.getCookies())
			if (cookie.getName().equals("JSESSIONID"))
				return cookie.getValue();
		return null;
	}

	private ClientResponse _execute(String token) throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(credentials.user, "username");
		Valid.checkNotEmpty(credentials.password, "password");
		var url = baseUrl + "/" + PATH;
		var data = new HashMap<String, String>();
		data.put("username", credentials.user);
		data.put("password", credentials.password);
		if (!Strings.nullOrEmpty(token)) {
			data.put("token", token);
		}
		return WebRequests.call(Type.POST, url, null, data);
	}

}

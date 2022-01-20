package org.openlca.app.collaboration.api;

import java.util.HashMap;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a web service call to login
 */
class LoginInvocation {

	private static final String PATH = "/public/login";
	String baseUrl;
	CredentialSupplier credentials;

	/**
	 * Login with the specified credentials
	 * 
	 * @throws WebRequestException
	 *             if the credentials were invalid or the user is already logged
	 *             in
	 */
	String execute() throws WebRequestException {
		var response = _execute(null);
		if (response.getStatus() != Status.OK.getStatusCode())
			return null;
		var result = response.getEntity(String.class);
		if ("tokenRequired".equals(result)) {
			var token = credentials.token();
			if (token == null)
				return null;
			response = _execute(token);
		}
		for (var cookie : response.getCookies())
			if (cookie.getName().equals("JSESSIONID"))
				return cookie.getValue();
		return null;
	}

	private ClientResponse _execute(Integer token) throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(credentials.username(), "username");
		Valid.checkNotEmpty(credentials.password(), "password");
		var url = baseUrl + PATH;
		var data = new HashMap<String, String>();
		data.put("username", credentials.username());
		data.put("password", credentials.password());
		if (token != null) {
			data.put("token", token.toString());
		}
		return WebRequests.call(Type.POST, url, null, data);
	}

}

package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;

/**
 * Invokes a web service call to logout
 */
class LogoutInvocation {

	private final static String PATH = "/public/logout";
	String baseUrl;
	String sessionId;

	/**
	 * Terminate the current user session
	 * 
	 * @throws WebRequestException
	 *             if the user was not logged in
	 */
	void execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(sessionId, "session id");
		var url = baseUrl + PATH;
		WebRequests.call(Type.POST, url, sessionId);
	}
}

package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.model.Announcement;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a webservice call to check for server announcements
 */
class AnnouncementInvocation {

	private static final String PATH = "/public/announcements";
	String baseUrl;
	String sessionId;

	Announcement execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		var response = WebRequests.call(Type.GET, baseUrl + PATH, sessionId);
		if (response.getStatus() == Status.NO_CONTENT.getStatusCode())
			return null;
		return new Gson().fromJson(response.getEntity(String.class), Announcement.class);
	}

}

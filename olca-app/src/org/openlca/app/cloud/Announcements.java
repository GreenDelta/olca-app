package org.openlca.app.cloud;

import org.openlca.app.cloud.ui.preferences.CloudPreference;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.Announcement;
import org.openlca.cloud.util.WebRequests.WebRequestException;

import com.sun.jersey.api.client.ClientResponse.Status;

public class Announcements {

	public static void check(RepositoryClient client) {
		try {
			Announcement announcement = client.getAnnouncement();
			if (announcement == null || announcement.message == null || announcement.message.isEmpty())
				return;
			if (CloudPreference.didReadAnnouncement(client.getConfig().baseUrl, announcement.id))
				return;
			Info.showBox("LCA Collaboration Server announcement", announcement.message);
			CloudPreference.markAnnouncementAsRead(client.getConfig().baseUrl, announcement.id);
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.NOT_FOUND.getStatusCode())
				return; // ignore, older servers don't provide announcement resource
			Error.showBox(e.getMessage());
		}
	}
	
}

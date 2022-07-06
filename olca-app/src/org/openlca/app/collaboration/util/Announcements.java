package org.openlca.app.collaboration.util;

import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Repository;
import org.openlca.app.util.MsgBox;

import com.sun.jersey.api.client.ClientResponse.Status;

public class Announcements {

	public static void check() {
		var repo = Repository.get();
		if (!Repository.isConnected() || !repo.isCollaborationServer())
			return;
		try {
			var client = repo.client;
			var announcement = client.getAnnouncement();
			if (announcement == null || announcement.message() == null || announcement.message().isEmpty())
				return;
			if (CollaborationPreference.didReadAnnouncement(repo.client.serverUrl, announcement.id()))
				return;
			MsgBox.error("LCA Collaboration Server announcement", announcement.message());
			CollaborationPreference.markAnnouncementAsRead(repo.client.serverUrl, announcement.id());
		} catch (WebRequestException e) {
			// ignore, older servers don't provide announcement resource
			if (e.getErrorCode() == Status.NOT_FOUND.getStatusCode())
				return;
			MsgBox.error(e.getMessage());
		}
	}

}

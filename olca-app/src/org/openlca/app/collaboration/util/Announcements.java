package org.openlca.app.collaboration.util;

import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Repository;
import org.openlca.app.util.MsgBox;

import com.sun.jersey.api.client.ClientResponse.Status;

public class Announcements {

	public static void check() {
		if (!Repository.isConnected() || !Repository.get().isCollaborationServer())
			return;
		try {
			var client = Repository.get().client;
			var announcement = client.getAnnouncement();
			if (announcement == null || announcement.message() == null || announcement.message().isEmpty())
				return;
			if (CollaborationPreference.didReadAnnouncement(client.config.serverUrl, announcement.id()))
				return;
			MsgBox.error("LCA Collaboration Server announcement", announcement.message());
			CollaborationPreference.markAnnouncementAsRead(client.config.serverUrl, announcement.id());
		} catch (WebRequestException e) {
			// ignore, older servers don't provide announcement resource
			if (e.getErrorCode() == Status.NOT_FOUND.getStatusCode())
				return;
			MsgBox.error(e.getMessage());
		}
	}

}

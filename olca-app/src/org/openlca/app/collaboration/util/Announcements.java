package org.openlca.app.collaboration.util;

import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.db.Repository;
import org.openlca.app.util.MsgBox;

public class Announcements {

	public static void check() {
		var repo = Repository.get();
		if (!Repository.isConnected() || !repo.isCollaborationServer())
			return;
		var client = repo.client;
		var announcement = client.getAnnouncement();
		if (announcement == null || announcement.message() == null || announcement.message().isEmpty())
			return;
		if (CollaborationPreference.didReadAnnouncement(repo.client.serverUrl, announcement.id()))
			return;
		MsgBox.info("LCA Collaboration Server announcement", announcement.message());
		CollaborationPreference.markAnnouncementAsRead(repo.client.serverUrl, announcement.id());
	}

}

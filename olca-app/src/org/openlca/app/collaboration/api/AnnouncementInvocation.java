package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.model.Announcement;
import org.openlca.app.collaboration.util.WebRequests.Type;

/**
 * Invokes a webservice call to check for server announcements
 */
final class AnnouncementInvocation extends Invocation<Announcement, Announcement> {

	AnnouncementInvocation() {
		super(Type.GET, "public/announcements", Announcement.class);
	}

}

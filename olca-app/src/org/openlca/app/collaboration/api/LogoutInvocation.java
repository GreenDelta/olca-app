package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.util.WebRequests.Type;

/**
 * Invokes a web service call to logout
 */
class LogoutInvocation extends Invocation<Void, Void> {

	LogoutInvocation() {
		super(Type.POST, "public/logout", Void.class);
	}

}

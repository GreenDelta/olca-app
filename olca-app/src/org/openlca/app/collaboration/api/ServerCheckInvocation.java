package org.openlca.app.collaboration.api;

import java.util.Map;

import org.openlca.app.collaboration.util.WebRequests.Type;

import com.google.gson.reflect.TypeToken;

public class ServerCheckInvocation extends Invocation<Map<String, Integer>, Boolean> {

	protected ServerCheckInvocation() {
		super(Type.GET, "public", new TypeToken<Map<String, Integer>>() {
		});
	}

	@Override
	protected Boolean process(Map<String, Integer> currentUser) {
		return currentUser != null && currentUser.get("id") == 0;
	}

}

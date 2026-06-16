package org.openlca.app.tools.hestia;

import java.util.List;

import org.openlca.app.tools.ApiKeyAuth;
import org.openlca.commons.Res;
import org.openlca.io.hestia.HestiaClient;
import org.openlca.io.hestia.Release;
import org.openlca.io.hestia.User;

record HestiaUser(HestiaClient client, User user, List<Release> releases) {

	static Res<HestiaUser> get(ApiKeyAuth.ApiKey key) {
		var client = HestiaClient.of(key.endpoint(), key.value());
		var user = client.getCurrentUser();
		if (user.isError())
			return user.wrapError("Failed to get user information");
		var releaseRes = client.getReleases();
		if (releaseRes.isError())
			return user.wrapError("Failed to get data releases.");
		var releases = releaseRes.value();
		if (releases.isEmpty())
			return Res.error("The HESTIA API did not return any data release version");
		return Res.ok(new HestiaUser(client, user.value(), releases));
	}

}

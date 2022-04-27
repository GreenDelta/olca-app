package org.openlca.app.tools.openepd;

import java.io.File;
import java.net.URI;

import org.openlca.app.rcp.Workspace;
import org.openlca.io.openepd.Ec3Credentials;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

public final class Ec3 {

	private Ec3() {
	}

	public static String baseUrl() {
		var url = credentials().ec3Url();
		if (Strings.nullOrEmpty(url))
			return "https://buildingtransparency.org";
		try {
			var uri = new URI(url);
			var base = uri.getScheme() + "://" + uri.getHost();
			var port = uri.getPort();
			return port != -1
				? base + ":" + port
				: base;
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Ec3.class);
			log.error("failed to parse EC3 URL " + url, e);
			return "https://buildingtransparency.org";
		}
	}

	public static String displayUrl(String epdId) {
		return baseUrl() + "/ec3/epds/" + epdId;
	}

	public static Ec3Credentials credentials() {
		return Ec3Credentials.getDefault(credentialsFile());
	}

	public static void save(Ec3Credentials cred) {
		if (cred == null)
			return;
		cred.save(credentialsFile());
	}

	private static File credentialsFile() {
		return new File(Workspace.root(), ".ec3");
	}

}

package org.openlca.app.rcp.update;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;

public class UpdateCheckService {
	private static final Logger log = LoggerFactory
			.getLogger(UpdateCheckService.class);

	public VersionInfo loadNewestVersionFromServer(String serverRoot)
			throws RuntimeException, IntermittentConnectionFailure {
		VersionInfo retval = null;
		Client c = ApacheHttpClient.create();
		WebResource r2 = c.resource(serverRoot + "openlca.json");
		try (InputStream s = r2.get(InputStream.class)) {
			log.debug("Inputstream for openlca.json null? " + (s == null));

			ObjectMapper mapper = new ObjectMapper();
			Map<?, ?> versionsByArch = mapper.readValue(s, Map.class);
			String ownFlavor = Platform.getOS() + "." + Platform.getOSArch();
			log.debug("Looking for available version for {}", ownFlavor);
			Map<?, ?> newVersionMap = (Map<?, ?>) versionsByArch.get(ownFlavor);

			if (newVersionMap != null) {
				retval = mapper.convertValue(newVersionMap, VersionInfo.class);
				// normalize the URL if it's relative
				if (!Strings.isNullOrEmpty(retval.getDownloadUrl())) {
					try {
						new URL(retval.getDownloadUrl());
					} catch (MalformedURLException mue) {
						// relative - canonicalize
						retval.setDownloadUrl(new URL(new URL(serverRoot),
								retval.getDownloadUrl()).toString());
					}
				}
				log.debug("Found newest version: {}", newVersionMap);
			} else {
				log.debug("Now current version for my flavor {}", ownFlavor);
			}
		} catch (ClientHandlerException che) {
			throw new IntermittentConnectionFailure(
					"Update server connection failure", che);
		} catch (Exception e) {
			log.debug("Update server loading of newest version", e);
			throw new RuntimeException(e);
		}
		return retval;
	}
}

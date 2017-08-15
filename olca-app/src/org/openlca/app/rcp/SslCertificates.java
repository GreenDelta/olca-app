package org.openlca.app.rcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openlca.cloud.util.Ssl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SslCertificates {

	private static final Logger log = LoggerFactory.getLogger(SslCertificates.class);

	static void load() {
		File workspace = Workspace.getDir();
		File certificates = new File(workspace, "ssl-certificates");
		if (!certificates.exists() || !certificates.isDirectory()) {
			log.debug("No certificates found in workspace");
			return;
		}
		int count = 0;
		for (File file : certificates.listFiles()) {
			if (file.isDirectory())
				continue;
			try (InputStream stream = new FileInputStream(file)) {
				Ssl.addCertificate("custom-cert-" + ++count, stream);
			} catch (IOException e) {
				log.error("Error loading certificate", e);
			}
		}
		log.debug("Added " + count + " certificates");
	}

}

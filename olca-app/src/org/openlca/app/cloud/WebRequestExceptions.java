package org.openlca.app.cloud;

import java.security.cert.X509Certificate;

import org.openlca.app.preferences.SslCertificateUtil;
import org.openlca.app.util.MsgBox;
import org.openlca.cloud.util.WebRequests.WebRequestException;

public class WebRequestExceptions {

	public static boolean handle(WebRequestException e) {
		if (!e.isSslCertificateException()) {
			MsgBox.error(e.getMessage());
		} else {
			MsgBox.question("SSL Certificate unknown",
					"The site " + e.getHost() + " you are trying to connect to uses an unknown SSL certificate. "
							+ "Do you want to add the certificate to the list of trusted certificates? "
							+ "You will need to rerun the current action to continue after adding the certificate",
					trust -> {
						if (trust) {
							X509Certificate cert = SslCertificateUtil.downloadCertificate(e.getHost(), e.getPort());
							SslCertificateUtil.importCertificate(cert, e.getHost());
						}
					});
		}
		return false;
	}

}

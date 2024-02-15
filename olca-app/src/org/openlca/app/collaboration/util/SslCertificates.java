package org.openlca.app.collaboration.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.openlca.app.rcp.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslCertificates {

	private final static Logger log = LoggerFactory.getLogger(SslCertificates.class);

	public static X509Certificate downloadCertificate(String host, int port) {
		try {
			var context = SSLContext.getInstance("TLS");
			var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(Ssl.getKeyStore());
			var defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
			var tm = new SavingTrustManager(defaultTrustManager);
			context.init(null, new TrustManager[] { tm }, null);
			var factory = context.getSocketFactory();
			var socket = (SSLSocket) factory.createSocket(host, port);
			socket.setSoTimeout(10000);
			try {
				socket.startHandshake();
				socket.close();
				return null;
			} catch (SSLException e) {
			}
			var chain = tm.chain;
			if (chain == null || chain.length == 0)
				return null;
			return chain[0];
		} catch (Exception e) {
			log.error("Error downloading ssl certificate from " + host + ":" + port, e);
			return null;
		}
	}

	public static void importCertificate(X509Certificate cert, String host) {
		try {
			var certDir = new File(Workspace.root(), "ssl-certificates");
			if (!certDir.exists()) {
				certDir.mkdirs();
			}
			var out = new ByteArrayOutputStream();
			out.write(("-----BEGIN CERTIFICATE-----" + "\r\n").getBytes("utf-8"));
			out.write(Base64.getMimeEncoder().encode(cert.getEncoded()));
			out.write("\r\n-----END CERTIFICATE-----".getBytes("utf-8"));
			var certFile = new File(certDir, host + ".pem");
			Files.write(certFile.toPath(), out.toByteArray());
			Ssl.addCertificate(host, cert);
		} catch (Exception e) {
			log.error("Error importing ssl certificate", e);
		}
	}

	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;
		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {
			throw new UnsupportedOperationException();
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}

	}

}
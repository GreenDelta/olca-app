package org.openlca.app.preferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.openlca.app.rcp.Workspace;
import org.openlca.cloud.util.Ssl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

public class SslCertificateUtil {

	private final static Logger log = LoggerFactory.getLogger(SslCertificateUtil.class);

	public static X509Certificate downloadCertificate(String host, int port) {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(Ssl.getKeyStore());
			X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
			SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
			context.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory factory = context.getSocketFactory();
			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
			socket.setSoTimeout(10000);
			try {
				socket.startHandshake();
				socket.close();
				return null;
			} catch (SSLException e) {
			}
			X509Certificate[] chain = tm.chain;
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
			File certDir = new File(Workspace.getDir(), "ssl-certificates");
			if (!certDir.exists()) {
				certDir.mkdirs();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BASE64Encoder encoder = new BASE64Encoder();
			out.write((X509Factory.BEGIN_CERT + "\r\n").getBytes("utf-8"));
			encoder.encodeBuffer(cert.getEncoded(), out);
			out.write(X509Factory.END_CERT.getBytes("utf-8"));
			File certFile = new File(certDir, host + ".pem");
			Files.write(out.toByteArray(), certFile);
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

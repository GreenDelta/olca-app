package org.openlca.app.collaboration.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ssl {

	private static final Logger log = LoggerFactory.getLogger(Ssl.class);
	private static KeyStore keyStore;
	private static CertificateFactory certificateFactory;
	private static TrustManagerFactory trustManagerFactory;

	static {
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
			keyStore = loadKeyStore();
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		} catch (Exception e) {
			certificateFactory = null;
			keyStore = null;
			trustManagerFactory = null;
			log.error("Error initializing Ssl util", e);
		}
	}

	static SSLContext createContext() {
		if (trustManagerFactory == null)
			return null;
		try {
			trustManagerFactory.init(keyStore);
			var context = SSLContext.getInstance("TLS");
			context.init(null, trustManagerFactory.getTrustManagers(), null);
			return context;
		} catch (Exception e) {
			return null;
		}
	}

	public static void addCertificate(String name, InputStream stream) {
		try {
			var certificate = certificateFactory.generateCertificate(stream);
			addCertificate(name, certificate);
		} catch (Exception e) {
			log.error("Error loading certificate", e);
		}
	}

	public static void addCertificate(String name, Certificate certificate) {
		try {
			keyStore.setCertificateEntry(name, certificate);
		} catch (Exception e) {
			log.error("Error adding certificate to keystore", e);
		}
	}

	public static void removeCertificate(String name) {
		try {
			keyStore.deleteEntry(name);
		} catch (Exception e) {
			log.error("Error removing certificate from keystore", e);
		}
	}

	private static KeyStore loadKeyStore() throws Exception {
		var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		var path = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
		keyStore.load(Files.newInputStream(path), "changeit".toCharArray());
		return keyStore;
	}

	public static KeyStore getKeyStore() {
		return keyStore;
	}

}

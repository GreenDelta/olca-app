package org.openlca.app;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Main {

	private static final KeyStore keyStore = initKeyStore();
	private static final Set<String> CNS = initCNS();

	public static void main(String[] args) throws Exception {
		 var https_url =
		 "https://lca-colabsv.deimos-space.com/TestGroup/DemoV3";
		initTrust();
//		var https_url = "https://collab.greendelta.com/zimmermann/ECCC_Collab_Demo";
		var url = new URL(https_url);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		print_https_cert(con);
	}

	private static KeyStore initKeyStore() {
		try {
			var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			var path = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
			keyStore.load(Files.newInputStream(path), "changeit".toCharArray());
			return keyStore;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Set<String> initCNS() {
		try {
			var cns = new HashSet<String>();
			var aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				var cert = (X509Certificate) keyStore.getCertificate(aliases.nextElement());
				cns.add(cert.getSubjectX500Principal().getName());
			}
			return cns;
		} catch (Exception e) {
			e.printStackTrace();
			return new HashSet<String>();
		}
	}

	private static void print_https_cert(HttpsURLConnection con) throws Exception {
		con.getResponseCode();
		var certs = con.getServerCertificates();
		for (var c : certs) {
			var cert = (X509Certificate) c;
			var subject = cert.getSubjectX500Principal().getName();
			var issuer = cert.getIssuerX500Principal().getName();
			System.out.println("Subject: " + subject + " - isInTrustore: " + CNS.contains(subject));
			System.out.println("Validity: " + cert.getNotBefore() + " - " + cert.getNotAfter());
			System.out.println("Issuer: " + issuer + " - isInTrustore: " + CNS.contains(issuer));
			System.out.println();
		}
	}

	private static void initTrust() throws Exception {
		var trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		var sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		var allHostsValid = new HostnameVerifier() {

			@Override
			public boolean verify(String hostname, SSLSession session) {

				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}
}
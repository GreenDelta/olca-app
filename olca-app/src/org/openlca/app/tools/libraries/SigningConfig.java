package org.openlca.app.tools.libraries;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.library.Library;
import org.openlca.license.certificate.Person;

class SigningConfig {

	File output;
	File certificateDir;
	Library library;
	String email;
	char[] password;
	Date validFrom = Calendar.getInstance().getTime();
	Date validUntil = Calendar.getInstance().getTime();

	Person subject() {
		return new Person(
			name(),
			name(),
			country(),
			email,
			"");
	}

	String name() {
		if (Strings.isBlank(email))
			return "";
		if (!email.contains("@"))
			return email;
		return email.substring(0, email.indexOf("@"));
	}

	String country() {
		var locale = Locale.getDefault();
		return locale == null
			? ""
			: locale.getCountry();
	}

	String getDefaultName() {
		return library != null ? library.name() + "-signed.zip" : null;

	}

	Date notBefore() {
		var cal = Calendar.getInstance();
		cal.setTime(validFrom);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	Date notAfter() {
		var cal = Calendar.getInstance();
		cal.setTime(validUntil);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal.getTime();
	}

	static Res<Void> validateCertificateFolder(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return Res.error("The given folder is not a directory: " + dir);
		var crtFile = new File(dir, dir.getName() + ".crt");
		if (!crtFile.exists() || !crtFile.isFile())
			return Res.error("The certificate file does not exist: " + crtFile);
		var privateDir = new File(dir, "private");
		var keyFile = new File(privateDir, dir.getName() + ".key");
		return keyFile.exists() && keyFile.isFile()
			? Res.error("The private key does not exist: " + keyFile)
			: Res.ok();
	}
}

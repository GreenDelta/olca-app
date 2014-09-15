package org.openlca.app.rcp.browser;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is for Windows 64bit only:
 * <p>
 * Checks if a Visual Studio C++ Runtime v10 64bit is installed on the system
 * (see http://www.microsoft.com/en-us/download/details.aspx?id=14632). We need
 * this to test if we can use the 64bit XulRunner v10 as it requires this
 * runtime to be installed (see http://wiki.mozilla-x86-64.com/Download).
 * <p>
 * We may can remove this when we can switch to the 64bit XulRunner v24 / v31
 * but this is currently not working in Eclipse (see
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=429739).
 * <p>
 * We currently test a set of registry keys (see
 * http://stackoverflow.com/questions
 * /730889/how-to-detect-whether-i-need-to-install-vcredist). Alternatively, we
 * could also use the following command: <code> wmic product </code> but this is
 * very slow.
 */
class VsCpp10Check implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private boolean failedWithError;
	private boolean installationFound;

	private String[] keys = {
			"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\x64",
			"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\ia64",
			"HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\x64",
			"HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\10.0\\VC\\VCRedist\\ia64"
	};

	public void run() {
		log.trace("check if a VS C++ 10 runtime is installed");
		failedWithError = false;
		installationFound = false;
		try {
			for (String key : keys) {
				if (checkKey(key)) {
					installationFound = true;
					log.info("VS C++ 10 found: {}", key);
					break;
				}
			}
		} catch (Exception e) {
			log.error("failed to check if a VS C++ 10 runtime is installed", e);
			failedWithError = true;
		}
	}

	private boolean checkKey(String key) throws Exception {
		String command = "REG QUERY " + key + " /v Installed";
		log.trace("execute command {}", command);
		Process process = Runtime.getRuntime().exec(command);
		process.waitFor();
		try (Scanner scanner = new Scanner(process.getInputStream())) {
			scanner.useDelimiter("\\A");
			if (!scanner.hasNext())
				return false;
			String out = scanner.next();
			scanner.close();
			return out != null
					&& out.contains("Installed")
					&& out.contains("REG_DWORD")
					&& out.contains("0x1");
		}
	}

	boolean failedWithError() {
		return failedWithError;
	}

	boolean installationFound() {
		return installationFound;
	}

}

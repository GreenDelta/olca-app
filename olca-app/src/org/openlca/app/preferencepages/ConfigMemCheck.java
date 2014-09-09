package org.openlca.app.preferencepages;

import org.openlca.util.OS;

/**
 * Checks memory settings from the configuration page. It shows error/warning
 * dialogs if an entered value is not appropriate for the system.
 */
class ConfigMemCheck {

	public static int getDefault() {
		if (isX86())
			return 1280;
		else
			return 3072;
	}

	/**
	 * Returns -1 if the given values is not o.k. In this case an error message
	 * is shown.
	 */
	public static int parseAndCheck(String value) {
		if (value == null || value.trim().isEmpty())
			return showError("@The given value is empty.");
		try {
			int val = Integer.parseInt(value);
			if (val < 256)
				return showError("@The memory value must be at least 256 MB.");
			if (val > 1280 && isX86() && OS.getCurrent() == OS.Windows)
				return showError("@The memory value is to high for your operating system.");
			else
				return val;
		} catch (Exception e) {
			return showError("@The given value is not an integer number");
		}
	}

	private static int showError(String message) {
		org.openlca.app.util.Error.showBox("@Invalid memory value", message);
		return -1;
	}

	private static boolean isX86() {
		String arch = System.getProperty("os.arch");
		if (arch == null)
			return false;
		if (arch.trim().toLowerCase().equals("x86"))
			return true;
		else
			return false;
	}

}

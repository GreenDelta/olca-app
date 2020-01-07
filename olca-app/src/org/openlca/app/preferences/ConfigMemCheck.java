package org.openlca.app.preferences;

import org.openlca.app.M;
import org.openlca.app.util.MsgBox;
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
			return showError(M.EmptyValueMessage);
		try {
			int val = Integer.parseInt(value);
			if (val < 256)
				return showError(M.MemoryToLowMessage);
			if (val > 1280 && isX86() && OS.get() == OS.WINDOWS)
				return showError(M.MemoryToHighMessage);
			else
				return val;
		} catch (Exception e) {
			return showError(M.NotAnIntegerNumber);
		}
	}

	private static int showError(String message) {
		MsgBox.error(M.InvalidMemoryValue, message);
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

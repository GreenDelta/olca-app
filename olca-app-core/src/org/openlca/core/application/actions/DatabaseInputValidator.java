package org.openlca.core.application.actions;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.osgi.util.NLS;
import org.openlca.core.application.Messages;

/**
 * Implementation of {@link IInputValidator} for validating the name of a
 * new database
 * 
 * @author Sebastian Greve
 * 
 */
public class DatabaseInputValidator implements IInputValidator {

	/**
	 * Databases that already exist on the data provider
	 */
	private final String[] existingDatabases;

	/**
	 * Creates a new validator instance
	 * 
	 * @param existingDatabases
	 *            Databases that already exist on the data provider
	 */
	public DatabaseInputValidator(final String[] existingDatabases) {
		this.existingDatabases = existingDatabases;
	}

	@Override
	public String isValid(final String text) {
		String error = null;
		String newText = text;
		newText = newText.toLowerCase();
		for (final String path : existingDatabases) {
			if (newText.equals(path)) {
				error = NLS.bind(Messages.AlreadyExists, newText);
				break;
			}
		}
		if (error == null) {
			if (newText.length() < 4) {
				error = Messages.MinimumCharactersError;
			} else if (newText.contains(" ")) {
				error = Messages.NoSpace;
			} else if (!(newText.charAt(0) >= 'a' && newText.charAt(0) <= 'z')) {
				error = Messages.MustStartWithLetter;
			} else if (newText.equals("mysql") || newText.equals("test")) {
				error = NLS.bind(Messages.Reserved, newText);
			} else {
				for (final char c : newText.toCharArray()) {
					if (!(c == '0' || c >= '1' && c <= '9' || c >= 'a'
							&& c <= 'z')) {
						error = Messages.OnlyNumbersLetters;
						break;
					}
				}
			}
		}
		return error;
	}
}
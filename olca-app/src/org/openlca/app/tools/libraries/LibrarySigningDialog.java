package org.openlca.app.tools.libraries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.util.Strings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LibraryCombo;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryPackage;
import org.openlca.license.License;
import org.openlca.license.Licensor;
import org.openlca.license.certificate.Person;

public class LibrarySigningDialog extends FormDialog {

	private final Config config;

	private LibrarySigningDialog() {
		super(UI.shell());
		this.config = new Config();
	}

	public static void show() {
		try {
			new LibrarySigningDialog().open();
		} catch (Exception e) {
			ErrorReporter.on("Failed to open library signing dialog", e);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.SignALibrary);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 450);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 2);
		UI.gridData(body, true, false);
		UI.label(body, tk, M.Library);
		new LibraryCombo(body, tk, lib -> !License.of(lib.folder()).isPresent(), lib -> {
			config.library = lib;
			updateOkButton();
		});
		UI.fileSelectSave(body, tk, M.TargetFile, ".zip", config::getDefaultName, file -> {
			config.output = file;
			updateOkButton();
		});
		UI.folderSelect(body, tk, M.CertificateDirectory, dir -> {
			config.certificateDir = dir;
			updateOkButton();
		});
		Controls.set(UI.labeledText(body, tk, M.Email), "", s -> {
			config.email = s;
			updateOkButton();
		});
		Controls.set(UI.labeledText(body, tk, M.Password, SWT.PASSWORD | SWT.BORDER), "", s -> {
			config.password = s.toCharArray();
			updateOkButton();
		});
		UI.date(body, tk, M.StartDate, config.notBefore, date -> {
			config.notBefore = date;
			updateOkButton();
		});
		UI.date(body, tk, M.EndDate, config.notAfter, date -> {
			config.notAfter = date;
			updateOkButton();
		});
	}

	private void updateOkButton() {
		if (getButton(OK) == null)
			return;
		getButton(OK).setEnabled(false);
		if (config.output == null)
			return;
		if (config.certificateDir == null
				|| !config.certificateDir.exists()
				|| !config.certificateDir.isDirectory())
			return;
		if (config.library == null)
			return;
		if (config.password == null || config.password.length < 6)
			return;
		if (Strings.isBlank(config.email))
			return;
		if (config.notBefore == null)
			return;
		if (config.notAfter == null || config.notAfter.before(config.notBefore))
			return;
		getButton(OK).setEnabled(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(OK).setEnabled(false);
	}

	@Override
	protected void okPressed() {
		super.okPressed();
		var signing = new Signing();
		App.runWithProgress(M.CreatingLibraryDots, signing, () -> {
			Navigator.refresh();
			if (signing.e != null) {
				ErrorReporter.on("Failed to sign library", signing.e);
			}
		});
	}

	private class Signing implements Runnable {

		private Exception e;

		@Override
		public void run() {
			try {
				var licensor = Licensor.getInstance(config.certificateDir);
				var info = licensor.createCertificateInfo(config.notBefore(), config.notAfter(), config.subject());
				var tmp = Files.createTempFile("olca-lib", ".zip").toFile();
				LibraryPackage.zip(config.library, tmp);
				try (var input = new ZipInputStream(new FileInputStream(tmp));
						var output = new ZipOutputStream(new FileOutputStream(config.output))) {
					licensor.license(input, output, config.password, info);
				}
			} catch (Exception e) {
				this.e = e;
			}
		}

	}

	private static class Config {

		private File output;
		private File certificateDir;
		private Library library;
		private String email;
		private char[] password;
		private Date notBefore = Calendar.getInstance().getTime();
		private Date notAfter = Calendar.getInstance().getTime();

		private Person subject() {
			return new Person(email, null, null, email, null);
		}

		private String getDefaultName() {
			return library != null ? library.name() + "-signed.zip" : null;

		}

		private Date notBefore() {
			var cal = Calendar.getInstance();
			cal.setTime(notBefore);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}

		private Date notAfter() {
			var cal = Calendar.getInstance();
			cal.setTime(notAfter);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			return cal.getTime();
		}

	}

}

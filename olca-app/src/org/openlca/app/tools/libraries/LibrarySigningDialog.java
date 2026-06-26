package org.openlca.app.tools.libraries;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LibraryCombo;
import org.openlca.commons.Strings;
import org.openlca.core.library.LibraryPackage;
import org.openlca.license.License;
import org.openlca.license.Licensor;

public class LibrarySigningDialog extends FormDialog {

	private final SigningConfig config;

	private LibrarySigningDialog() {
		super(UI.shell());
		this.config = new SigningConfig();
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
		new LibraryCombo(body, tk, lib -> License.of(lib.folder()).isEmpty(), lib -> {
			config.library = lib;
			checkOk();
		});

		// output file selector
		FileSelector.create(body, tk, M.TargetFile)
				.onSelect(() -> {
					var file = FileChooser.forSavingFile(
						"Select the file where the signed library should be saved",
						config.getDefaultName());
					if (file == null)
						return Optional.ofNullable(config.output);
					config.output = file;
					checkOk();
					return Optional.of(file);
				});

		// selector for the certificate folder
		FileSelector.create(body, tk, M.CertificateDirectory)
				.onSelect(() -> {
					var dir = FileChooser.selectFolder();
					if (dir == null)
						return Optional.ofNullable(config.certificateDir);
					var res = SigningConfig.validateCertificateFolder(dir);
					if (res.isError()) {
						MsgBox.error("Invalid certificate directory",
							"The folder you selected is not a valid certificate directors: "
								+ res.error());
						return Optional.ofNullable(config.certificateDir);
					}
					config.certificateDir = dir;
					checkOk();
					return Optional.of(dir);
				});

		Controls.set(UI.labeledText(body, tk, M.Email), "", s -> {
			config.email = s;
			checkOk();
		});
		Controls.set(UI.labeledText(body, tk, M.Password, SWT.PASSWORD | SWT.BORDER), "", s -> {
			config.password = s.toCharArray();
			checkOk();
		});
		UI.date(body, tk, M.StartDate, config.validFrom, date -> {
			config.validFrom = date;
			checkOk();
		});
		UI.date(body, tk, M.EndDate, config.validUntil, date -> {
			config.validUntil = date;
			checkOk();
		});
	}



	private void checkOk() {
		if (getButton(OK) == null)
			return;
		getButton(OK).setEnabled(false);
		if (config.output == null)
			return;

		var res = SigningConfig.validateCertificateFolder(config.certificateDir);
		if (res.isError())
			return;

		if (config.library == null)
			return;
		if (config.password == null || config.password.length < 6)
			return;
		if (Strings.isBlank(config.email))
			return;
		if (config.validFrom == null)
			return;
		if (config.validUntil == null || config.validUntil.before(config.validFrom))
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
}

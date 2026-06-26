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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LibraryCombo;
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
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);
		createLibrarySection(body, tk);
		createOwnerSection(body, tk);
	}

	private void createLibrarySection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Licensed library");
		UI.label(comp, tk, M.Library);
		new LibraryCombo(comp, tk, lib -> License.of(lib.folder()).isEmpty(), lib -> {
			config.library = lib;
			checkOk();
		});

		FileSelector.create(comp, tk, "Output file")
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

		FileSelector.create(comp, tk, "Vendor certificate")
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

		Controls.set(UI.labeledText(comp, tk, M.Password, SWT.PASSWORD), "", s -> {
			config.password = s;
			checkOk();
		});
		Controls.set(UI.labeledText(comp, tk, "Confirm password", SWT.PASSWORD), "", s -> {
			config.passwordConfirm = s;
			checkOk();
		});

		UI.filler(comp, tk);
		var timeComp = UI.composite(comp, tk);
		UI.stretchX(timeComp);
		UI.gridLayout(timeComp, 4, 10, 0);
		UI.date(timeComp, tk, "Valid from", config.validFrom, date -> {
			config.validFrom = date;
			checkOk();
		});
		UI.date(timeComp, tk, "Valid until", config.validUntil, date -> {
			config.validUntil = date;
			checkOk();
		});
	}

	private void createOwnerSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "License owner");
		Controls.set(UI.labeledText(comp, tk, "Full name"), "", s -> {
			config.fullName = s;
			checkOk();
		});
		Controls.set(UI.labeledText(comp, tk, M.Email), "", s -> {
			config.email = s;
			checkOk();
		});

		config.country = System.getProperty("user.country");
		Controls.set(UI.labeledText(comp, tk, "Country code"), config.country, s -> {
			config.country = s;
			checkOk();
		});
		Controls.set(UI.labeledText(comp, tk, "Organisation"), "", s -> {
			config.organisation = s;
			checkOk();
		});
	}

	private void checkOk() {
		var btn = getButton(OK);
		if (btn == null) return;
		getButton(OK).setEnabled(config.isComplete());
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
					licensor.license(input, output, config.password.toCharArray(), info);
				}
			} catch (Exception e) {
				this.e = e;
			}
		}

	}
}

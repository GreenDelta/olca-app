package org.openlca.app.devtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.license.Licensor;
import org.openlca.license.certificate.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryLicensing extends FormDialog {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private LibCombo combo;
	private Text ownerNameText;
	private Text ownerEmailText;
	private Text passwordText;
	private Date notBefore;
	private Date notAfter;

	public static int show() {
		if (Workspace.getLibraryDir()
				.getLibraries().isEmpty()) {
			MsgBox.error(M.NoLibrariesFound, M.NeedAddLibrary);
			return Window.CANCEL;
		}
		return new LibraryLicensing().open();
	}

	public LibraryLicensing() {
		super(UI.shell());
		initDates();
		setBlockOnOpen(false);
		setShellStyle(SWT.CLOSE
				| SWT.MODELESS
				| SWT.BORDER
				| SWT.TITLE
				| SWT.RESIZE
				| SWT.MIN);
	}

	private void initDates() {
		notBefore = new GregorianCalendar().getTime();
		var cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, 1);
		notAfter = cal.getTime();
	}

	@Override
	protected void okPressed() {
		Licensor licensor;
		try {
			licensor = getLicensor();
		} catch (IOException | URISyntaxException e) {
			log.error("Failed to create library licensor", e);
			MsgBox.error(M.FailedToLicenseLibrary);
			return;
		}

		var fileName = combo.selected.name() + ".zip";
		var file = FileChooser.forSavingFile(M.LibraryLicensing, fileName);


		App.exec(M.LibraryLicensing, () -> {
			try {
				runOn(licensor, file);
			} catch (IOException e) {
				log.error("Failed to license library", e);
				MsgBox.error(M.FailedToLicenseLibrary);
			}
		});
		super.okPressed();
	}

	private void runOn(Licensor licensor, File target) throws IOException {
		var library = combo.selected;
		var subject = new Person(
				ownerNameText.getText(), "", "", ownerEmailText.getText(), "");
		var info = licensor.createCertificateInfo(notBefore, notAfter, subject);

		var in = zipLibrary(library);
		try (var input = new ZipInputStream(new FileInputStream(in));
				 var output = new ZipOutputStream(new FileOutputStream(target))) {
			licensor.license(input, output, passwordText.getTextChars(), info);
		}
	}

	private File zipLibrary(Library library) throws IOException {
		// Create a temporary zip file from the library folder
		var zip = File.createTempFile("tmp", ".zip");
		zip.deleteOnExit();
		try (var output = new ZipOutputStream(new FileOutputStream(zip))) {
			var files = library.folder().listFiles();
			if (files == null)
				return zip;
			for (var file : files) {
				if (!file.isFile())
					throw new IOException(file + " is not a file");
				try (FileInputStream fileInput = new FileInputStream(file)) {
					// Add the file entry to the ZIP
					ZipEntry entry = new ZipEntry(file.getName());
					output.putNextEntry(entry);
					fileInput.transferTo(output);
					output.closeEntry();
				}
			}
		}
		return zip;
	}

	private Licensor getLicensor() throws IOException, URISyntaxException {
		var caURL = getClass().getResource("nexus-ca");
		var ca = new File(Objects.requireNonNull(caURL).toURI());
		return Licensor.getInstance(ca);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.LibraryLicensing);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 700);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), mForm.getToolkit());

		// port text
		var comp = UI.composite(body, tk);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2);

		combo = new LibCombo(UI.labeledCombo(comp, tk, M.Library));
		ownerNameText = UI.labeledText(comp, tk, M.Owner);
		ownerEmailText = UI.labeledText(comp, tk, M.Email);
		passwordText = UI.labeledText(comp, tk, M.Password,
				SWT.BORDER | SWT.PASSWORD);

		createTimeSection(comp, tk);
	}

	private void createTimeSection(Composite comp, FormToolkit tk) {
		// the handler for setting the start or end time
		BiConsumer<DateTime, Boolean> setTime = (widget, isStart) -> {
			var current = isStart
					? notBefore
					: notAfter;
			if (current != null) {
				var cal = new GregorianCalendar();
				cal.setTime(current);
				widget.setDate(
						cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH),
						cal.get(Calendar.DAY_OF_MONTH));
			}

			widget.addSelectionListener(Controls.onSelect(_e -> {
				var selected = new GregorianCalendar(
						widget.getYear(), widget.getMonth(), widget.getDay()).getTime();
				if (isStart) {
					notBefore = selected;
				} else {
					notAfter = selected;
				}
			}));
		};

		// start date
		UI.label(comp, tk, M.StartDate);
		var startBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		UI.gridData(startBox, false, false).minimumWidth = 150;
		setTime.accept(startBox, true);

		// end date
		UI.label(comp, tk, M.EndDate);
		var endBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		UI.gridData(endBox, false, false).minimumWidth = 150;
		setTime.accept(endBox, false);
	}

	static class LibCombo {

		private final List<Library> libraries = new ArrayList<>();
		private final Combo combo;
		private Library selected;

		LibCombo(Combo combo) {
			this.combo = combo;
			fill();
			if (!libraries.isEmpty()) {
				select(libraries.get(0));
			}
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				if (idx >= 0) {
					selected = libraries.get(idx);
				}
			});
		}

		void updateWith(Library newLib) {
			fill();
			select(newLib);
		}

		private void fill() {
			libraries.clear();
			libraries.addAll(Workspace.getLibraryDir()
					.getLibraries());
			libraries.sort(Comparator.comparing(Library::name));
			var items = libraries.stream()
					.map(Library::name)
					.toArray(String[]::new);
			combo.setItems(items);
		}

		private void select(Library lib) {
			if (lib == null) {
				selected = null;
				return;
			}
			int idx = libraries.indexOf(lib);
			if (idx < 0)
				return;
			combo.select(idx);
			selected = lib;
		}

	}

}

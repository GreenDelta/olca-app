package org.openlca.app.editors.libraries;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;
import org.openlca.license.certificate.CertificateInfo;
import org.openlca.license.certificate.Person;

public class LibraryLicensePage extends FormPage {

	public static final String ID = "LibraryLicensePage";
	private final LibraryInfo info;
	private final Library library;

	public LibraryLicensePage(LibraryEditor editor) {
		super(editor, ID, M.LicenseInformation);
		info = editor.info;
		library = editor.library;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var title = M.LicenseInformation + " - " + info.name();
		var form = UI.header(mForm, title, Icon.LIBRARY.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var license = Libraries.getLicense(library.folder());
		license.ifPresentOrElse(l -> renderLicenseForm(body, tk, l),
				() -> renderNoLicenseForm(body, tk));
	}

	private void renderNoLicenseForm(Composite body, FormToolkit tk) {
		UI.label(body, tk, M.NoLicenseDetected);
	}

	private void renderLicenseForm(Composite body, FormToolkit tk,
			CertificateInfo license) {
		createStatusSection(body, tk, license);

		var owner = UI.formSection(body, tk, M.Owner, 2);
		createPeopleSection(owner, tk, license.subject());

		var vendor = UI.formSection(body, tk, M.Vendor, 2);
		createPeopleSection(vendor, tk, license.issuer());
	}

	private void createStatusSection(Composite body, FormToolkit tk,
			CertificateInfo license) {
		var comp = UI.formSection(body, tk, M.Status, 2);

		createStatusLabel(comp, tk, license);
		createExpiryDateLabel(comp, tk, license);
		createStartDateLabel(comp, tk, license);
	}

	private void createStatusLabel(Composite comp, FormToolkit tk,
      CertificateInfo license) {
		UI.label(comp, tk, M.Status);
		var label = UI.cLabel(comp, tk);

		var date = new Date();
		if (license.notBefore().after(date)) {
      label.setText(M.Upcoming);
      label.setImage(Icon.YELLOW_DOT.get());
			return;
		}

		if (license.isValid()) {
			label.setText(M.Valid);
			label.setImage(Icon.GREEN_DOT.get());
			return;
		}

		label.setText(M.Expired);
		label.setImage(Icon.RED_DOT.get());
  }

	private void createExpiryDateLabel(Composite comp, FormToolkit tk,
			CertificateInfo license) {
		UI.label(comp, tk, M.ExpiryDate);

		var notAfter = license.notAfter();
		var date = DateFormat.getDateInstance().format(notAfter);
		var timeDiff = Math.abs(notAfter.getTime() - new Date().getTime());
		var daysDiff = TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS);
		var text = notAfter.after(new Date())
				? date + " (" + daysDiff + " " + M.DaysFromNow + ")"
				: date + " (" + daysDiff + " " + M.DaysAgo + ")";
		UI.label(comp, tk, text);
	}

	private void createStartDateLabel(Composite comp, FormToolkit tk,
			CertificateInfo license) {
		UI.label(comp, tk, M.StartDate);

		var notBefore = license.notBefore();
		var date = DateFormat.getDateInstance().format(notBefore);
		var timeDiff = Math.abs(notBefore.getTime() - new Date().getTime());
		var daysDiff = TimeUnit.DAYS.convert(timeDiff, TimeUnit.MILLISECONDS);
		var text = notBefore.after(new Date())
				? date + " (" + daysDiff + " " + M.DaysFromNow + ")"
				: date;
		UI.label(comp, tk, text);
	}

	private void createPeopleSection(Composite comp, FormToolkit tk,
			Person person) {
		UI.label(comp, tk, M.Name);
		UI.label(comp, tk, noneIfBlank(person.commonName()));

		UI.label(comp, tk, M.Organisation);
		UI.label(comp, tk, noneIfBlank(person.organisation()));

		UI.label(comp, tk, M.Country);
		UI.label(comp, tk, noneIfBlank(person.country()));

		UI.label(comp, tk, M.Email);
		UI.label(comp, tk, noneIfBlank(person.email()));
	}

	private static String noneIfBlank(String string) {
		return string.isBlank()
				? M.NoneHyphen
				: string;
	}

}

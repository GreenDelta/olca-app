package org.openlca.app.editors.results.openepd;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

import com.google.gson.GsonBuilder;

public class ExportDialog extends FormDialog {

	private final ResultModel result;
	private final Ec3Epd epd;
	private final Credentials credentials;
	private String scope = "A1A2A3";

	private ExportDialog(ResultModel result) {
		super(UI.shell());
		this.result = result;
		setBlockOnOpen(true);
		setShellStyle(SWT.CLOSE
			| SWT.MODELESS
			| SWT.BORDER
			| SWT.TITLE
			| SWT.RESIZE
			| SWT.MIN);
		credentials = Credentials.init();
		epd = new Ec3Epd();
		epd.name = result.name;
		epd.description = result.description;
		epd.isPrivate = true;
		epd.isDraft = true;
	}

	public static int show(ResultModel result) {
		return new ExportDialog(result).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export as OpenEPD to EC3");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		credentialsSection(body, tk);
		metaSection(body, tk);
	}

	private void credentialsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// url
		var filled = 0;
		var urlText = UI.formText(comp, tk, "URL");
		if (Strings.notEmpty(credentials.url)) {
			urlText.setText(credentials.url);
			filled++;
		}
		urlText.addModifyListener($ -> {
			credentials.url = urlText.getText();
		});

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user)) {
			userText.setText(credentials.user);
			filled++;
		}
		userText.addModifyListener($ -> {
			credentials.user = userText.getText();
		});

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD);
		if (Strings.notEmpty(credentials.password)) {
			pwText.setText(credentials.password);
			filled++;
		}
		pwText.addModifyListener($ -> {
			credentials.password = pwText.getText();
		});

		section.setExpanded(filled < 3);
	}

	private void metaSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.GeneralInformation);
		var comp = UI.sectionClient(section, tk, 2);
		text(UI.formText(comp, tk, "Name"),
			epd.name, s -> epd.name = s);
		text(UI.formMultiText(comp, tk, "Description"),
			epd.description, s -> epd.description = s);

		var combo = UI.formCombo(comp, tk, "Scope");
		var items = new String[]{
			"A1A2A3", "A1", "A2", "A3", "A4", "A5",
			"B1", "B2", "B3", "B4", "B5", "B6", "B7",
			"C1", "C2", "C3", "C4"};
		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> {
			var idx = combo.getSelectionIndex();
			scope = combo.getItem(idx);
		});
	}

	private void text(Text text, String initial, Consumer<String> onChange) {
		if (initial != null) {
			text.setText(initial);
		}
		text.addModifyListener($ -> onChange.accept(text.getText()));
	}


	@Override
	protected void okPressed() {

		/**
		 var client = credentials.login().orElse(null);
		 if (client == null) {
		 MsgBox.error(
		 "Failed to login to EC3",
		 "Could not login to EC3 with the provided credentials.");
		 return;
		 }
		 */

		var file = FileChooser.forSavingFile(
			"Save in OpenEPD format",
			URLEncoder.encode(Strings.orEmpty(epd.name), StandardCharsets.UTF_8)
				+ ".json");
		if (file == null)
			return;
		try {
			var json = new GsonBuilder().setPrettyPrinting()
				.create()
				.toJson(epd.toJson());
			Files.writeString(file.toPath(), json);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}
}

package org.openlca.app.editors.results.openepd.output;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.openlca.app.editors.results.openepd.model.Credentials;
import org.openlca.app.editors.results.openepd.model.Ec3Epd;
import org.openlca.app.editors.results.openepd.model.Ec3ImpactModel;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class ExportDialog extends FormDialog {

	private final Ec3Epd epd;
	private final Credentials credentials;
	final Ec3ImpactModel impactModel;

	private final List<ResultSection> sections = new ArrayList<>();

	private ExportDialog(ResultModel result) {
		super(UI.shell());
		setBlockOnOpen(true);
		setShellStyle(SWT.CLOSE
			| SWT.MODELESS
			| SWT.BORDER
			| SWT.TITLE
			| SWT.RESIZE
			| SWT.MIN);
		credentials = Credentials.getDefault();
		epd = new Ec3Epd();
		epd.name = result.name;
		epd.description = result.description;
		epd.isPrivate = true;
		epd.isDraft = true;

		impactModel = Ec3ImpactModel.get();
		sections.add(ResultSection.of(this, result));
	}

	public static int show(ResultModel result) {
		return new ExportDialog(result).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Upload as OpenEPD to EC3");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 700);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		credentialsSection(body, tk);
		metaSection(body, tk);
		for (var section : sections) {
			section.render(body, tk);
		}
	}

	private void credentialsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// url
		var filled = 0;
		var urlText = UI.formText(comp, tk, "URL");
		if (Strings.notEmpty(credentials.url())) {
			urlText.setText(credentials.url());
			filled++;
		}
		urlText.addModifyListener(
			$ -> credentials.url(urlText.getText()));

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user())) {
			userText.setText(credentials.user());
			filled++;
		}
		userText.addModifyListener(
			$ -> credentials.user(userText.getText()));

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD);
		if (Strings.notEmpty(credentials.password())) {
			pwText.setText(credentials.password());
			filled++;
		}
		pwText.addModifyListener(
			$ -> credentials.password(pwText.getText()));

		section.setExpanded(filled < 3);
	}

	private void metaSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.GeneralInformation);
		var comp = UI.sectionClient(section, tk, 2);
		text(UI.formText(comp, tk, "Name"),
			epd.name, s -> epd.name = s);
		text(UI.formMultiText(comp, tk, "Description"),
			epd.description, s -> epd.description = s);
		UI.filler(comp, tk);

		var button = tk.createButton(comp, "Add result", SWT.PUSH);
		button.setImage(Images.get(ModelType.RESULT));

	}

	private void text(Text text, String initial, Consumer<String> onChange) {
		if (initial != null) {
			text.setText(initial);
		}
		text.addModifyListener($ -> onChange.accept(text.getText()));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Upload to EC3", false);
		createButton(parent, 1024, "Save as file", false);
		createButton(parent, IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId != 1024) {
			super.buttonPressed(buttonId);
			return;
		}
		var file = FileChooser.forSavingFile(
			"Save as OpenEPD document",
			epd.name + ".json");
		if (file == null)
			return;
		addResults();
		var json = new GsonBuilder().setPrettyPrinting()
			.create()
			.toJson(epd.toJson());
		try {
			Files.writeString(file.toPath(), json);
			close();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}

	@Override
	protected void okPressed() {

		var client = credentials.login().orElse(null);
		if (client == null) {
			MsgBox.error(
				"Failed to login to EC3",
				"Could not login to EC3 with the provided credentials.");
			return;
		}

		try {
			addResults();
			var response = client.post("epds", epd.toJson(), JsonElement.class);
			var respStr = new GsonBuilder().setPrettyPrinting()
				.create()
				.toJson(response);
			System.out.println(respStr);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}

	private void addResults() {
		epd.clearImpacts();
		// TODO: merge the impact sets
		for (var section : sections) {
			var pair = section.createImpacts();
			if (pair == null)
				continue;
			epd.putImpactSet(pair.first, pair.second);
		}
	}
}

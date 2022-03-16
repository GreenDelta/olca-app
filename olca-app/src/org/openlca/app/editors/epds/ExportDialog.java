package org.openlca.app.editors.epds;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.tools.openepd.CategoryDialog;
import org.openlca.app.tools.openepd.ErrorDialog;
import org.openlca.app.tools.openepd.LoginPanel;
import org.openlca.app.tools.openepd.model.Api;
import org.openlca.app.tools.openepd.model.Ec3CategoryTree;
import org.openlca.app.tools.openepd.model.EpdDoc;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;

import com.google.gson.GsonBuilder;
import org.openlca.util.Strings;

class ExportDialog extends FormDialog {

	private final EpdDoc epd;
	private Ec3CategoryTree categories;
	private LoginPanel loginPanel;

	public static void show(EpdDoc epd) {
		if (epd == null)
			return;
		new ExportDialog(epd).open();
	}

	private ExportDialog(EpdDoc epd) {
		super(UI.shell());
		this.epd = Objects.requireNonNull(epd);
		this.categories = Ec3CategoryTree.loadFromCacheFile();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export an openEPD document");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		loginPanel = LoginPanel.create(body, tk);

		// info section
		var infoSection = UI.section(body, tk, M.GeneralInformation);
		var comp = UI.sectionClient(infoSection, tk, 2);

		// name and unit
		Controls.set(
			UI.formText(comp, tk, M.Name),
			epd.productName, name -> epd.productName = name);
		// Controls.set(
		//	UI.formText(comp, tk, "Declared unit"),
		// 	epd.declaredUnit, s -> epd.declaredUnit = s);

		// category link
		UI.formLabel(comp, tk, M.Category);
		new CategoryLink(this).render(comp, tk);

		// description
		Controls.set(
			UI.formMultiText(comp, tk, "Description"),
			epd.productDescription, s -> epd.productDescription = s);

		// date fields
		UI.formLabel(comp, tk, "Date of issue");
		var issueDate = new DateTime(comp, SWT.DROP_DOWN);
		// UI.gridData(issueDate, false, false).widthHint = 120;
		tk.adapt(issueDate);
		date(issueDate, epd.dateOfIssue, d -> epd.dateOfIssue = d);
		UI.formLabel(comp, tk, "End of validity");
		var endDate = new DateTime(comp, SWT.DROP_DOWN);
		// UI.gridData(endDate, false, false).widthHint = 120;
		tk.adapt(endDate);
		date(endDate, epd.dateValidityEnds, d -> epd.dateValidityEnds = d);

		// result sections
		for (var result : epd.impactResults) {
			new ExportResultSection(result).render(body, tk);
		}
	}

	private void date(
		DateTime widget, LocalDate initial, Consumer<LocalDate> onChange) {
		if (initial != null) {
			widget.setDate(
				initial.getYear(),
				initial.getMonthValue() - 1, // !
				initial.getDayOfMonth());
		}
		widget.addSelectionListener(Controls.onSelect($ -> {
			var newDate = LocalDate.of(
				widget.getYear(), widget.getMonth() + 1, widget.getDay());
			onChange.accept(newDate);
		}));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Upload", true);
		createButton(parent, 999, "Save as file", false);
		createButton(parent, IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		var client = loginPanel.login().orElse(null);
		if (client == null)
			return;
		var b = Question.ask("Upload as draft?",
			"Upload this as draft to " + loginPanel.credentials().ec3Url() + "?");
		if (!b)
			return;
		try {
			var response = client.post("/epds", epd.toJson());
			if (!response.hasJson()) {
				MsgBox.error("Received no response from server");
				return;
			}
			var json = response.json();
			if (!json.isJsonObject()) {
				MsgBox.error("Received an unexpected response from server");
				return;
			}
			var obj = json.getAsJsonObject();
			// TODO
			System.out.println(
				new GsonBuilder().setPrettyPrinting().create().toJson(obj));
			if (obj.has("validation_errors")) {
				ErrorDialog.show(obj);
				return;
			}
			var url = loginPanel.credentials().ec3Url();
			Popup.info("Uploaded EPD",
				"The EPD was uploaded to <a href='" + url + "'>" + url + "</a>");
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			okPressed();
			return;
		}
		if (buttonId == IDialogConstants.CANCEL_ID) {
			cancelPressed();
			return;
		}

		// save as file
		var json = epd.toJson();
		var file = FileChooser.forSavingFile(
			"Save openEPD document", epd.productName + ".json");
		if (file == null)
			return;
		try {
			Json.write(json, file);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to save openEPD document", e);
		}
	}

	private record CategoryLink(ExportDialog dialog) {

		void render(Composite comp, FormToolkit tk) {
			var link = tk.createImageHyperlink(comp, SWT.NONE);
			updateLink(link, getPath());

			Controls.onClick(link, $ -> {
				var categories = getCategories();
				if (categories.isEmpty())
					return;
				var category = CategoryDialog.selectFrom(categories);
				if (category == null)
					return;
				var path = Strings.notEmpty(category.openEpd)
					? category.openEpd
					: categories.pathOf(category);
				if (Strings.nullOrEmpty(path))
					return;
				setPath(path);
				updateLink(link, path);
			});
		}

		String getPath() {
			for (var c : dialog.epd.productClasses) {
				if (Objects.equals(c.first, "io.cqd.ec3")) {
					return c.second;
				}
			}
			return null;
		}

		Ec3CategoryTree getCategories() {
			var d = dialog;
			if (!d.categories.isEmpty())
				return d.categories;
			var client = d.loginPanel.login().orElse(null);
			if (client == null)
				return dialog.categories;
			d.categories = Api.getCategoryTree(client);
			if (d.categories.isEmpty()) {
				MsgBox.error("No categories could be loaded",
					"No categories could be loaded from "
						+ d.loginPanel.credentials().ec3Url());
			}
			return d.categories;
		}

		void setPath(String path) {
			var classes = dialog.epd.productClasses;
			classes.clear();
			if (Strings.nullOrEmpty(path))
				return;
			classes.add(Pair.of("io.cqd.ec3", path));
		}

		void updateLink(ImageHyperlink link, String path) {
			if (Strings.nullOrEmpty(path)) {
				link.setText(" - none -");
			} else {
				link.setText(path);
			}
			link.getParent().pack();
		}
	}

}

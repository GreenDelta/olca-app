package org.openlca.app.editors.epds;

import java.io.File;
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
import org.openlca.app.rcp.Workspace;
import org.openlca.app.tools.openepd.CategoryDialog;
import org.openlca.app.tools.openepd.ErrorDialog;
import org.openlca.app.tools.openepd.LoginPanel;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Popup;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.io.openepd.Api;
import org.openlca.io.openepd.Ec3CategoryTree;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdQuantity;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

import com.google.gson.GsonBuilder;

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
		this.categories = Ec3CategoryTree.fromFile(categoryCacheFile());
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
		var header = epd.productName;
		if (epd.declaredUnit != null) {
			var num = Numbers.format(epd.declaredUnit.amount(), 2)
				+ " " + epd.declaredUnit.unit();
			header = num + " " + header;
		}
		var infoSection = UI.section(body, tk, header);
		var comp = UI.sectionClient(infoSection, tk, 3);

		// declaration URL
		if (Strings.nullOrEmpty(epd.declarationUrl)) {
			epd.declarationUrl = "http://add.an.original.declaration.url";
		}
		Controls.set(UI.formText(comp, tk, "Declaration URL"),
			epd.declarationUrl, s -> epd.declarationUrl = s);
		UI.filler(comp, tk);

		// kg per declared unit
		if (epd.kgPerDeclaredUnit == null) {
			epd.kgPerDeclaredUnit = new EpdQuantity(1, "kg");
		}
		Controls.set(UI.formText(comp, tk, "Mass per declared unit"),
			epd.kgPerDeclaredUnit.amount(),
			amount -> epd.kgPerDeclaredUnit = new EpdQuantity(amount, "kg"));
		UI.formLabel(comp, "kg/" + (epd.declaredUnit != null
			? epd.declaredUnit.unit()
			: "?"));

		// category link
		UI.formLabel(comp, tk, M.Category);
		new CategoryLink(this).render(comp, tk);
		UI.filler(comp, tk);

		// date fields
		UI.formLabel(comp, tk, "Date of issue");
		var issueDate = new DateTime(comp, SWT.DROP_DOWN);
		tk.adapt(issueDate);
		date(issueDate, epd.dateOfIssue, d -> epd.dateOfIssue = d);
		UI.filler(comp, tk);

		UI.formLabel(comp, tk, "End of validity");
		var endDate = new DateTime(comp, SWT.DROP_DOWN);
		tk.adapt(endDate);
		date(endDate, epd.dateValidityEnds, d -> epd.dateValidityEnds = d);
		UI.filler(comp, tk);

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
			"Upload this as draft to " + loginPanel.url() + "?");
		if (!b)
			return;
		try {
			var response = client.postEpd("/epds", epd.toJson());
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
			var url = loginPanel.url();
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
			if (!d.categories.isEmpty()) {
				d.categories.save(categoryCacheFile());
			} else {
				MsgBox.error("No categories could be loaded",
					"No categories could be loaded from "
						+ d.loginPanel.url());
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
			link.getParent().layout();
		}
	}

	private static File categoryCacheFile() {
		return new File(Workspace.getDir(), ".ec3-categories");
	}

}

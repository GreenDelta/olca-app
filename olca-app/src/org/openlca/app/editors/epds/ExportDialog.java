package org.openlca.app.editors.epds;

import java.io.File;
import java.util.Objects;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.tools.openepd.CategoryDialog;
import org.openlca.app.tools.openepd.LoginPanel;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;
import org.openlca.io.openepd.Api;
import org.openlca.io.openepd.Ec3CategoryTree;
import org.openlca.io.openepd.Ec3Client;
import org.openlca.io.openepd.EpdConverter;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdImpactResult;
import org.openlca.io.openepd.EpdQuantity;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

class ExportDialog extends FormDialog {

	private final EpdDoc epd;
	private final String existingId;

	private Ec3CategoryTree categories;
	private LoginPanel loginPanel;
	private ExportState state;

	static ExportState of(Epd model) {
		if (model == null)
			return ExportState.canceled();
		var dialog = new ExportDialog(model);
		dialog.open();
		return dialog.state != null
			? dialog.state
			: ExportState.canceled();
	}

	private ExportDialog(Epd model) {
		super(UI.shell());
		this.epd = EpdConverter.toEpdDoc(model);
		this.existingId = model.urn != null && model.urn.startsWith("openEPD:")
			? model.urn.substring(8)
			: null;
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

		// result sections
		for (var result : epd.impactResults) {
			new ExportResultSection(result).render(body, tk);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
			existingId == null ? "Upload" : "Update",
			true);
		createButton(parent, 999, "Save as file", false);
		createButton(parent, IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		var client = loginPanel.login().orElse(null);
		if (client == null)
			return;

		var qTitle = existingId == null
			? "Upload as draft?"
			: "Update existing EPD?";
		var qText = existingId == null
			? "Upload this as draft to " + loginPanel.url() + "?"
			: "Update existing EPD on " + loginPanel.url() + "?";
		if (!Question.ask(qTitle, qText))
			return;

		if (existingId == null) {
			tryUploadNew(client);
		} else {
			tryUpdateExisting(client);
		}
	}

	private void tryUploadNew(Ec3Client client) {
		try {

			var response = client.postEpd(epd.toJson());
			var json = response.hasJson()
				? response.json()
				: null;

			// check the response
			if (response.isError()) {
				if (json != null) {
					JsonErrorDialog.show(
						"The upload failed with the following error", json);
				} else {
					MsgBox.error("Upload failed", "Failed to upload EPD to EC3.");
				}
				return;
			}

			// extract the ID from the response
			String id = null;
			if (json != null && json.isJsonObject()) {
				id = Json.getString(json.getAsJsonObject(), "id");
			}
			if (Strings.nullOrEmpty(id)) {
				var error = "No ID returned from server.";
				if (json != null) {
					JsonErrorDialog.show(error, json);
				} else {
					MsgBox.error("Upload failed", error);
				}
				return;
			}

			state = ExportState.created(id);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}

	private void tryUpdateExisting(Ec3Client client) {
		try {
			var r = client.getEpd(existingId);
			if (r.isError() || !r.hasJson() || !r.json().isJsonObject()) {
				MsgBox.error(
					"Failed to get existing EPD",
					"Failed to load EPD (id='" + existingId + "') from server.");
				return;
			}
			var json = r.json().getAsJsonObject();
			var impacts = EpdImpactResult.toJson(epd.impactResults);
			json.add("impacts", impacts);
			r = client.putEpd(existingId, json);
			if (r.isError()) {
				var error = "Failed to update EPD " + existingId;
				if (r.hasJson()) {
					JsonErrorDialog.show(error, r.json());
				} else {
					MsgBox.error("Upload failed", error);
				}
				return;
			}
			state = ExportState.updated(existingId);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to update EPD", e);
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
			state = ExportState.file(file.getAbsolutePath());
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

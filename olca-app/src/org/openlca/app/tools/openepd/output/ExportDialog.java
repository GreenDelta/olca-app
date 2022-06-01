package org.openlca.app.tools.openepd.output;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.tools.openepd.CategoryDialog;
import org.openlca.app.tools.openepd.LoginPanel;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;
import org.openlca.io.openepd.Api;
import org.openlca.io.openepd.Ec3CategoryTree;
import org.openlca.io.openepd.EpdConverter;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdQuantity;
import org.openlca.io.openepd.io.MappedExportResult;
import org.openlca.io.openepd.io.MappingModel;
import org.openlca.io.openepd.io.MethodMapping;
import org.openlca.jsonld.Json;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

public class ExportDialog extends FormDialog {

	private final EpdDoc doc;
	private final String existingId;
	private final List<MethodMapping> mappings;

	private Ec3CategoryTree categories;
	private LoginPanel loginPanel;
	private ExportState state;

	public static ExportState of(Epd model) {
		if (model == null)
			return ExportState.canceled();
		var dialog = new ExportDialog(model);
		dialog.open();
		return dialog.state != null
			? dialog.state
			: ExportState.canceled();
	}

	private ExportDialog(Epd epd) {
		super(UI.shell());
		this.mappings = MappingModel.initFrom(epd).mappings();
		this.doc = EpdConverter.toEpdDoc(epd);
		this.existingId = epd.urn != null && epd.urn.startsWith("openEPD:")
			? epd.urn.substring(8)
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
		var comp = UI.formSection(body, tk, "Product information", 3);
		UI.formLabel(comp, M.Product);
		UI.formLabel(comp, doc.productName);
		UI.filler(comp, tk);

		// declared unit
		UI.formLabel(comp, tk, "Declared unit");
		UI.formLabel(comp, tk, doc.declaredUnit != null
			? doc.declaredUnit.toString()
			: "?");
		UI.filler(comp, tk);
		new MassField(this).render(comp, tk);

		// category link
		UI.formLabel(comp, tk, M.Category);
		new CategoryLink(this).render(comp, tk);
		UI.filler(comp, tk);

		// result sections
		mappings.forEach(model -> new MappingSection(model).render(body, tk));
		mForm.reflow(true);
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

		MappedExportResult.of(mappings).applyOn(doc);
		var upload = new Upload(client, doc);
		state = existingId == null
			? upload.newDraft()
			: upload.update(existingId);
		if (state.isError())
			return;
		super.okPressed();
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
		MappedExportResult.of(mappings).applyOn(doc);
		var json = doc.toJson();
		var file = FileChooser.forSavingFile(
			"Save openEPD document", doc.productName + ".json");
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

	private static File categoryCacheFile() {
		return new File(Workspace.root(), ".ec3-categories");
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
			for (var c : dialog.doc.productClasses) {
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
			var classes = dialog.doc.productClasses;
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

	record MassField(ExportDialog dialog) {

		void render(Composite comp, FormToolkit tk) {
			UI.formLabel(comp, tk, "Mass per declared unit");
			var mass = dialog.doc.kgPerDeclaredUnit;
			if (mass != null) {
				var num = mass.amount() + " kg";
				UI.formLabel(comp, tk, num);
				UI.filler(comp, tk);
			} else {
				var text = tk.createText(comp, "", SWT.BORDER);
				UI.fillHorizontal(text);
				update(text);
				text.addModifyListener($ -> update(text));
				UI.formLabel(comp, tk, "kg");
			}
		}

		private void update(Text text) {
			var mass = OptionalDouble.empty();
			try {
				var s = text.getText();
				if (Strings.notEmpty(s)) {
					var d = Double.parseDouble(s);
					mass = OptionalDouble.of(d);
				}
			} catch (NumberFormatException ignored) {
			}

			var epd = dialog.doc;
			if (mass.isPresent()) {
				epd.kgPerDeclaredUnit = new EpdQuantity(
					mass.getAsDouble(), "kg");
				text.setBackground(Colors.white());
				text.setToolTipText("");
			} else {
				epd.kgPerDeclaredUnit = null;
				text.setBackground(Colors.errorColor());
				text.setToolTipText("This field is requited.");
			}
		}
	}

}

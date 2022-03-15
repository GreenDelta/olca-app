package org.openlca.app.tools.openepd;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gson.GsonBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.openepd.model.Api;
import org.openlca.app.tools.openepd.model.Ec3CategoryTree;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.tools.openepd.model.Ec3ImpactModel;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;

public class EpdEditor extends SimpleFormEditor {

	final Ec3ImpactModel impactModel = Ec3ImpactModel.get();
	private final IDatabase db = Database.get();
	private final Ec3Epd epd = new Ec3Epd();
	private Ec3CategoryTree categories;

	public static void open() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return;
		}
		var id = UUID.randomUUID().toString();
		Editors.open(new SimpleEditorInput(id, "New EPD"), "EpdEditor");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		epd.isDraft = true;
		epd.isPrivate = true;
		epd.name = "New EPD";
		epd.declaredUnit = "1 kg";
		var today = LocalDate.now();
		epd.dateOfIssue = today;
		epd.dateValidityEnds = LocalDate.of(
			today.getYear() + 1, today.getMonth(), today.getDayOfMonth());
		categories = Ec3CategoryTree.loadFromCacheFile();
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		private final List<ResultSection> sections = new ArrayList<>();

		public Page() {
			super(EpdEditor.this, "EpdEditor.Page", "New EPD");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Building Transparency - openEPD",
				Icon.EC3_WIZARD.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var loginPanel = LoginPanel.create(body, tk);

			// info section
			var infoSection = UI.section(body, tk, M.GeneralInformation);
			var comp = UI.sectionClient(infoSection, tk, 2);
			text(UI.formText(comp, tk, M.Name),
				epd.name, s -> epd.name = s);
			text(UI.formText(comp, tk, "Declared unit"),
				epd.declaredUnit, s -> epd.declaredUnit = s);

			// category link
			UI.formLabel(comp, tk, M.Category);
			var categoryLink = tk.createImageHyperlink(comp, SWT.NONE);
			categoryLink.setText(epd.category == null
				? " - none -"
				: categories.pathOf(epd.category));
			Controls.onClick(categoryLink, $ -> {
				if (categories.isEmpty()) {
					var client = loginPanel.login().orElse(null);
					if (client == null)
						return;
					categories = Api.getCategoryTree(client);
					if (categories.isEmpty()) {
						MsgBox.error("No categories could be loaded",
							"No categories could be loaded from "
								+ loginPanel.credentials().ec3Url());
						return;
					}
				}
				epd.category = CategoryDialog.selectFrom(categories);
				if (epd.category != null) {
					epd.categoryId = epd.category.id;
				}
				categoryLink.setText(epd.category == null
					? " - none -"
					: categories.pathOf(epd.category));
				categoryLink.getParent().layout();
			});

			// date fields
			UI.formLabel(comp, tk, "Date of issue");
			var issueDate = new DateTime(comp, SWT.DROP_DOWN);
			UI.gridData(issueDate, false, false).widthHint = 120;
			tk.adapt(issueDate);
			date(issueDate, epd.dateOfIssue, d -> epd.dateOfIssue = d);
			UI.formLabel(comp, tk, "End of validity");
			var endDate = new DateTime(comp, SWT.DROP_DOWN);
			UI.gridData(endDate, false, false).widthHint = 120;
			tk.adapt(endDate);
			date(endDate, epd.dateValidityEnds, d -> epd.dateValidityEnds = d);

			// description
			text(UI.formMultiText(comp, tk, "Description"),
				epd.description, s -> epd.description = s);

			// create the buttons
			UI.filler(comp, tk);
			var buttonComp = tk.createComposite(comp);
			UI.gridLayout(buttonComp, 3, 10, 5);
			var resultButton = tk.createButton(buttonComp, "Add result", SWT.PUSH);
			resultButton.setImage(Images.get(ModelType.RESULT));
			var fileButton = tk.createButton(buttonComp, "Save as file", SWT.PUSH);
			fileButton.setImage(Icon.FILE.get());
			var uploadButton = tk.createButton(buttonComp, "Upload to EC3", SWT.PUSH);
			uploadButton.setImage(Icon.EXPORT.get());

			// layout the buttons nicely
			var buttons = List.of(resultButton, fileButton, uploadButton);
			int maxWidth = 0;
			for (var b : buttons) {
				b.pack();
				maxWidth = Math.max(maxWidth, b.getBounds().width);
			}
			for (var b : buttons) {
				UI.gridData(b, false, false).widthHint = maxWidth;
			}

			// add result
			Controls.onSelect(resultButton, $ -> {
				var ds = ModelSelector.multiSelect(ModelType.RESULT);
				if (ds == null)
					return;
				for (var d : ds) {
					var model = db.get(Result.class, d.id);
					if (model == null)
						return;
					var section = ResultSection.of(EpdEditor.this, model)
						.render(body, tk)
						.onDelete(s -> {
							sections.remove(s);
							form.reflow(true);
						});
					sections.add(section);
				}
				form.reflow(true);
			});

			// save as file
			Controls.onSelect(fileButton, $ -> {
				var file = FileChooser.forSavingFile(
					"Save as openEPD document",
					epd.name + ".json");
				if (file == null)
					return;
				mergeResults();
				var json = new GsonBuilder().setPrettyPrinting()
					.create()
					.toJson(epd.toJson());
				try {
					Files.writeString(file.toPath(), json);
				} catch (Exception e) {
					ErrorReporter.on("Failed to save EPD", e);
				}
			});

			// upload to EC3
			Controls.onSelect(uploadButton, $ -> {
				var client = loginPanel.login().orElse(null);
				if (client == null)
					return;
				var b = Question.ask("Upload as draft?",
					"Upload this as draft to " + loginPanel.credentials().ec3Url() + "?");
				if (!b)
					return;
				mergeResults();
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
					if (obj.has("validation_errors")) {
						ErrorDialog.show(obj);
						return;
					}
					var url = loginPanel.credentials().epdUrl();
					Popup.info("Uploaded EPD",
						"The EPD was uploaded to <a href='" + url + "'>" + url + "</a>");
				} catch (Exception e) {
					ErrorReporter.on("Failed to upload EPD", e);
				}
			});
		}

		private void text(Text text, String initial, Consumer<String> onChange) {
			if (initial != null) {
				text.setText(initial);
			}
			text.addModifyListener($ -> onChange.accept(text.getText()));
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

		private void mergeResults() {
			epd.impactResults.clear();

			for (var section : sections) {
				var nextResult = section.createEpdResult();

				// check the method
				var epdResult = epd.impactResults.stream()
					.filter(r -> Objects.equals(r.method(), nextResult.method()))
					.findAny()
					.orElse(null);
				if (epdResult == null) {
					epd.impactResults.add(nextResult);
					continue;
				}

				for (var nextIndicator : nextResult.indicatorResults()) {

					// check the indicator
					var epdIndicator = epdResult.indicatorResults().stream()
						.filter(i -> Objects.equals(nextIndicator.indicator(), i.indicator()))
						.findAny()
						.orElse(null);
					if (epdIndicator == null) {
						epdResult.indicatorResults().add(nextIndicator);
						continue;
					}

					// add next scope values
					for (var v : nextIndicator.values()) {
						epdIndicator.values().add(v);
					}
				}
			}
		}
	}
}

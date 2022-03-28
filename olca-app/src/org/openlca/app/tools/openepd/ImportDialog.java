package org.openlca.app.tools.openepd;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.ImportLogView;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Epd;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.input.EpdImport;
import org.openlca.io.openepd.input.ImportMapping;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final EpdDoc epdDoc;
	final ImportMapping mapping;
	private final AtomicBoolean mappingChanged = new AtomicBoolean(false);

	public static int show(EpdDoc doc) {
		if (doc == null)
			return -1;
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return -1;
		}
		var epd = db.get(Epd.class, doc.id);
		if (epd != null) {
			MsgBox.error("EPD already exists",
				"An EPD with ID='" + epd.id + "' already exists in the database.");
			return -1;
		}
		return new ImportDialog(doc, db).open();
	}

	private ImportDialog(EpdDoc epdDoc, IDatabase db) {
		super(UI.shell());
		this.epdDoc = Objects.requireNonNull(epdDoc);
		this.db = Objects.requireNonNull(db);
		this.mapping = ImportMapping.init(epdDoc, db);
	}

	void setMappingChanged() {
		mappingChanged.set(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Import an openEPD document");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		createProductSection(body, tk);
		ImpactSection.initAllOf(this)
			.forEach(section -> section.render(body, tk));
	}

	private void createProductSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Declared product");

		// name
		var nameText = UI.formText(comp, tk, M.Product);
		nameText.setEditable(false);
		Controls.set(nameText, epdDoc.productName);

		// category
		var categoryText = UI.formText(comp, tk, M.Category);
		categoryText.setEditable(false);
		Controls.set(categoryText,
			EpdImport.categoryOf(epdDoc)
				.map(path -> String.join(" >> ", path))
				.orElse("- none -"));

		// amount
		var amountText = UI.formText(comp, tk, "Declared unit");
		amountText.setEditable(false);
		var quantity = mapping.quantity();
		if (quantity.hasUnit()) {
			Controls.set(amountText, quantity.amount() +
				" " + quantity.unit().name);
		} else {
			Controls.set(amountText, quantity.amount() +
				" ERROR! could not find matching unit in openLCA");
		}
	}

	@Override
	protected void okPressed() {

		if (mapping.hasEmptyMappings()) {
			boolean b = Question.ask("Missing mappings",
				"Not all of the used impact assessment methods and categories of " +
					"the openEPD document are mapped to corresponding impact assessment " +
					"methods and categories in openLCA. These results will be skipped. " +
					"Do you want to continue?");
			if (!b) {
				return;
			}
		}

		if (mappingChanged.get()) {
			boolean b = Question.ask("Save indicator mappings?",
				"Should the assigned mapping codes of the LCIA" +
					" methods and indicators be saved in the database?");
			if (b) {
				mapping.persistIn(db);
			}
		}

		try {
			var imp = new EpdImport(db, epdDoc, mapping);
			var epd = imp.run();

			var msg = new MessageDialog(
				UI.shell(),
				"Import finished",
				null,
				"Imported EPD and related data sets.",
				MessageDialog.INFORMATION,
				new String[]{
					IDialogConstants.OK_LABEL, "Open EPD", "Import details"},
				0);
			msg.setBlockOnOpen(true);
			var state = msg.open();
			if (state == 1) {
				App.open(epd);
			} else if (state == 2) {
				ImportLogView.open(imp.log());
			}

		} catch (Exception e) {
			ErrorReporter.on("failed to save EPD", e);
			return;
		}

		Navigator.refresh();
		super.okPressed();
	}
}

package org.openlca.app.tools.openepd.input;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Epd;
import org.openlca.core.model.FlowProperty;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final Ec3Epd epdDoc;
	final ImportMapping mapping;
	private final AtomicBoolean mappingChanged = new AtomicBoolean(false);

	public static int show(Ec3Epd doc) {
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

	private ImportDialog(Ec3Epd epdDoc, IDatabase db) {
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
		return new Point(800, 700);
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
		var comp = UI.formSection(body, tk, M.Product);

		// name
		var nameText = UI.formText(comp, tk, M.Name);
		Controls.set(nameText , epdDoc.productName);

		// category
		var categoryText = UI.formText(comp, tk, M.Category);
		Controls.set(categoryText, Util.categoryOf(epdDoc));

		// amount
		var amountText = UI.formText(comp, tk, M.Amount);
		Controls.set(amountText, Double.toString(mapping.quantity().amount()));

		// quantity
		var quantityCombo = EntityCombo.of(
				UI.formCombo(comp, tk, M.Quantity), FlowProperty.class, db)
			.select(mapping.quantity().property());

		// unit
		// TODO: checks..
		var unitCombo = EntityCombo.of(
				UI.formCombo(comp, tk, M.Unit), mapping.quantity().property().unitGroup.units)
			.select(mapping.quantity().unit());
			// .onSelected(unit -> product.unit = unit);

		/*
		quantityCombo.onSelected(property -> {
			Util.setFlowProperty(product, property);
			unitCombo.update(Util.allowedUnitsOf(product))
				.select(product.unit);
		});

		 */
	}

	@Override
	protected void okPressed() {

		if (mappingChanged.get()) {
			boolean b = Question.ask("Save indicator mappings?",
				"Should the assigned mapping codes of the LCIA" +
					" methods and indicators be saved in the database?");
			if (b) {
				mapping.persistIn(db);
			}
		}

		try {
			var imp = new Import(db, epdDoc, mapping);
			imp.run();
		} catch (Exception e) {
			ErrorReporter.on("failed to save EPD", e);
			return;
		}

		Navigator.refresh();
		super.okPressed();
	}
}

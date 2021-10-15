package org.openlca.app.editors.results.openepd.input;

import java.util.Objects;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.db.Database;
import org.openlca.app.editors.results.openepd.model.Ec3CategoryIndex;
import org.openlca.app.editors.results.openepd.model.Ec3Epd;
import org.openlca.app.editors.results.openepd.model.Ec3ImpactModel;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ResultFlow;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final Ec3Epd epd;
	final Ec3ImpactModel impactModel;
	private final ResultFlow product;

	public static void show(Ec3Epd epd) {
		show(epd, Ec3CategoryIndex.empty());
	}

	public static int show(Ec3Epd epd, Ec3CategoryIndex categories) {
		if (epd == null)
			return -1;
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened);
			return -1;
		}
		return new ImportDialog(epd, db).open();
	}

	private ImportDialog(Ec3Epd epd, IDatabase db) {
		super(UI.shell());
		this.epd = Objects.requireNonNull(epd);
		this.db = Objects.requireNonNull(db);
		this.impactModel = Ec3ImpactModel.get();
		product = Util.initQuantitativeReference(epd, db);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Import results from an OpenEPD document");
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

		var resultSections = ResultSection.initAllOf(this);
		for (var section : resultSections) {
			section.render(body, tk);
		}

	}

	private void createProductSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Product);
		var nameText = UI.formText(comp, tk, M.Name);
		Controls.set(nameText, product.flow.name, name -> product.flow.name = name);
		var amountText = UI.formText(comp, tk, M.Amount);
		Controls.set(amountText, product.amount, amount -> product.amount = amount);

		var quantityCombo = EntityCombo.of(
				UI.formCombo(comp, tk, M.Quantity), FlowProperty.class, db)
			.select(product.flowPropertyFactor != null
				? product.flowPropertyFactor.flowProperty
				: null);

		var unitCombo = EntityCombo.of(
				UI.formCombo(comp, tk, M.Unit), Util.allowedUnitsOf(product))
			.select(product.unit)
			.onSelected(unit -> product.unit = unit);

		quantityCombo.onSelected(property -> {
			Util.setFlowProperty(product, property);
			unitCombo.update(Util.allowedUnitsOf(product))
				.select(product.unit);
		});

	}


}

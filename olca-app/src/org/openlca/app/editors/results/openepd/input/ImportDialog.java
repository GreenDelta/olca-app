package org.openlca.app.editors.results.openepd.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultFlow;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final Ec3Epd epd;
	final Ec3ImpactModel impactModel;
	private final ResultFlow product;
	private String categoryPath;
	private final List<ResultSection> sections = new ArrayList<>();

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
		return new ImportDialog(epd, categories, db).open();
	}

	private ImportDialog(Ec3Epd epd, Ec3CategoryIndex categories, IDatabase db) {
		super(UI.shell());
		this.epd = Objects.requireNonNull(epd);
		this.db = Objects.requireNonNull(db);
		this.impactModel = Ec3ImpactModel.get();
		this.categoryPath = categories.pathOf(epd.category);
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
			sections.add(section);
			section.render(body, tk);
		}
	}

	private void createProductSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Product);

		// name
		var nameText = UI.formText(comp, tk, M.Name);
		Controls.set(nameText, product.flow.name, name -> product.flow.name = name);

		// category
		var categoryText = UI.formText(comp, tk, M.Category);
		Controls.set(categoryText, categoryPath, path -> categoryPath = path);

		// amount
		var amountText = UI.formText(comp, tk, M.Amount);
		Controls.set(amountText, product.amount, amount -> product.amount = amount);

		// quantity
		var quantityCombo = EntityCombo.of(
				UI.formCombo(comp, tk, M.Quantity), FlowProperty.class, db)
			.select(product.flowPropertyFactor != null
				? product.flowPropertyFactor.flowProperty
				: null);

		// unit
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

	@Override
	protected void okPressed() {

		// check and prepare the results
		var results = new ArrayList<ResultModel>();
		for (var section : sections) {
			var result = section.createResult();
			if (Strings.nullOrEmpty(result.name)) {
				MsgBox.error("Invalid result",
					"Result has an empty name.");
				return;
			}
			results.add(result);
		}
		if (results.isEmpty()) {
			// create a default result in no LCIA results could
			// be found in the EPD
			results.add(ResultModel.of(epd.name));
		}

		// create the reference product
		var productFlow = createProduct();
		if (productFlow == null)
			return;

		// save the results
		try {
			var resultCategory = syncCategory(ModelType.RESULT);
			for (var result : results) {
				var refFlow = product.copy();
				refFlow.flow = productFlow;
				// unit and amount are correctly set but need to sync
				// the flow property factor
				refFlow.flowPropertyFactor = productFlow.getReferenceFactor();
				result.referenceFlow = refFlow;
				result.inventory.add(refFlow);
				result.category = resultCategory;
				result.lastChange = System.currentTimeMillis();
				db.insert(result);
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to save results", e);
		}

		Navigator.refresh();
		super.okPressed();
	}

	private Flow createProduct() {
		var productFlow = product.flow;
		var error = validateProduct(productFlow);
		if (error != null) {
			MsgBox.error("Invalid product", error);
			return null;
		}
		try {
			productFlow.category = syncCategory(ModelType.FLOW);
			return db.insert(productFlow);
		} catch (Exception e) {
			ErrorReporter.on("failed to save product flow", e);
			return null;
		}
	}

	private Category syncCategory(ModelType type) {
		if (categoryPath == null)
			return null;
		var path = Arrays.stream(categoryPath.split("/"))
			.map(String::trim)
			.filter(p -> !p.isEmpty())
			.toArray(String[]::new);
		return path.length == 0
			? null
			: CategoryDao.sync(db, type, path);
	}

	private String validateProduct(Flow flow) {
		if (flow == null)
			return "No product flow defined";
		if (Strings.nullOrEmpty(flow.name))
			return "Product name is empty.";
		if (flow.getReferenceFactor() == null)
			return "Product flow has no flow property";
		if (flow.getReferenceUnit() == null)
			return "Product flow has no reference unit";
		return null;
	}

}

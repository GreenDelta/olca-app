package org.openlca.app.tools.openepd.input;

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
import org.openlca.app.navigation.Navigator;
import org.openlca.app.tools.openepd.model.Ec3Epd;
import org.openlca.app.tools.openepd.model.Ec3ImpactModel;
import org.openlca.app.tools.openepd.model.Ec3Org;
import org.openlca.app.tools.openepd.model.Ec3Pcr;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.EpdProduct;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Source;
import org.openlca.core.model.Version;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final Ec3Epd epdDoc;
	final Ec3ImpactModel impactModel;
	final ImportMapping mapping;
	private final FlowResult product;
	private String categoryPath;
	private final List<ModuleSection> sections = new ArrayList<>();

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
		this.impactModel = Ec3ImpactModel.get();
		this.mapping = ImportMapping.init(epdDoc, db);
		categoryPath = Util.categoryOf(epdDoc);
		product = Util.initQuantitativeReference(epdDoc, db);
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
		for (var section : ModuleSection.initAllOf(this)) {
			section.render(body, tk);
			section.onDeleted(s -> {
				sections.remove(s);
				mForm.reflow(true);
			});
			sections.add(section);
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

		// create the reference product
		var productFlow = createProduct();
		if (productFlow == null)
			return;

		// prepare the modules
		var modules = new ArrayList<EpdModule>();
		for (var section : sections) {
			modules.add(section.createModule());
		}

		// save the results and the EPD
		try {

			for (var module : modules) {
				var result = module.result;
				if (result != null) {
					var refFlow = product.copy();
					refFlow.flow = productFlow;
					refFlow.flowPropertyFactor = productFlow.getReferenceFactor();
					result.referenceFlow = refFlow;
					result.flowResults.add(refFlow);
					result.category = syncCategory(ModelType.RESULT);
					result.lastChange = System.currentTimeMillis();
					module.result = db.insert(result);
				}
			}

			var epd = new Epd();
			epd.name = productFlow.name;
			epd.refId = epdDoc.id;
			epd.description = epdDoc.lcaDiscussion;
			epd.modules.addAll(modules);
			epd.urn = "openEPD:" + epdDoc.id;
			epd.category = syncCategory(ModelType.EPD);
			epd.lastChange = System.currentTimeMillis();
			epd.product = EpdProduct.of(productFlow, product.amount);
			epd.product.unit = product.unit;

			epd.manufacturer = toActor(epdDoc.manufacturer);
			epd.verifier = toActor(epdDoc.verifier);
			epd.programOperator = toActor(epdDoc.programOperator);
			epd.pcr = toSource(epdDoc.pcr);

			db.insert(epd);
		} catch (Exception e) {
			ErrorReporter.on("failed to save EPD", e);
			return;
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

	private Actor toActor(Ec3Org org) {
		if (org == null || Strings.nullOrEmpty(org.name))
			return null;
		var id = org.id;
		if (Strings.nullOrEmpty(id)) {
			id = Strings.notEmpty(org.ref)
				? KeyGen.get(org.ref)
				: KeyGen.get(org.name);
		}
		var actor = db.get(Actor.class, id);
		if (actor != null)
			return actor;
		actor = Actor.of(org.name);
		actor.refId = id;
		actor.website = org.website;
		actor.address = org.address;
		actor.country = org.country;
		return db.insert(actor);
	}

	private Source toSource(Ec3Pcr pcr) {
		if (pcr == null || Strings.nullOrEmpty(pcr.name))
			return null;
		var id = pcr.id;
		if (Strings.nullOrEmpty(id)) {
			id = Strings.notEmpty(pcr.ref)
				? KeyGen.get(pcr.ref)
				: KeyGen.get(pcr.name);
		}
		var source = db.get(Source.class, id);
		if (source != null)
			return source;
		source = Source.of(pcr.name);
		source.refId = id;
		source.url = pcr.ref;
		source.version = Version.fromString(pcr.version).getValue();
		return db.insert(source);
	}
}

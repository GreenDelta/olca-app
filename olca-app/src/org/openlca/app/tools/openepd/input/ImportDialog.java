package org.openlca.app.tools.openepd.input;

import java.util.Objects;

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
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.io.ImportLogView;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Epd;
import org.openlca.io.UnitMapping;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.io.EpdImport;
import org.openlca.io.openepd.io.MappingModel;

public class ImportDialog extends FormDialog {

	final IDatabase db;
	final EpdDoc epdDoc;
	final MappingModel mapping;

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
			MsgBox.error(M.EpdAlreadyExists,
					M.EpdAlreadyExistsErr + " - " + epd.refId);
			return -1;
		}
		var mapping = ImpactMappings.askCreate(db, doc);
		return new ImportDialog(doc, db, mapping).open();
	}

	private ImportDialog(EpdDoc epdDoc, IDatabase db, MappingModel mapping) {
		super(UI.shell());
		this.epdDoc = Objects.requireNonNull(epdDoc);
		this.db = Objects.requireNonNull(db);
		this.mapping = mapping;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.ImportAnOpenEpdDocument);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		createProductSection(body, tk);
		MappingSection.initAllOf(this)
			.forEach(section -> section.render(body, tk));
	}

	private void createProductSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.DeclaredProduct);

		// name
		var nameText = UI.labeledText(comp, tk, M.Product);
		nameText.setEditable(false);
		Controls.set(nameText, epdDoc.productName);

		// category
		var categoryText = UI.labeledText(comp, tk, M.Category);
		categoryText.setEditable(false);
		Controls.set(categoryText,
			EpdImport.categoryOf(epdDoc)
				.map(path -> String.join(" >> ", path))
				.orElse(M.NoneHyphen));

		// amount
		var amountText = UI.labeledText(comp, tk, M.DeclaredUnit);
		amountText.setEditable(false);
		amountText.setText(getDeclaredUnit());
	}

	private String getDeclaredUnit() {
		if (epdDoc.declaredUnit == null)
			return M.ErrorNoDeclaredUnitAvailable;
		var unit = epdDoc.declaredUnit.unit();
		var uMap = UnitMapping.createDefault(db);
		var u = uMap.getEntry(unit);
		if (u == null)
			return "ERROR! no matching unit for: " + unit;
		return epdDoc.declaredUnit.amount() + " " + Labels.name(u.unit);
	}

	@Override
	protected void okPressed() {

		if (mapping.hasEmptyMappings()) {
			boolean b = Question.ask(M.MissingMappings,
					M.NotAllMethodsAreMapped + M.DoYouWantToContinue);
			if (!b) {
				return;
			}
		}

		try {
			var imp = new EpdImport(db, epdDoc, mapping);
			var epd = imp.run();

			var msg = new MessageDialog(
				UI.shell(),
				M.ImportFinished,
				null,
				M.ImportedEpdAndDataSets,
				MessageDialog.INFORMATION,
				new String[]{
					IDialogConstants.OK_LABEL, "Open EPD", M.ImportDetails},
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

package org.openlca.app.editors.results.openepd.input;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.results.openepd.model.Ec3CategoryIndex;
import org.openlca.app.editors.results.openepd.model.Ec3Epd;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ResultFlow;
import org.openlca.core.model.ResultModel;

public class ImportDialog extends FormDialog {

	private final IDatabase db;
	private final Ec3Epd epd;
	private final List<ResultModel> results = new ArrayList<>();
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
		this.epd = epd;
		this.db = db;
		product = new ResultFlow();
		product.flow = new Flow();

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
	}

	private void createProductSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Product);
		var nameText = UI.formText(comp, tk, M.Name);


	}


}

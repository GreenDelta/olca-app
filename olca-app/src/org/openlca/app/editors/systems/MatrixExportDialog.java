package org.openlca.app.editors.systems;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ProductSystem;

public class MatrixExportDialog extends FormDialog {

	private final IDatabase db;
	private final Optional<ProductSystem> system;

	/**
	 * Opens a dialog for exporting the complete database in to
	 * a matrix format.
	 */
	public static void open(IDatabase db) {
		open(db, null);
	}

	/**
	 * Opens a dialog for exporting the given product system into
	 * a matrix format.
	 */
	public static void open(IDatabase db, ProductSystem system) {
		if (db == null)
			return;
		new MatrixExportDialog(db, system).open();
	}

	private MatrixExportDialog(IDatabase db, ProductSystem system) {
		super(UI.shell());
		this.db = Objects.requireNonNull(db);
		this.system = Optional.ofNullable(system);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export matrices");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);

	}
}

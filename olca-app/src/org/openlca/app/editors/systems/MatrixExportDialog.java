package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.Strings;

public class MatrixExportDialog extends FormDialog {

	private final IDatabase db;
	private final Optional<ProductSystem> system;
	private final Config config = new Config();

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

		parametersCombo(body);
		allocationCombo(body);
		methodCombo(body);

	}

	private void parametersCombo(Composite comp) {
		if (this.system.isEmpty())
			return;
		var system = this.system.get();
		var paramSets = new ArrayList<>(system.parameterSets);

		paramSets.sort((s1, s2) -> {
			if (s1.isBaseline)
				return -1;
			if (s2.isBaseline)
				return 1;
			return Strings.compare(s1.name, s2.name);
		});

		UI.formLabel(comp, "Parameter set");
		var combo = new TableCombo(comp,
			SWT.READ_ONLY | SWT.BORDER);
		UI.gridData(combo, true, false);
		for (var paramSet : paramSets) {
			var item = new TableItem(
				combo.getTable(), SWT.NONE);
			item.setText(paramSet.name);
		}

		combo.select(0);
		config.parameters = paramSets.get(0);
		Controls.onSelect(combo, e -> {
			int i = combo.getSelectionIndex();
			config.parameters = paramSets.get(i);
		});
	}

	private void allocationCombo(Composite comp) {
		UI.formLabel(comp, M.AllocationMethod);
		var combo = new AllocationCombo(
			comp, AllocationMethod.values());
		combo.setNullable(false);
		combo.select(AllocationMethod.USE_DEFAULT);
		combo.addSelectionChangedListener(
			method -> config.allocation = method);
	}

	private void methodCombo(Composite comp) {
		UI.formLabel(comp, M.ImpactAssessmentMethod);
		var combo = new ImpactMethodViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		combo.addSelectionChangedListener(
			_e -> config.impactMethod = combo.getSelected());
	}

	private static class Config {
		AllocationMethod allocation;
		ImpactMethodDescriptor impactMethod;
		ParameterRedefSet parameters;
	}
}

package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.util.Strings;

import java.util.List;

class GeoFactorDialog extends FormDialog {

	private final GeoPage page;
	private final List<ImpactFactor> factors;

	static void open(GeoPage page, List<ImpactFactor> factors) {
		new GeoFactorDialog(page, factors).open();
	}

	private GeoFactorDialog(GeoPage page, List<ImpactFactor> factors) {
		super(UI.shell());
		this.page = page;
		this.factors = factors;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Calculated factors");
	}

	@Override
	protected void createButtonsForButtonBar(Composite comp) {
		createButton(comp, IDialogConstants.OK_ID,
				"Add new factors", true);
		createButton(comp, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		var table = Tables.createViewer(body,
				M.Flow, M.Category, M.Factor, M.Unit, M.Location);
		Tables.bindColumnWidths(table, 0.3, 0.25, 0.15, 0.15, 0.15);
		table.setLabelProvider(new FactorLabel(page));
		table.setInput(factors);
	}

	private static class FactorLabel extends LabelProvider
			implements ITableLabelProvider {

		private final ImpactCategory impact;

		FactorLabel(GeoPage page) {
			this.impact = page.editor.getModel();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImpactFactor f))
				return null;
			return col == 0
					? Images.get(f.flow)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactFactor f))
				return null;
			return switch (col) {
				case 0 -> Labels.name(f.flow);
				case 1 -> Labels.category(f.flow);
				case 2 -> Numbers.format(f.value);
				case 3 -> unitOf(f);
				case 4 -> Labels.code(f.location);
				default -> null;
			};
		}

		private String unitOf(ImpactFactor f) {
			if (f.unit == null)
				return null;
			return Strings.notEmpty(impact.referenceUnit)
					? impact.referenceUnit + "/" + f.unit.name
					: "1/" + f.unit.name;
		}
	}
}

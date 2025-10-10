package org.openlca.app.editors.sd.editor;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.editors.sd.SdVars;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.sd.eqn.Var;

public class VarsPage extends FormPage {

	private final SdModelEditor editor;

	public VarsPage(SdModelEditor editor) {
		super(editor, "SdModelParametersPage", "Variables");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var section = UI.section(body, tk, "Variables");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var table = Tables.createViewer(comp,
				"Type", "Name", "Cell type", "Unit");
		Tables.bindColumnWidths(table, 0.10, 0.40, 0.40, 0.10);
		UI.gridData(table.getControl(), true, true);
		table.setLabelProvider(new VarsLabelProvider());
		table.setInput(editor.vars());
	}

	private static class VarsLabelProvider
			extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 ? Icon.FORMULA.get() : null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Var v))
				return null;
			return switch (col) {
				case 0 -> SdVars.typeOf(v);
				case 1 -> v.name().label();
				case 2 -> SdVars.cellTypeOf(v);
				case 3 -> v.unit();
				default -> null;
			};
		}
	}
}

package org.openlca.app.results;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.results.ContributionResult;

public class InventoryPage2 extends FormPage {

	private final ResultEditor<?> editor;
	private final CalculationSetup setup;
	private final ContributionResult result;
	private final DQResult dqResult;

	public InventoryPage2(ResultEditor<?> editor) {
		super(editor, "InventoryPage2", M.InventoryResults);
		this.editor = editor;
		this.result = editor.result;
		this.setup = editor.setup;
		this.dqResult = editor.dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.name(setup.productSystem),
				Images.get(result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		tk.paintBordersFor(body);

		DataLayer data = new DataLayer(new DataProvider());
		SelectionLayer selection = new SelectionLayer(data);
		ViewportLayer viewPort = new ViewportLayer(selection);
		viewPort.setRegionName(GridRegion.BODY);

		DataLayer header = new DataLayer(new ColumnHeader());
		ILayer columnLayer = new ColumnHeaderLayer(
				header, viewPort, selection);

		CompositeLayer compositeLayer = new CompositeLayer(1, 2);
		compositeLayer.setChildLayer(GridRegion.COLUMN_HEADER, columnLayer, 0, 0);
		compositeLayer.setChildLayer(GridRegion.BODY, viewPort, 0, 1);

		NatTable nat = new NatTable(body, compositeLayer);
		nat.setTheme(new ModernNatTableThemeConfiguration());
		nat.setBackground(Colors.white());

		UI.gridData(nat, true, true);
		form.reflow(true);
	}

	private class DataProvider implements IDataProvider {

		@Override
		public Object getDataValue(int column, int row) {
			IndexFlow flow = result.getFlows().get(row);
			if (flow == null)
				return null;
			switch (column) {
			case 0:
				return Labels.name(flow);
			case 1:
				return Labels.category(flow.flow);
			case 2:
				return result.getTotalFlowResult(flow);
			case 3:
				return Labels.refUnit(flow);
			default:
				return null;
			}
		}

		@Override
		public void setDataValue(int column, int row, Object newValue) {
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return result.getFlows().size();
		}
	}

	private class ColumnHeader implements IDataProvider {

		@Override
		public Object getDataValue(int col, int row) {
			switch (col) {
			case 0:
				return M.Flow;
			case 1:
				return M.Category;
			case 2:
				return M.Amount;
			case 3:
				return M.Unit;
			default:
				return null;
			}
		}

		@Override
		public void setDataValue(int col, int row, Object val) {
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return 1;
		}

	}

}

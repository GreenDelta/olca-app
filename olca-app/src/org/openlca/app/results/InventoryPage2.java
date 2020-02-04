package org.openlca.app.results;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.painter.layer.NatGridLayerPainter;
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

		// data + selection + view port
		DataProvider dataProvider = new DataProvider();
		DataLayer dataLayer = new DataLayer(dataProvider);
		SelectionLayer selectionLayer = new SelectionLayer(dataLayer);
		ViewportLayer viewPort = new ViewportLayer(selectionLayer);
		viewPort.setRegionName(GridRegion.BODY);

		// column header
		ColumnHeader columnHeaderProvider = new ColumnHeader();
		ILayer columnHeaderLayer = new ColumnHeaderLayer(
				new DataLayer(columnHeaderProvider),
				viewPort, selectionLayer);

		// row header
		IDataProvider rowHeaderProvider = new DefaultRowHeaderDataProvider(dataProvider);
		ILayer rowHeaderLayer = new RowHeaderLayer(new DataLayer(rowHeaderProvider),
				viewPort, selectionLayer);

		// corner
		DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(
				columnHeaderProvider, rowHeaderProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		CornerLayer cornerLayer = new CornerLayer(cornerDataLayer,
				rowHeaderLayer, columnHeaderLayer);

		// grid layer
		GridLayer gridLayer = new GridLayer(viewPort, columnHeaderLayer,
				rowHeaderLayer, cornerLayer);

		NatTable nat = new NatTable(body, gridLayer);
		tk.paintBordersFor(nat);

		// styling
		nat.setTheme(new ModernNatTableThemeConfiguration());
		nat.setBackground(Colors.white());
		nat.setLayerPainter(
				new NatGridLayerPainter(nat, DataLayer.DEFAULT_ROW_HEIGHT));

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

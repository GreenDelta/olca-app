package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeData;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.tree.GlazedListTreeRowModel;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.painter.cell.BackgroundPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.PaddingDecorator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.theme.ModernNatTableThemeConfiguration;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.tree.config.TreeConfigAttributes;
import org.eclipse.nebula.widgets.nattable.tree.painter.IndentedTreeImagePainter;
import org.eclipse.nebula.widgets.nattable.tree.painter.TreeImagePainter;
import org.eclipse.nebula.widgets.nattable.ui.util.CellEdgeEnum;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
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
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.util.Strings;

import com.google.common.primitives.Doubles;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.TreeList;

public class InventoryPage2 extends FormPage {

	private final ResultEditor<?> editor;
	private final CalculationSetup setup;
	private final ContributionResult result;
	private final DQResult dqResult;

	private final Map<IndexFlow, Item> rootItems = new HashMap<>();
	private final List<Item> items = new ArrayList<>();

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

		makeItems();
		// data + selection + view port
		ItemDataLayer dataLayer = new ItemDataLayer();

		/*
		 * DataProvider dataProvider = new DataProvider(); DataLayer dataLayer = new
		 * DataLayer(dataProvider); SelectionLayer selectionLayer = new
		 * SelectionLayer(dataLayer); ViewportLayer viewPort = new
		 * ViewportLayer(selectionLayer); viewPort.setRegionName(GridRegion.BODY);
		 */

		// column header
		ColumnHeader columnHeaderProvider = new ColumnHeader();
		ILayer columnHeaderLayer = new ColumnHeaderLayer(
				new DataLayer(columnHeaderProvider),
				dataLayer, dataLayer.selectionLayer);

		// row header
		IDataProvider rowHeaderProvider = new DefaultRowHeaderDataProvider(dataLayer.bodyDataProvider);
		ILayer rowHeaderLayer = new RowHeaderLayer(new DataLayer(rowHeaderProvider),
				dataLayer, dataLayer.selectionLayer);

		// corner
		DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(
				columnHeaderProvider, rowHeaderProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		CornerLayer cornerLayer = new CornerLayer(cornerDataLayer,
				rowHeaderLayer, columnHeaderLayer);

		// grid layer
		GridLayer gridLayer = new GridLayer(dataLayer, columnHeaderLayer,
				rowHeaderLayer, cornerLayer);

		NatTable nat = new NatTable(body, gridLayer, false);
		tk.paintBordersFor(nat);



		nat.setConfigRegistry(new ConfigRegistry());
		nat.addConfiguration(new DefaultNatTableStyleConfiguration());
		nat.addConfiguration(new AbstractRegistryConfiguration() {

			@Override
			public void configureRegistry(IConfigRegistry configRegistry) {
				TreeImagePainter treeImagePainter = new TreeImagePainter(
						false,
						GUIHelper.getImage("right"), //$NON-NLS-1$
						GUIHelper.getImage("right_down"), null); //$NON-NLS-1$
				ICellPainter treeStructurePainter = new BackgroundPainter(
						new PaddingDecorator(
								new IndentedTreeImagePainter(10,
										null, CellEdgeEnum.LEFT, treeImagePainter,
										false, 2, true),
								0, 5, 0, 5, false));

				configRegistry.registerConfigAttribute(
						TreeConfigAttributes.TREE_STRUCTURE_PAINTER,
						treeStructurePainter,
						DisplayMode.NORMAL);

			}
		});

		// styling
		nat.setTheme(new ModernNatTableThemeConfiguration());
		nat.setBackground(Colors.white());
//		nat.setLayerPainter(
//				new NatGridLayerPainter(nat, DataLayer.DEFAULT_ROW_HEIGHT));

		nat.configure();

		UI.gridData(nat, true, true);
		form.reflow(true);
	}

	private void makeItems() {
		if (!result.hasFlowResults())
			return;
		for (IndexFlow f : result.getFlows()) {
			double amount = result.getTotalFlowResult(f);
			Item root = new Item();
			root.flow = f;
			root.amount = amount;
			items.add(root);
			rootItems.put(f, root);
			for (CategorizedDescriptor p : result.getProcesses()) {
				double contribution = result.getDirectFlowResult(p, f);
				Item item = new Item();
				item.process = p;
				item.flow = f;
				item.amount = contribution;
				items.add(item);
			}
		}
	}

	class ItemDataLayer extends AbstractLayerTransform {

		private final TreeList<Item> treeList;

		private final IRowDataProvider<Item> bodyDataProvider;

		private final SelectionLayer selectionLayer;

		public ItemDataLayer() {

			EventList<Item> eventList = GlazedLists.eventList(items);
			TransformedList<Item, Item> rowObjectsGlazedList = GlazedLists.threadSafeList(eventList);

			SortedList<Item> sortedList = new SortedList<>(rowObjectsGlazedList, null);
			// wrap the SortedList with the TreeList
			this.treeList = new TreeList<Item>(sortedList, new TreeFormat(), TreeList.nodesStartExpanded());

			this.bodyDataProvider = new ListDataProvider<Item>(
					this.treeList, new ColumnAccessor());
			DataLayer bodyDataLayer = new DataLayer(this.bodyDataProvider);

			// layer for event handling of GlazedLists and PropertyChanges
			GlazedListsEventLayer<Item> glazedListsEventLayer = new GlazedListsEventLayer<>(
					bodyDataLayer, this.treeList);

			GlazedListTreeData<Item> treeData = new GlazedListTreeData<>(this.treeList);
			ITreeRowModel<Item> treeRowModel = new GlazedListTreeRowModel<>(treeData);

			this.selectionLayer = new SelectionLayer(glazedListsEventLayer);

			TreeLayer treeLayer = new TreeLayer(this.selectionLayer, treeRowModel);
			ViewportLayer viewportLayer = new ViewportLayer(treeLayer);

			setUnderlyingLayer(viewportLayer);
		}

	}

	private class ColumnAccessor implements IColumnAccessor<Item> {

		@Override
		public Object getDataValue(Item item, int col) {
			switch (col) {
			case 0:
				return item.isContribution()
						? Labels.name(item.process)
						: Labels.name(item.flow);
			case 1:
				return item.isContribution()
						? Labels.category(item.process)
						: Labels.category(item.flow);
			case 2:
				return Numbers.format(item.amount);
			case 3:
				return Labels.refUnit(item.flow);
			default:
				return null;
			}
		}

		@Override
		public void setDataValue(Item item, int col, Object val) {
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

	}

	private class TreeFormat implements TreeList.Format<Item> {

		@Override
		public void getPath(List<Item> path, Item item) {
			if (item.process != null) {
				Item root = rootItems.get(item.flow);
				if (root != null) {
					path.add(root);
				}
			}
			path.add(item);
		}

		@Override
		public boolean allowsChildren(Item item) {
			return true;
		}

		@Override
		public Comparator<? super Item> getComparator(int depth) {
			if (depth == 0) {
				return (i1, i2) -> {
					String name1 = Labels.name(i1.flow);
					String name2 = Labels.name(i2.flow);
					return Strings.compare(name1, name2);
				};
			}
			return (i1, i2) -> {
				return Doubles.compare(i1.amount, i2.amount);
			};
		}
	}

	private class ColumnHeader implements IDataProvider {

		@Override
		public Object getDataValue(int col, int row) {
			switch (col) {
			case 0:
				return M.Flow + " / " + M.Process;
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

	private class Item {

		IndexFlow flow;
		double amount;
		CategorizedDescriptor process;

		boolean isContribution() {
			return process != null;
		}
	}

}

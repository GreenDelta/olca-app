package org.openlca.app.editors.processes;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.util.Tables;
import org.openlca.core.model.ProcessCostEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The table viewer of the cost entries in the process cost page.
 */
class ProcessCostViewer {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String COST_CATEGORY = Messages.CostCategory;
	private final String AMOUNT = Messages.Amount;
	private final String FIX = Messages.FixedCosts;
	private TableViewer viewer;
	private ProcessEditor editor;

	public ProcessCostViewer(ProcessEditor editor) {
		this.editor = editor;
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	public void render(FormToolkit toolkit, Composite parent) {
		viewer = Tables.createViewer(parent, new String[] { COST_CATEGORY,
				AMOUNT, FIX });
		Tables.bindColumnWidths(viewer, 0.5, 0.4, 0.1);
		addEditing();
	}

	private void addEditing() {
		CellEditor[] editors = new CellEditor[2];
		editors[1] = new TextCellEditor(viewer.getTable());
		viewer.setCellEditors(editors);
		viewer.setCellModifier(new CostCellModifier());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LableProvider());
	}

	private class CostCellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object obj, String property) {
			if (!(obj instanceof ProcessCostEntry))
				return false;
			if (AMOUNT.equals(property))
				return true;
			return false;
		}

		@Override
		public Object getValue(Object obj, String property) {
			if (!(obj instanceof ProcessCostEntry) || !AMOUNT.equals(property))
				return null;
			ProcessCostEntry entry = (ProcessCostEntry) obj;
			return Double.toString(entry.getAmount());
		}

		@Override
		public void modify(Object element, String property, Object val) {
			if (element instanceof Item)
				element = ((Item) element).getData();
			if (!(element instanceof ProcessCostEntry)
					|| !AMOUNT.equals(property) || val == null)
				return;
			ProcessCostEntry entry = (ProcessCostEntry) element;
			try {
				Double v = Double.parseDouble(val.toString());
				entry.setAmount(v);
				viewer.refresh();
				editor.setDirty(true);
			} catch (Exception e) {
				log.warn("Number parse error for " + val, e);
			}
		}
	}

	private class LableProvider extends ColumnLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			if (!(element instanceof ProcessCostEntry))
				return null;
			ProcessCostEntry entry = (ProcessCostEntry) element;
			switch (column) {
			case 0:
				return costCategory(entry);
			case 1:
				return Double.toString(entry.getAmount());
			case 2:
				return isFix(entry) ? Messages.Yes : Messages.No;
			}
			return null;
		}

		private boolean isFix(ProcessCostEntry entry) {
			if (entry.getCostCategory() == null)
				return false;
			return entry.getCostCategory().isFix();
		}

		private String costCategory(ProcessCostEntry entry) {
			if (entry.getCostCategory() == null)
				return null;
			return entry.getCostCategory().getName();
		}

	}

}

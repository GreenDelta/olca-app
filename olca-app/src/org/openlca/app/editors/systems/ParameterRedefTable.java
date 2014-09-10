package org.openlca.app.editors.systems;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table with parameter redefinitions. The list which is modified by this
 * table should be directly the live-list of the respective product system.
 */
class ParameterRedefTable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystemEditor editor;

	private final String PARAMETER = Messages.Parameter;
	private final String CONTEXT = Messages.Context;
	private final String AMOUNT = Messages.Amount;
	private final String UNCERTAINTY = Messages.Uncertainty;

	private TableViewer viewer;

	public ParameterRedefTable(ProductSystemEditor editor) {
		this.editor = editor;
	}

	public void create(FormToolkit toolkit, Composite composite) {
		viewer = Tables.createViewer(composite, new String[] { CONTEXT,
				PARAMETER, AMOUNT, UNCERTAINTY });
		viewer.setLabelProvider(new LabelProvider());
		ModifySupport<ParameterRedef> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(AMOUNT, new AmountModifier());
		modifySupport.bind(UNCERTAINTY,
				new UncertaintyCellEditor(viewer.getTable(), editor));
		Tables.bindColumnWidths(viewer, 0.3, 0.3, 0.2, 0.2);
		List<ParameterRedef> redefs = editor.getModel().getParameterRedefs();
		Collections.sort(redefs, new ParameterComparator());
		viewer.setInput(redefs);
	}

	public void setInput(List<ParameterRedef> redefinitions) {
		Collections.sort(redefinitions, new ParameterComparator());
		viewer.setInput(redefinitions);
	}

	public void bindActions(Section section) {
		Action addAction = Actions.onAdd(this::add);
		Action removeAction = Actions.onRemove(this::remove);
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, addAction, removeAction);
		Actions.bind(viewer, addAction, removeAction, copy);
		Tables.onDeletePressed(viewer, (e) -> remove());
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				add();
		});
	}

	private void add() {
		ProductSystem system = editor.getModel();
		List<ParameterRedef> systemRedefs = system.getParameterRedefs();
		List<ParameterRedef> redefs = ParameterRedefDialog.select(system
				.getProcesses());
		if (redefs.isEmpty())
			return;
		log.trace("add new parameter redef");
		for (ParameterRedef redef : redefs) {
			if (!contains(redef, systemRedefs)) {
				systemRedefs.add(redef.clone());
			}
		}
		viewer.setInput(systemRedefs);
		editor.setDirty(true);
	}

	private boolean contains(ParameterRedef redef, List<ParameterRedef> redefs) {
		for (ParameterRedef contained : redefs) {
			if (Strings.nullOrEqual(contained.getName(), redef.getName())
					&& Objects.equals(contained.getContextId(),
							redef.getContextId()))
				return true;
		}
		return false;
	}

	private void remove() {
		log.trace("remove parameter redef");
		ProductSystem system = editor.getModel();
		List<ParameterRedef> systemRedefs = system.getParameterRedefs();
		List<ParameterRedef> redefs = Viewers.getAllSelected(viewer);
		for (ParameterRedef redef : redefs)
			systemRedefs.remove(redef);
		viewer.setInput(systemRedefs);
		editor.setDirty(true);
	}

	private class AmountModifier extends TextCellModifier<ParameterRedef> {

		protected String getText(ParameterRedef redef) {
			return Double.toString(redef.getValue());
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			try {
				double val = Double.parseDouble(text);
				redef.setValue(val);
				editor.setDirty(true);
			} catch (Exception e) {
				Dialog.showError(viewer.getTable().getShell(), text
						+ " " + Messages.IsNotValidNumber);
			}
		}
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		private EntityCache cache = Cache.getEntityCache();

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			BaseDescriptor model = getModel(redef);
			if (model == null)
				return ImageType.FORMULA_ICON.get();
			else
				return Images.getIcon(model);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			switch (columnIndex) {
			case 0:
				BaseDescriptor model = getModel(redef);
				if (model != null)
					return Labels.getDisplayName(model);
				return "global";
			case 1:
				return redef.getName();
			case 2:
				return Double.toString(redef.getValue());
			case 3:
				return UncertaintyLabel.get(redef.getUncertainty());
			default:
				return null;
			}
		}

		private BaseDescriptor getModel(ParameterRedef redef) {
			if (redef == null || redef.getContextId() == null)
				return null;
			long modelId = redef.getContextId();
			BaseDescriptor model = cache.get(ImpactMethodDescriptor.class,
					modelId);
			if (model != null)
				return model;
			else
				return cache.get(ProcessDescriptor.class, modelId);
		}
	}

	private class ParameterComparator implements Comparator<ParameterRedef> {

		private EntityCache cache = Cache.getEntityCache();

		@Override
		public int compare(ParameterRedef o1, ParameterRedef o2) {
			if (Objects.equals(o1.getContextId(), o2.getContextId()))
				return byName(o1, o2);
			if (o1.getContextId() == null) {
				return -1; // global before process
			}
			if (o2.getContextId() == null)
				return 1; // process after global
			return compareProcesses(o1.getContextId(), o2.getContextId());
		}

		private int byName(ParameterRedef o1, ParameterRedef o2) {
			return Strings.compare(o1.getName(), o2.getName());
		}

		private int compareProcesses(Long processId1, Long processId2) {
			if (processId1 == null || processId2 == null)
				return 0;
			BaseDescriptor d1 = cache.get(ProcessDescriptor.class, processId1);
			String name1 = Labels.getDisplayName(d1);
			BaseDescriptor d2 = cache.get(ProcessDescriptor.class, processId2);
			String name2 = Labels.getDisplayName(d2);
			return Strings.compare(name1, name2);
		}

	}
}

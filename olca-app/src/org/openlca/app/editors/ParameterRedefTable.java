package org.openlca.app.editors;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.Cache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table with parameter redefinitions. The list which is modified by this
 * table should be directly the live-list of the respective model (i.e. product
 * system or project variant).
 */
public class ParameterRedefTable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private List<ParameterRedef> redefinitions;
	private ModelEditor<?> editor;

	private final String PARAMETER = Messages.Parameter;
	private final String PROCESS = Messages.Process;
	private final String AMOUNT = Messages.Amount;

	private TableViewer viewer;

	public ParameterRedefTable(ModelEditor<?> editor,
			List<ParameterRedef> redefinitions) {
		this.editor = editor;
		this.redefinitions = redefinitions;
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public void create(FormToolkit toolkit, Composite composite) {
		viewer = Tables.createViewer(composite, new String[] { PROCESS,
				PARAMETER, AMOUNT });
		viewer.setLabelProvider(new LabelProvider());
		ModifySupport<ParameterRedef> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(AMOUNT, new AmountModifier());
		Tables.bindColumnWidths(viewer, 0.4, 0.3, 0.3);
		Collections.sort(redefinitions, new ParameterComparator());
		viewer.setInput(redefinitions);
	}

	public void bindActions(Section section) {
		Action addAction = Actions.onAdd(new Runnable() {
			public void run() {
				add();
			}
		});
		Action removeAction = Actions.onRemove(new Runnable() {
			public void run() {
				remove();
			}
		});
		Actions.bind(section, addAction, removeAction);
		Actions.bind(viewer, addAction, removeAction);
	}

	private void add() {
		List<ParameterRedef> redefs = ParameterRedefDialog.select();
		if (redefs.isEmpty())
			return;
		log.trace("add new parameter redef");
		for (ParameterRedef redef : redefs) {
			if (!contains(redef)) {
				redefinitions.add(redef.clone());
			}
		}
		viewer.setInput(redefinitions);
		editor.setDirty(true);
	}

	private boolean contains(ParameterRedef redef) {
		for (ParameterRedef contained : redefinitions) {
			if (Strings.nullOrEqual(contained.getName(), redef.getName())
					&& Objects.equals(contained.getProcessId(),
							redef.getProcessId()))
				return true;
		}
		return false;
	}

	private void remove() {
		log.trace("remove parameter redef");
		List<ParameterRedef> redefs = Viewers.getAllSelected(viewer);
		for (ParameterRedef redef : redefs)
			redefinitions.remove(redef);
		viewer.setInput(redefinitions);
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
						+ " is not a valid number");
			}
		}
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		private Cache cache = Database.getCache();

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (redef.getProcessId() == null)
				return ImageType.FORMULA_ICON.get();
			else
				return ImageType.PROCESS_ICON.get();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			switch (columnIndex) {
			case 0:
				if (redef.getProcessId() != null) {
					BaseDescriptor d = cache.getProcessDescriptor(redef
							.getProcessId());
					return Labels.getDisplayName(d);
				}
				return "global";
			case 1:
				return redef.getName();
			case 2:
				return Double.toString(redef.getValue());
			default:
				return null;
			}
		}
	}

	private class ParameterComparator implements Comparator<ParameterRedef> {

		private Cache cache = Database.getCache();

		@Override
		public int compare(ParameterRedef o1, ParameterRedef o2) {
			if (Objects.equals(o1.getProcessId(), o2.getProcessId()))
				return byName(o1, o2);
			if (o1.getProcessId() == null) {
				return -1; // global before process
			}
			if (o2.getProcessId() == null)
				return 1; // process after global
			return compareProcesses(o1.getProcessId(), o2.getProcessId());
		}

		private int byName(ParameterRedef o1, ParameterRedef o2) {
			return Strings.compare(o1.getName(), o2.getName());
		}

		private int compareProcesses(Long processId1, Long processId2) {
			if (processId1 == null || processId2 == null)
				return 0;
			BaseDescriptor d1 = cache.getProcessDescriptor(processId1);
			String name1 = Labels.getDisplayName(d1);
			BaseDescriptor d2 = cache.getProcessDescriptor(processId2);
			String name2 = Labels.getDisplayName(d2);
			return Strings.compare(name1, name2);
		}

	}
}

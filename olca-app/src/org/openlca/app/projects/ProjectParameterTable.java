package org.openlca.app.projects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ProjectParameterTable {

	private final String PARAMETER = Messages.Parameter;
	private final String PROCESS = Messages.Process;

	private ProjectEditor editor;
	private EntityCache cache = Cache.getEntityCache();

	private List<ParameterRedef> redefs = new ArrayList<>();
	private ModifySupport<ParameterRedef> modifySupport;
	private Column[] columns;
	private TableViewer viewer;

	public ProjectParameterTable(ProjectEditor editor) {
		this.editor = editor;
		Project project = editor.getModel();
		initColumns(project);
		initParameterRedefs(project);
	}

	private void initColumns(Project project) {
		if (project == null) {
			columns = new Column[0];
			return;
		}
		columns = new Column[project.getVariants().size()];
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(project.getVariants().get(i));
		Arrays.sort(columns);
	}

	private void initParameterRedefs(Project project) {
		for (ProjectVariant variant : project.getVariants()) {
			for (ParameterRedef redef : variant.getParameterRedefs()) {
				if (!contains(redef))
					redefs.add(redef);
			}
		}
		Collections.sort(redefs, new Comparator<ParameterRedef>() {
			@Override
			public int compare(ParameterRedef o1, ParameterRedef o2) {
				return Strings.compare(o1.getName(), o2.getName());
			}
		});
	}

	/**
	 * true if a parameter redefinition with the given name and process ID (can
	 * be null) exists.
	 */
	private boolean contains(ParameterRedef redef) {
		for (ParameterRedef contained : redefs) {
			if (Objects.equals(redef.getName(), contained.getName())
					&& Objects.equals(redef.getContextId(),
							contained.getContextId()))
				return true;
		}
		return false;
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		viewer = Tables.createViewer(composite, getColumnTitles());
		viewer.setLabelProvider(new LabelProvider());
		Tables.bindColumnWidths(viewer, 0.15, 0.2);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		viewer.setInput(redefs);
		createModifySupport();
		bindActions(section);
	}

	private String[] getColumnTitles() {
		String[] titles = new String[columns.length + 2];
		titles[0] = PARAMETER;
		titles[1] = PROCESS;
		for (int i = 0; i < columns.length; i++)
			titles[i + 2] = columns[i].getTitle();
		return titles;
	}

	private void createModifySupport() {
		// we use unique key to map the columns / editors to project variants
		String[] keys = new String[columns.length + 2];
		keys[0] = PARAMETER;
		keys[1] = PROCESS;
		for (int i = 0; i < columns.length; i++)
			keys[i + 2] = columns[i].getKey();
		viewer.setColumnProperties(keys);
		modifySupport = new ModifySupport<>(viewer);
		for (int i = 2; i < keys.length; i++)
			modifySupport.bind(keys[i], new ValueModifier(keys[i]));
	}

	private void bindActions(Section section) {
		Action add = Actions.onAdd(new Runnable() {
			public void run() {
				onAdd();
			}
		});
		Action remove = Actions.onRemove(new Runnable() {
			public void run() {
				onRemove();
			}
		});
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove);
	}

	private void onAdd() {
		List<ParameterRedef> redefs = ParameterRedefDialog.select();
		for (ParameterRedef redef : redefs) {
			if (contains(redef))
				continue;
			this.redefs.add(redef);
			for (Column column : columns) {
				if (findVariantRedef(column.variant, redef) == null)
					column.variant.getParameterRedefs().add(redef.clone());
			}
		}
		viewer.setInput(this.redefs);
		editor.setDirty(true);
	}

	private void onRemove() {
		List<ParameterRedef> selection = Viewers.getAllSelected(viewer);
		for (ParameterRedef selected : selection) {
			this.redefs.remove(selected);
			for (Column column : columns) {
				ProjectVariant variant = column.variant;
				ParameterRedef redef = findVariantRedef(variant, selected);
				if (redef != null)
					variant.getParameterRedefs().remove(redef);
			}
		}
		viewer.setInput(this.redefs);
		editor.setDirty(true);
	}

	public void addVariant(ProjectVariant variant) {
		Column newColumn = new Column(variant);
		Table table = viewer.getTable();
		TableColumn tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(150);
		tableColumn.setText(newColumn.getTitle());
		Column[] newColumns = new Column[columns.length + 1];
		System.arraycopy(columns, 0, newColumns, 0, columns.length);
		newColumns[columns.length] = newColumn;
		columns = newColumns;
		createModifySupport();
		viewer.refresh();
	}

	public void removeVariant(ProjectVariant variant) {
		int idx = getIndex(variant);
		if (idx == -1)
			return;
		Column[] newColumns = new Column[columns.length - 1];
		System.arraycopy(columns, 0, newColumns, 0, idx);
		if ((idx + 1) < columns.length)
			System.arraycopy(columns, idx + 1, newColumns, idx,
					newColumns.length - idx);
		columns = newColumns;
		Table table = viewer.getTable();
		table.getColumn(idx + 2).dispose();
		createModifySupport();
		viewer.refresh();
	}

	public void updateVariant(ProjectVariant variant) {
		int idx = getIndex(variant);
		if (idx == -1)
			return;
		Column column = columns[idx];
		Table table = viewer.getTable();
		String title = column.getTitle() == null ? "" : column.getTitle();
		table.getColumn(idx + 2).setText(title);
		viewer.refresh();
	}

	private ParameterRedef findVariantRedef(ProjectVariant variant,
			ParameterRedef redef) {
		if (variant == null)
			return null;
		for (ParameterRedef variantRedef : variant.getParameterRedefs()) {
			if (Objects.equals(variantRedef.getName(), redef.getName())
					&& Objects.equals(variantRedef.getContextId(),
							redef.getContextId()))
				return variantRedef;
		}
		return null;
	}

	private ProjectVariant findVariant(String key) {
		for (Column column : columns) {
			if (Objects.equals(key, column.getKey()))
				return column.getVariant();
		}
		return null;
	}

	public int getIndex(ProjectVariant variant) {
		for (int i = 0; i < columns.length; i++) {
			if (Objects.equals(variant, columns[i].getVariant()))
				return i;
		}
		return -1;
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (redef.getContextId() == null)
				return ImageType.FORMULA_ICON.get();
			else
				return ImageType.PROCESS_ICON.get();
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (col == 0)
				return redef.getName();
			if (col == 1)
				return getProcessColumnText(redef);
			else
				return getVariantValue(col, redef);
		}

		private String getVariantValue(int col, ParameterRedef redef) {
			int idx = col - 2;
			if (idx < 0 || idx >= columns.length)
				return null;
			ProjectVariant variant = columns[idx].getVariant();
			ParameterRedef variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return null;
			return Double.toString(variantRedef.getValue());
		}

		private String getProcessColumnText(ParameterRedef redef) {
			if (redef.getContextId() == null)
				return "global";
			else {
				ProcessDescriptor descriptor = cache.get(
						ProcessDescriptor.class, redef.getContextId());
				return Labels.getDisplayName(descriptor);
			}
		}
	}

	private class ValueModifier extends TextCellModifier<ParameterRedef> {

		private String key;

		public ValueModifier(String key) {
			this.key = key;
		}

		protected String getText(ParameterRedef redef) {
			ProjectVariant variant = findVariant(key);
			ParameterRedef variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return "";
			return Double.toString(variantRedef.getValue());
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			if (redef == null || text == null)
				return;
			ProjectVariant variant = findVariant(key);
			if (variant == null)
				return;
			ParameterRedef variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null) {
				variantRedef = redef.clone();
				variant.getParameterRedefs().add(variantRedef);
			}
			try {
				double d = Double.parseDouble(text);
				variantRedef.setValue(d);
				editor.setDirty(true);
			} catch (Exception e) {
			}
		}
	}

	private class Column implements Comparable<Column> {

		private ProjectVariant variant;
		private String key;

		public Column(ProjectVariant variant) {
			this.variant = variant;
			key = UUID.randomUUID().toString();
		}

		public ProjectVariant getVariant() {
			return variant;
		}

		public String getKey() {
			return key;
		}

		public String getTitle() {
			if (variant == null)
				return "";
			return variant.getName();
		}

		@Override
		public int compareTo(Column other) {
			if (this.variant == null || other.variant == null)
				return 0;
			return Strings.compare(this.variant.getName(),
					other.variant.getName());
		}

	}

}

package org.openlca.app.editors.projects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ProjectParameterTable {

	private final int LABEL_COLS = 4;
	private final String PARAMETER = Messages.Parameter;
	private final String CONTEXT = Messages.Context;
	private final String NAME = Messages.ReportName;
	private final String DESCRIPTION = Messages.Description;

	private ProjectEditor editor;
	private ReportParameterSync reportSync;
	private EntityCache cache = Cache.getEntityCache();

	private List<ParameterRedef> redefs = new ArrayList<>();
	private Column[] columns;
	private TableViewer viewer;

	public ProjectParameterTable(ProjectEditor editor) {
		this.editor = editor;
		this.reportSync = new ReportParameterSync(editor);
		Project project = editor.getModel();
		initColumns(project);
		initParameterRedefs(project);
		editor.onSaved(() -> updateOnSave(editor));
	}

	private void updateOnSave(ProjectEditor editor) {
		Project newProject = editor.getModel();
		for (ProjectVariant newVar : newProject.getVariants()) {
			for (Column col : columns) {
				if (equal(col.variant, newVar)) {
					col.variant = newVar;
					break;
				}
			}
		}
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
		Collections.sort(redefs,
				(o1, o2) -> Strings.compare(o1.getName(), o2.getName()));
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
		Tables.bindColumnWidths(viewer, 0.15, 0.15, 0.15, 0.15);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		viewer.setInput(redefs);
		createModifySupport();
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private String[] getColumnTitles() {
		String[] titles = new String[LABEL_COLS + columns.length];
		titles[0] = PARAMETER;
		titles[1] = CONTEXT;
		titles[2] = NAME;
		titles[3] = DESCRIPTION;
		for (int i = 0; i < columns.length; i++)
			titles[i + LABEL_COLS] = columns[i].getTitle();
		return titles;
	}

	private void createModifySupport() {
		// we use unique key to map the columns / editors to project variants
		String[] keys = new String[LABEL_COLS + columns.length];
		keys[0] = PARAMETER;
		keys[1] = CONTEXT;
		keys[2] = NAME;
		keys[3] = DESCRIPTION;
		for (int i = 0; i < columns.length; i++)
			keys[i + LABEL_COLS] = columns[i].getKey();
		viewer.setColumnProperties(keys);
		ModifySupport<ParameterRedef> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(NAME, new NameModifier());
		modifySupport.bind(DESCRIPTION, new DescriptionModifier());
		for (int i = LABEL_COLS; i < keys.length; i++)
			modifySupport.bind(keys[i], new ValueModifier(keys[i]));
	}

	private void onAdd() {
		Set<Long> contexts = getParameterContexts();
		List<ParameterRedef> redefs = ParameterRedefDialog.select(contexts);
		for (ParameterRedef redef : redefs) {
			if (contains(redef))
				continue;
			this.redefs.add(redef);
			reportSync.parameterAdded(redef);
			for (Column column : columns) {
				if (findVariantRedef(column.variant, redef) == null)
					column.variant.getParameterRedefs().add(redef.clone());
			}
		}
		viewer.setInput(this.redefs);
		editor.setDirty(true);
	}

	private Set<Long> getParameterContexts() {
		Project project = editor.getModel();
		HashSet<Long> contexts = new HashSet<>();
		if (project.getImpactMethodId() != null)
			contexts.add(project.getImpactMethodId());
		for (ProjectVariant variant : project.getVariants()) {
			if (variant.getProductSystem() == null)
				continue;
			contexts.addAll(variant.getProductSystem().getProcesses());
		}
		return contexts;
	}

	private void onRemove() {
		List<ParameterRedef> selection = Viewers.getAllSelected(viewer);
		for (ParameterRedef selected : selection) {
			this.redefs.remove(selected);
			reportSync.parameterRemoved(selected);
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
		table.getColumn(idx + LABEL_COLS).dispose();
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
		table.getColumn(idx + LABEL_COLS).setText(title);
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

	private int getIndex(ProjectVariant variant) {
		for (int i = 0; i < columns.length; i++) {
			if (equal(variant, columns[i].getVariant()))
				return i;
		}
		return -1;
	}

	private boolean equal(ProjectVariant var1, ProjectVariant var2) {
		// saving the project changes the ID of an unsaved variant and thus the
		// equal function of the ProjectVariant class will fail -> thus, we
		// check the name in this case.
		if (var1 == var2)
			return true;
		if (var1 == null || var2 == null)
			return false;
		if (var1.getId() != 0 && var2.getId() != 0)
			return var1.getId() == var2.getId();
		else
			return Objects.equals(var1.getName(), var2.getName());
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
			BaseDescriptor model = getModel(redef);
			if (model == null)
				return ImageType.FORMULA_ICON.get();
			else
				return Images.getIcon(model);
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (col == 0)
				return redef.getName();
			if (col == 1)
				return getModelColumnText(redef);
			if (col == 2)
				return reportSync.getName(redef);
			if (col == 3)
				return reportSync.getDescription(redef);
			else
				return getVariantValue(col, redef);
		}

		private String getVariantValue(int col, ParameterRedef redef) {
			int idx = col - LABEL_COLS;
			if (idx < 0 || idx >= columns.length)
				return null;
			ProjectVariant variant = columns[idx].getVariant();
			ParameterRedef variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return null;
			return Double.toString(variantRedef.getValue());
		}

		private String getModelColumnText(ParameterRedef redef) {
			BaseDescriptor model = getModel(redef);
			if (model == null)
				return "global";
			else
				return Labels.getDisplayName(model);
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
				reportSync.valueChanged(redef, variant, d);
				editor.setDirty(true);
			} catch (Exception e) {
				org.openlca.app.util.Error.showBox(Messages.InvalidNumber, text
						+ " " + Messages.IsNotValidNumber);
			}
		}
	}

	private class NameModifier extends TextCellModifier<ParameterRedef> {
		@Override
		protected String getText(ParameterRedef redef) {
			return reportSync.getName(redef);
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			String oldName = reportSync.getName(redef);
			if (Objects.equals(oldName, text))
				return;
			reportSync.setName(text, redef);
			editor.setDirty(true);
		}
	}

	private class DescriptionModifier extends TextCellModifier<ParameterRedef> {
		@Override
		protected String getText(ParameterRedef redef) {
			return reportSync.getDescription(redef);
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			String oldText = reportSync.getDescription(redef);
			if (Objects.equals(oldText, text))
				return;
			reportSync.setDescription(text, redef);
			editor.setDirty(true);
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

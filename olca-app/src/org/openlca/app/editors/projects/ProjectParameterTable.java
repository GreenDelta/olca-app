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
import org.openlca.app.M;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.Daos;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ProjectParameterTable {

	private final int LABEL_COLS = 4;
	private final String PARAMETER = M.Parameter;
	private final String CONTEXT = M.Context;
	private final String NAME = M.ReportName;
	private final String DESCRIPTION = M.Description;

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
		for (ProjectVariant newVar : newProject.variants) {
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
		columns = new Column[project.variants.size()];
		for (int i = 0; i < columns.length; i++)
			columns[i] = new Column(project.variants.get(i));
		Arrays.sort(columns);
	}

	private void initParameterRedefs(Project project) {
		for (ProjectVariant variant : project.variants) {
			for (ParameterRedef redef : variant.parameterRedefs) {
				if (!contains(redef))
					redefs.add(redef);
			}
		}
		Collections.sort(redefs,
				(o1, o2) -> Strings.compare(o1.name, o2.name));
	}

	/**
	 * true if a parameter redefinition with the given name and process ID (can
	 * be null) exists.
	 */
	private boolean contains(ParameterRedef redef) {
		for (ParameterRedef contained : redefs) {
			if (Objects.equals(redef.name, contained.name)
					&& Objects.equals(redef.contextId,
							contained.contextId))
				return true;
		}
		return false;
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit, 1);
		viewer = Tables.createViewer(composite, getColumnTitles());
		viewer.setLabelProvider(new LabelProvider());
		Tables.bindColumnWidths(viewer, 0.15, 0.15, 0.15, 0.15);
		viewer.setInput(redefs);
		createModifySupport();
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action copy = TableClipboard.onCopy(viewer);
		CommentAction.bindTo(section, "parameters", editor.getComments(), add,
				remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private String[] getColumnTitles() {
		boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
		int colSize = showComments ? 2 * columns.length : columns.length;
		String[] titles = new String[LABEL_COLS + colSize];
		titles[0] = PARAMETER;
		titles[1] = CONTEXT;
		titles[2] = NAME;
		titles[3] = DESCRIPTION;
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			titles[LABEL_COLS + index] = columns[i].getTitle();
			if (showComments) {
				titles[LABEL_COLS + index + 1] = "";
			}
		}
		return titles;
	}

	private void createModifySupport() {
		// we use unique key to map the columns / editors to project variants
		boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
		int colSize = showComments ? 2 * columns.length : columns.length;
		String[] keys = new String[LABEL_COLS + colSize];
		keys[0] = PARAMETER;
		keys[1] = CONTEXT;
		keys[2] = NAME;
		keys[3] = DESCRIPTION;
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			keys[LABEL_COLS + index] = columns[i].getKey();
			if (showComments) {
				keys[LABEL_COLS + index + 1] = columns[i].getKey() + "_COMMENT";
			}
		}
		viewer.setColumnProperties(keys);
		ModifySupport<ParameterRedef> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(NAME, new NameModifier());
		modifySupport.bind(DESCRIPTION, new DescriptionModifier());
		for (int i = LABEL_COLS; i < keys.length; i++) {
			if (!showComments || i % 2 == 0) {
				modifySupport.bind(keys[i], new ValueModifier(keys[i]));
			} else {
				ProjectVariant variant = columns[(i - LABEL_COLS - 1) / 2].variant;
				modifySupport.bind(
						keys[i],
						new CommentDialogModifier<>(editor.getComments(),
								redef -> CommentPaths.get(variant, redef,
										getContext(redef))));
			}
		}
	}

	private CategorizedDescriptor getContext(ParameterRedef p) {
		if (p.contextId == null)
			return null;
		return Daos.categorized(Database.get(), p.contextType)
				.getDescriptor(p.contextId);
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
					column.variant.parameterRedefs.add(redef.clone());
			}
		}
		viewer.setInput(this.redefs);
		editor.setDirty(true);
	}

	private Set<Long> getParameterContexts() {
		Project project = editor.getModel();
		HashSet<Long> contexts = new HashSet<>();
		if (project.impactMethodId != null)
			contexts.add(project.impactMethodId);
		for (ProjectVariant variant : project.variants) {
			if (variant.productSystem == null)
				continue;
			contexts.addAll(variant.productSystem.processes);
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
					variant.parameterRedefs.remove(redef);
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
		for (ParameterRedef variantRedef : variant.parameterRedefs) {
			if (Objects.equals(variantRedef.name, redef.name)
					&& Objects.equals(variantRedef.contextId,
							redef.contextId))
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
		if (var1.id != 0 && var2.id != 0)
			return var1.id == var2.id;
		else
			return Objects.equals(var1.name, var2.name);
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			BaseDescriptor model = getModel(redef);
			if (column == 0) {
				if (model == null)
					return Images.get(ModelType.PARAMETER);
				return Images.get(model);
			}
			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			if (column > LABEL_COLS && showComments && column % 2 == 1) {
				ProjectVariant variant = columns[(column - LABEL_COLS - 1) / 2].variant;
				String path = CommentPaths.get(variant, redef, getContext(redef));
				return Images.get(editor.getComments(), path);
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (col == 0)
				return redef.name;
			if (col == 1)
				return getModelColumnText(redef);
			if (col == 2)
				return reportSync.getName(redef);
			if (col == 3)
				return reportSync.getDescription(redef);
			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			if (!showComments || col % 2 == 0)
				return getVariantValue(col, redef);
			return null;
		}

		private String getVariantValue(int col, ParameterRedef redef) {
			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			int idx = (col - LABEL_COLS);
			if (showComments)
				idx /= 2;
			if (idx < 0 || idx >= columns.length)
				return null;
			ProjectVariant variant = columns[idx].getVariant();
			ParameterRedef variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return null;
			return Double.toString(variantRedef.value);
		}

		private String getModelColumnText(ParameterRedef redef) {
			BaseDescriptor model = getModel(redef);
			if (model == null)
				return "global";
			else
				return Labels.getDisplayName(model);
		}

		private BaseDescriptor getModel(ParameterRedef redef) {
			if (redef == null || redef.contextId == null)
				return null;
			long modelId = redef.contextId;
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
			return Double.toString(variantRedef.value);
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
				variant.parameterRedefs.add(variantRedef);
			}
			try {
				double d = Double.parseDouble(text);
				variantRedef.value = d;
				reportSync.valueChanged(redef, variant, d);
				editor.setDirty(true);
			} catch (Exception e) {
				org.openlca.app.util.Error.showBox(M.InvalidNumber, text + " "
						+ M.IsNotValidNumber);
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
			return variant.name;
		}

		@Override
		public int compareTo(Column other) {
			if (this.variant == null || other.variant == null)
				return 0;
			return Strings.compare(
					this.variant.name, other.variant.name);
		}
	}
}

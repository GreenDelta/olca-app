package org.openlca.app.editors.projects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

class ProjectParameterTable {

	/**
	 * The number of label columns before the value columns start.
	 */
	private final int LABEL_OFFSET = 3;

	private final ProjectEditor editor;
	private final SortedSet<ParameterRedef> redefs;
	private Column[] columns;
	private TableViewer viewer;

	ProjectParameterTable(ProjectEditor editor) {
		this.editor = editor;
		Project project = editor.getModel();
		columns = Column.createAllOf(project);
		editor.onSaved(
			() -> Column.updateAll(columns, editor.getModel()));

		// collect the parameter redefinitions from
		// the project variants. A redefinition is
		// identified by name and context
		redefs = new TreeSet<>((redef1, redef2) -> {
			int c = Strings.compare(redef1.name, redef2.name);
			if (c != 0)
				return c;
			long c1 = redef1.contextId != null
				? redef1.contextId
				: 0;
			long c2 = redef2.contextId != null
				? redef2.contextId
				: 0;
			return Long.compare(c1, c2);
		});
		project.variants.stream()
			.flatMap(v -> v.parameterRedefs.stream())
			.forEach(redefs::add);
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
		Action copy = TableClipboard.onCopySelected(viewer);
		CommentAction.bindTo(
			section, "parameters", editor.getComments(), add, remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private String[] getColumnTitles() {
		boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
		int columnCount = showComments
			? 2 * columns.length
			: columns.length;
		String[] titles = new String[LABEL_OFFSET + columnCount];
		titles[0] = M.Parameter;
		titles[1] = M.Context;
		titles[2] = M.Description;
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			titles[LABEL_OFFSET + index] = columns[i].variant.name;
			if (showComments) {
				titles[LABEL_OFFSET + index + 1] = "";
			}
		}
		return titles;
	}

	private void createModifySupport() {
		// we use unique key to map the columns / editors to project variants
		boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
		int columnCount = showComments
			? 2 * columns.length
			: columns.length;
		var keys = new String[LABEL_OFFSET + columnCount];
		keys[0] = M.Parameter;
		keys[1] = M.Context;
		keys[2] = M.Description;
		for (int i = 0; i < columns.length; i++) {
			int index = showComments ? 2 * i : i;
			keys[LABEL_OFFSET + index] = columns[i].key;
			if (showComments) {
				keys[LABEL_OFFSET + index + 1] = columns[i].key + "_COMMENT";
			}
		}
		viewer.setColumnProperties(keys);
		var modifiers = new ModifySupport<ParameterRedef>(viewer)
			.bind(M.Description, new DescriptionModifier());
		for (int i = LABEL_OFFSET; i < keys.length; i++) {
			if (!showComments || i % 2 == 0) {
				modifiers.bind(keys[i], new ValueModifier(keys[i]));
			} else {
				var variant = columns[(i - LABEL_OFFSET - 1) / 2].variant;
				modifiers.bind(keys[i], new CommentDialogModifier<>(
					editor.getComments(),
					redef -> CommentPaths.get(variant, redef, getContext(redef))));
			}
		}
	}

	private RootDescriptor getContext(ParameterRedef p) {
		if (p.contextId == null)
			return null;
		return Daos.root(Database.get(), p.contextType)
			.getDescriptor(p.contextId);
	}

	private void onAdd() {

		// collect the possible local parameter contexts
		var contexts = new HashSet<Long>();
		var project = editor.getModel();
		if (project.impactMethod != null) {
			contexts.add(project.impactMethod.id);
		}
		project.variants.stream()
			.filter(v -> v.productSystem != null)
			.forEach(v -> contexts.addAll(v.productSystem.processes));

		var newRedefs = ParameterRedefDialog.select(contexts);
		for (var redef : newRedefs) {
			if (!this.redefs.add(redef))
				continue;
			for (Column column : columns) {
				if (findVariantRedef(column.variant, redef) == null)
					column.variant.parameterRedefs.add(redef.copy());
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
					variant.parameterRedefs.remove(redef);
			}
		}
		viewer.setInput(this.redefs);
		editor.setDirty(true);
	}

	public void addVariant(ProjectVariant variant) {
		columns = Column.add(columns, variant);
		Table table = viewer.getTable();
		TableColumn tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(150);
		tableColumn.setText(variant.name);
		createModifySupport();
		viewer.refresh();
	}

	public void removeVariant(ProjectVariant variant) {

		int idx = Column.indexOf(columns, variant);
		if (idx == -1)
			return;
		Column[] newColumns = new Column[columns.length - 1];
		System.arraycopy(columns, 0, newColumns, 0, idx);
		if ((idx + 1) < columns.length)
			System.arraycopy(columns, idx + 1, newColumns, idx,
				newColumns.length - idx);
		columns = newColumns;

		Table table = viewer.getTable();
		table.getColumn(idx + LABEL_OFFSET).dispose();
		createModifySupport();
		viewer.refresh();
	}

	public void updateVariant(ProjectVariant variant) {
		int idx = Column.indexOf(columns, variant);
		if (idx == -1)
			return;
		Column column = columns[idx];
		Table table = viewer.getTable();
		table.getColumn(idx + LABEL_OFFSET).setText(column.title());
		viewer.refresh();
	}

	private ParameterRedef findVariantRedef(
		ProjectVariant variant, ParameterRedef redef) {
		if (variant == null)
			return null;
		for (ParameterRedef variantRedef : variant.parameterRedefs) {
			if (Objects.equals(variantRedef.name, redef.name)
				&& Objects.equals(variantRedef.contextId, redef.contextId))
				return variantRedef;
		}
		return null;
	}

	private static boolean equal(ProjectVariant var1, ProjectVariant var2) {
		// saving the project changes the ID of an unsaved variant and thus the
		// equal function of the ProjectVariant class will fail -> thus, we
		// check the name in this case.
		if (var1 == var2)
			return true;
		if (var1 == null || var2 == null)
			return false;
		return var1.id != 0 && var2.id != 0
			? var1.id == var2.id
			: Objects.equals(var1.name, var2.name);
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ParameterRedef))
				return null;
			var redef = (ParameterRedef) obj;

			if (col == 0) {
				var model = getModel(redef);
				return model == null
					? Images.get(ModelType.PARAMETER)
					: Images.get(model);
			}

			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			if (col > LABEL_OFFSET && showComments && col % 2 == 1) {
				var variant = columns[(col - LABEL_OFFSET - 1) / 2].variant;
				String path = CommentPaths.get(variant, redef, getContext(redef));
				return Images.get(editor.getComments(), path);
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ParameterRedef))
				return null;
			var redef = (ParameterRedef) obj;
			if (col == 0)
				return redef.name;
			if (col == 1)
				return getModelColumnText(redef);
			if (col == 2)
				return redef.description;
			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			if (!showComments || col % 2 == 0)
				return getVariantValue(col, redef);
			return null;
		}

		private String getVariantValue(int col, ParameterRedef redef) {
			boolean showComments = editor.hasAnyComment("variants.parameterRedefs");
			int idx = (col - LABEL_OFFSET);
			if (showComments)
				idx /= 2;
			if (idx < 0 || idx >= columns.length)
				return null;
			var variant = columns[idx].variant;
			var variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return null;
			return Double.toString(variantRedef.value);
		}

		private String getModelColumnText(ParameterRedef redef) {
			var model = getModel(redef);
			if (model == null)
				return "global";
			else
				return Labels.name(model);
		}

		private Descriptor getModel(ParameterRedef redef) {
			if (redef == null || redef.contextId == null)
				return null;
			long modelId = redef.contextId;
			var db = Database.get();
			var model = db.getDescriptor(Process.class, modelId);
			return model != null
				? model
				: db.getDescriptor(ImpactCategory.class, modelId);
		}
	}

	private class ValueModifier extends TextCellModifier<ParameterRedef> {

		private final String key;

		public ValueModifier(String key) {
			this.key = key;
		}

		@Override
		protected String getText(ParameterRedef redef) {
			var variant = Column.variantOf(columns, key);
			var variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null)
				return "";
			return Double.toString(variantRedef.value);
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			if (redef == null || text == null)
				return;
			ProjectVariant variant = Column.variantOf(columns, key);
			if (variant == null)
				return;
			var variantRedef = findVariantRedef(variant, redef);
			if (variantRedef == null) {
				variantRedef = redef.copy();
				variant.parameterRedefs.add(variantRedef);
			}
			try {
				variantRedef.value = Double.parseDouble(text);
				editor.setDirty(true);
			} catch (Exception e) {
				MsgBox.error(M.InvalidNumber, text + " " + M.IsNotValidNumber);
			}
		}
	}

	private class DescriptionModifier extends TextCellModifier<ParameterRedef> {
		@Override
		protected String getText(ParameterRedef redef) {
			return redef == null ? null : redef.description;
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			if (redef == null)
				return;
			if (Objects.equals(redef.description, text))
				return;
			redef.description = text;
			editor.setDirty(true);
		}
	}


	/**
	 * Column maps a variant to a column of the parameter table.
	 */
	private static class Column implements Comparable<Column> {

		final int index;
		final ProjectVariant variant;
		final String key;

		Column(int index, ProjectVariant variant) {
			this(index, variant, UUID.randomUUID().toString());
		}

		Column(int index, ProjectVariant variant, String key) {
			this.index = index;
			this.variant = variant;
			this.key = key;
		}

		String title() {
			return variant == null || variant.name == null
				? ""
				: variant.name;
		}

		static Column[] createAllOf(Project project) {
			if (project == null)
				return new Column[0];
			var variants = new ArrayList<>(project.variants);
			variants.sort((v1, v2) -> Strings.compare(v1.name, v2.name));
			var columns = new Column[variants.size()];
			for (int i = 0; i < variants.size(); i++) {
				columns[i] = new Column(i, variants.get(i));
			}
			return columns;
		}

		/**
		 * Updates the column array with the matching fresh instances of the
		 * given project. We need to do this when the project is saved in
		 * the database because otherwise the project variants of the
		 * columns are out of sync with the JPA store.
		 */
		static void updateAll(Column[] columns, Project project) {
			for (int i = 0; i < columns.length; i++) {
				var oldCol = columns[i];
				for (var freshVar : project.variants) {
					if (!equal(oldCol.variant, freshVar))
						continue;
					columns[i] = new Column(i, freshVar, oldCol.key);
					break;
				}
			}
		}

		static Column[] add(Column[] columns, ProjectVariant variant) {
			var newColumn = new Column(columns.length, variant);
			var newColumns = new Column[columns.length + 1];
			System.arraycopy(columns, 0, newColumns, 0, columns.length);
			newColumns[columns.length] = newColumn;
			return newColumns;
		}

		static ProjectVariant variantOf(Column[] columns, String key) {
			for (Column column : columns) {
				if (Objects.equals(key, column.key))
					return column.variant;
			}
			return null;
		}

		static int indexOf(Column[] columns, ProjectVariant variant) {
			for (int i = 0; i < columns.length; i++) {
				if (equal(variant, columns[i].variant))
					return i;
			}
			return -1;
		}

		@Override
		public int compareTo(Column other) {
			return this.variant != null && other.variant != null
				? Strings.compare(this.variant.name, other.variant.name)
				: 0;
		}
	}
}

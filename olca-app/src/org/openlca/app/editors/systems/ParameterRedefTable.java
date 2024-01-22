package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ParameterRedefDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.field.DoubleModifier;
import org.openlca.app.viewers.tables.modify.field.StringModifier;
import org.openlca.core.database.Daos;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table with parameter redefinitions. The list which is modified by this
 * table should be directly the live-list of the respective product system.
 */
class ParameterRedefTable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ProductSystemEditor editor;
	private TableViewer table;
	private final Supplier<List<ParameterRedef>> supplier;

	ParameterRedefTable(
			ProductSystemEditor editor, Supplier<List<ParameterRedef>> supplier
	) {
		this.editor = editor;
		this.supplier = supplier;
	}

	public void update() {
		if (table == null)
			return;
		List<ParameterRedef> redefs = supplier.get();
		table.setInput(redefs);
	}

	public void create(Composite comp) {
		// configure the table
		table = Tables.createViewer(comp,
				/*0 */ M.Context,
				/* 1 */ M.Parameter,
				/* 2 */ M.Amount,
				/* 3 */ M.Uncertainty,
				/* 4 */ M.Description,
				/* 5 */ "" // comment
		);
		table.setLabelProvider(new LabelProvider());
		Tables.bindColumnWidths(table, 0.25, 0.25, 0.15, 0.2, 0.15);

		// bind modifiers
		new ModifySupport<ParameterRedef>(table)
				.bind(M.Amount, new DoubleModifier<>(editor, "value"))
				.bind(M.Description, new StringModifier<>(editor, "description"))
				.bind(M.Uncertainty, new UncertaintyCellEditor(
						table.getTable(), editor))
				.bind("", new CommentDialogModifier<>(
						editor.getComments(),
						p -> CommentPaths.get(p, getContext(p))));

		// set the input
		var redefs = supplier.get();
		redefs.sort(new ParameterComparator());
		table.setInput(redefs);
	}

	private RootDescriptor getContext(ParameterRedef p) {
		if (p.contextId == null)
			return null;
		return Daos.root(Database.get(), p.contextType)
				.getDescriptor(p.contextId);
	}

	public void bindActions(Section section) {
		var add = Actions.onAdd(this::add);
		var remove = Actions.onRemove(this::remove);
		var copy = TableClipboard.onCopySelected(table);
		var paste = TableClipboard.onPaste(table, this::onPaste);
		var usage = Actions.create(M.Usage, Icon.LINK.descriptor(), () -> {
			ParameterRedef redef = Viewers.getFirstSelected(table);
			if (redef != null) {
				ParameterUsagePage.show(redef.name);
			}
		});

		var toggleProtection = Actions.create(
				"Toggle protection", Icon.LOCK.descriptor(), () -> {
					ParameterRedef redef = Viewers.getFirstSelected(table);
					if (redef == null)
						return;
					redef.isProtected = !redef.isProtected;
					table.refresh();
					editor.setDirty();
				});

		CommentAction.bindTo(section, "parameterRedefs",
				editor.getComments(), add, remove);
		Actions.bind(table, add, remove, copy, paste, usage, toggleProtection);
		Tables.onDeletePressed(table, _e -> remove());

		// open the original parameter in double click
		Tables.onDoubleClick(table, e -> {
			ParameterRedef redef = Viewers.getFirstSelected(table);
			if (redef == null)
				return;
			var db = Database.get();
			if (redef.contextId != null && redef.contextType != null) {
				// process or LCIA parameter
				var model = redef.contextType == ModelType.PROCESS
						? db.get(Process.class, redef.contextId)
						: db.get(ImpactCategory.class, redef.contextId);
				if (model != null) {
					App.open(model);
				}
			} else {
				// global parameter
				new ParameterDao(db)
						.getGlobalDescriptors()
						.stream()
						.filter(g -> Strings.nullOrEqual(g.name, redef.name))
						.findAny()
						.ifPresent(App::open);
			}
		});
	}

	private void add() {
		List<ParameterRedef> existing = supplier.get();
		List<ParameterRedef> redefs = ParameterRedefDialog.select(
				editor.getModel().processes);
		if (redefs.isEmpty())
			return;
		log.trace("add new parameter redef");
		for (ParameterRedef redef : redefs) {
			if (!contains(redef, existing)) {
				existing.add(redef.copy());
			}
		}
		table.setInput(existing);
		editor.setDirty(true);
	}

	private void onPaste(String text) {
		List<ParameterRedef> newList = new ArrayList<>();
		App.runWithProgress("Paste parameters ...",
				() -> newList.addAll(ParameterClipboard.read(text)));
		if (newList.isEmpty())
			return;
		List<ParameterRedef> redefs = supplier.get();
		boolean added = false;
		for (ParameterRedef redef : newList) {
			if (!contains(redef, redefs)) {
				redefs.add(redef.copy());
				added = true;
			}
		}
		if (added) {
			table.setInput(redefs);
			editor.setDirty(true);
		}
	}

	private boolean contains(ParameterRedef redef, List<ParameterRedef> redefs) {
		for (ParameterRedef contained : redefs) {
			if (Strings.nullOrEqual(contained.name, redef.name)
					&& Objects.equals(contained.contextId, redef.contextId))
				return true;
		}
		return false;
	}

	private void remove() {
		log.trace("remove parameter redef");
		List<ParameterRedef> redefs = supplier.get();
		List<ParameterRedef> selected = Viewers.getAllSelected(table);
		redefs.removeAll(selected);
		table.setInput(redefs);
		editor.setDirty(true);
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		private final EntityCache cache = Cache.getEntityCache();

		@Override
		public Image getColumnImage(Object obj, int column) {
			if (!(obj instanceof ParameterRedef redef))
				return null;
			var model = getModel(redef);
			return switch (column) {
				case 0 -> model == null
						? Images.get(ModelType.PARAMETER)
						: Images.get(model);
				case 1 -> redef.isProtected ? Icon.LOCK.get() : null;
				case 5 -> Images.get(
						editor.getComments(),
						CommentPaths.get(redef, getContext(redef)));
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ParameterRedef redef))
				return null;
			return switch (col) {
				case 0 -> {
					var model = getModel(redef);
					yield model != null ? Labels.name(model) : "global";
				}
				case 1 -> redef.name;
				case 2 -> Double.toString(redef.value);
				case 3 -> Uncertainty.string(redef.uncertainty);
				case 4 -> redef.description;
				default -> null;
			};
		}

		private Descriptor getModel(ParameterRedef redef) {
			if (redef == null || redef.contextId == null)
				return null;
			long modelId = redef.contextId;
			var model = cache.get(ImpactDescriptor.class, modelId);
			return model != null
					? model
					: cache.get(ProcessDescriptor.class, modelId);
		}
	}

	private static class ParameterComparator implements Comparator<ParameterRedef> {

		private EntityCache cache = Cache.getEntityCache();

		@Override
		public int compare(ParameterRedef o1, ParameterRedef o2) {
			if (Objects.equals(o1.contextId, o2.contextId))
				return byName(o1, o2);
			if (o1.contextId == null) {
				return -1; // global before process
			}
			if (o2.contextId == null)
				return 1; // process after global
			return compareProcesses(o1.contextId, o2.contextId);
		}

		private int byName(ParameterRedef o1, ParameterRedef o2) {
			return Strings.compare(o1.name, o2.name);
		}

		private int compareProcesses(Long processId1, Long processId2) {
			if (processId1 == null || processId2 == null)
				return 0;
			var d1 = cache.get(ProcessDescriptor.class, processId1);
			String name1 = Labels.name(d1);
			var d2 = cache.get(ProcessDescriptor.class, processId2);
			String name2 = Labels.name(d2);
			return Strings.compare(name1, name2);
		}

	}
}

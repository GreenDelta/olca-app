package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.splitbutton.SplitButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

/// Dialog for selecting the analysis group of the processes and for editing
/// the groups of the product system: adding, renaming, re-coloring, deleting,
/// and adding groups from another product system or a [GroupTemplate]. It
/// works on a copy of the groups; `selected()` and `groupsChanged()` hold the
/// result that is applied by the [SetProcessGroupCommand].
class GroupDialog extends FormDialog {

	private final GraphEditor editor;
	private final List<AnalysisGroup> groups;
	private AnalysisGroup selected;
	private boolean groupsChanged;
	private Button noneRadio;

	GroupDialog(
		GraphEditor editor,
		List<AnalysisGroup> groups,
		AnalysisGroup current) {
		super(UI.shell());
		this.editor = editor;
		this.groups = groups;
		this.selected = current;
	}

	/// The selected group or `null` if "None" is selected.
	AnalysisGroup selected() {
		return selected;
	}

	/// Returns `true` if groups were added, edited, or deleted.
	boolean groupsChanged() {
		return groupsChanged;
	}

	void select(AnalysisGroup group) {
		selected = group;
	}

	void markChanged() {
		groupsChanged = true;
	}

	void delete(AnalysisGroup group) {
		groups.remove(group);
		groupsChanged = true;
		if (selected == group) {
			selected = null;
			if (noneRadio != null && !noneRadio.isDisposed()) {
				noneRadio.setSelection(true);
			}
		}
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 500, 500);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Select an analysis group");
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);

		var groupComp = tk.createComposite(body);
		UI.stretchX(groupComp);
		UI.gridLayout(groupComp, 2, 10, 0);

		noneRadio = new GroupPanel(this, null).render(groupComp, tk);
		for (var group : groups) {
			new GroupPanel(this, group).render(groupComp, tk);
		}

		var addBtn = new SplitButton(body, SWT.NONE);
		addBtn.setText("Add analysis group");
		addBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		Consumer<GroupSync> onSync = sync -> {
			for (var g : sync.newGroups()) {
				new GroupPanel(this, g).render(groupComp, tk);
			}
			body.layout();
			form.reflow(true);
			groupsChanged = true;
		};

		// sync groups from other system
		var menu = addBtn.getMenu();
		var systemItem = new MenuItem(menu, SWT.NONE);
		systemItem.setText("Copy groups from product system");
		Controls.onSelect(systemItem, _ -> {
			var d = ModelSelector.select(ModelType.PRODUCT_SYSTEM);
			if (d == null)
				return;
			var system = Database.get().get(ProductSystem.class, d.id);
			if (system != null) {
				onSync.accept(newSync().sync(system));
			}
		});

		// add groups from templates
		for (var t : GroupTemplate.values()) {
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(t.label);
			Controls.onSelect(item, _ -> onSync.accept(newSync().sync(t)));
		}

		// add a new group
		Controls.onSelect(addBtn, _ -> {
			var group = new AnalysisGroup();
			group.name = "New analysis group";
			groups.add(group);
			new GroupPanel(this, group).render(groupComp, tk);
			body.layout();
			form.reflow(true);
			groupsChanged = true;
		});
	}

	private GroupSync newSync() {
		return new GroupSync(editor.getProductSystem(), groups);
	}

	Color colorOf(AnalysisGroup group) {
		var c = group != null ? group.color : null;
		if (Strings.isNotBlank(c))
			return Colors.fromHex(c);
		return editor.getTheme() != null && editor.getTheme().isDark()
			? Colors.fromHex("#FFFDE7")
			: Colors.fromHex("#212121");
	}
}

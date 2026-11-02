package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.nebula.widgets.splitbutton.SplitButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
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

class GroupDialog extends FormDialog {

	final GraphEditor editor;
	final List<AnalysisGroup> groups;
	AnalysisGroup selected;
	boolean groupsChanged;

	GroupDialog(
		GraphEditor editor,
		List<AnalysisGroup> groups,
		AnalysisGroup current) {
		super(UI.shell());
		this.editor = editor;
		this.groups = groups;
		this.selected = current;
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

		new GroupPanel(this, null).render(groupComp, tk);
		for (var group : groups) {
			new GroupPanel(this, group).render(groupComp, tk);
		}

		var addBtn = new SplitButton(body, SWT.NONE);
		addBtn.setText("Add analysis group");
		addBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		Consumer<GroupSync> onSync = sync -> {
			for (var g : sync.newGroups) {
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
				onSync.accept(GroupSync.sync(this, system));
			}
		});

		// add groups from templates
		for (var t : GroupTemplate.values()) {
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(t.label);
			Controls.onSelect(item,
				_ -> onSync.accept(GroupSync.syncTemplate(this, t)));
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

	Color colorOf(AnalysisGroup group) {
		var c = group != null ? group.color : null;
		if (Strings.isNotBlank(c))
			return Colors.fromHex(c);
		return editor.getTheme() != null && editor.getTheme().isDark()
			? Colors.fromHex("#FFFDE7")
			: Colors.fromHex("#212121");
	}
}

package org.openlca.app.editors.graphical.model.commands;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.splitbutton.SplitButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

public class SetProcessGroupCommand extends Command {

	private final List<NodeEditPart> nodes;
	private final List<Long> processes;

	public SetProcessGroupCommand(NodeEditPart node) {
		this(List.of(node));
	}

	public SetProcessGroupCommand(List<NodeEditPart> nodes) {
		this.nodes = nodes;
		this.processes = nodes.stream()
				.map(n -> n.getModel() != null ? n.getModel().descriptor : null)
				.filter(Objects::nonNull)
				.map(d -> d.id)
				.toList();
	}

	@Override
	public boolean canRedo() {
		return false;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public boolean canExecute() {
		return !nodes.isEmpty() && nodes.size() == processes.size();
	}

	@Override
	public void execute() {
		var editor = getEditor();
		if (editor == null)
			return;

		// create a copy of the current groups
		var origin = editor.getProductSystem().analysisGroups;
		var copy = new ArrayList<AnalysisGroup>(origin.size());
		for (var g : origin) {
			var c = g.copy();
			c.id = g.id;
			copy.add(c);
		}
		copy.sort((g1, g2) -> Strings.compare(g1.name, g2.name));

		// edit the copy and check if something changed
		var dialog = new Dialog(editor, copy, getCurrent(copy));
		if (dialog.open() != Window.OK)
			return;
		boolean isCurrent = isCurrent(dialog.selected, copy);
		if (isCurrent && !dialog.groupsChanged)
			return;

		// sync changes
		origin.clear();
		origin.addAll(copy);

		if (!isCurrent) {
			applySelection(dialog.selected, copy);
		}
		if (dialog.groupsChanged) {
			applyGroupChange(origin);
		}
		editor.setDirty();
	}

	private GraphEditor getEditor() {
		if (nodes.isEmpty())
			return null;
		return nodes.getFirst().getParent() instanceof GraphEditPart gep
				? gep.getModel().editor
				: null;
	}

	private AnalysisGroup getCurrent(List<AnalysisGroup> groups) {
		return groups.stream()
				.filter(g -> g.processes.containsAll(processes))
				.findAny()
				.orElse(null);
	}

	private boolean isCurrent(AnalysisGroup g, List<AnalysisGroup> groups) {
		if (g != null)
			return g.processes.containsAll(processes);
		// the group is null -> no process can be in another group
		for (var gi : groups) {
			for (var pid : processes) {
				if (gi.processes.contains(pid))
					return false;
			}
		}
		return true;
	}

	/// Apply a newly selected group (which could be null) for the processes.
	private void applySelection(AnalysisGroup selected, List<AnalysisGroup> gs) {
		for (var g : gs) {
			processes.forEach(g.processes::remove);
		}
		if (selected != null) {
			selected.processes.addAll(processes);
		}
		for (var node : nodes) {
			node.propertyChange(new PropertyChangeEvent(
					this, Node.GROUP_PROP, null, selected));
		}
	}

	 /// Handle changes, like name or color, of groups. This has an effect not
	 /// only on the currently selected processes but all processes of the
	 /// respective groups, their figures may need an update.
	private void applyGroupChange(List<AnalysisGroup> groups) {
		if (nodes.isEmpty())
			return;
		var parent = nodes.getFirst().getParent();
		if (parent == null)
			return;

		var map = new HashMap<Long, AnalysisGroup>();
		for (var g : groups) {
			for (var pid : g.processes) {
				map.put(pid, g);
			}
		}

		for (var c : parent.getChildren()) {
			if (!(c instanceof NodeEditPart n))
				continue;
			var model = n.getModel();
			if (model == null || model.descriptor == null)
				continue;
			var group = map.get(model.descriptor.id);
			if (group != null) {
				var evt = new PropertyChangeEvent(this, Node.GROUP_PROP, null, group);
				n.propertyChange(evt);
			}
		}
	}

	private static class Dialog extends FormDialog {

		private final GraphEditor editor;
		private final List<AnalysisGroup> groups;
		private AnalysisGroup selected;
		private boolean groupsChanged;

		Dialog(
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
			UI.fillHorizontal(groupComp);
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
			Controls.onSelect(systemItem, $ -> {
				var d = ModelSelector.select(ModelType.PRODUCT_SYSTEM);
				if (d == null)
					return;
				var system = Database.get().get(ProductSystem.class, d.id);
				if (system != null) {
					onSync.accept(GroupSync.sync(this, system));
				}
			});

			// add EN 15804 modules
			var enItem = new MenuItem(menu, SWT.NONE);
			enItem.setText("Add EN 15804 modules");
			Controls.onSelect(enItem,
					$ -> onSync.accept(GroupSync.syncEn15804(this)));

			// add a new group
			Controls.onSelect(addBtn, $ -> {
				var group = new AnalysisGroup();
				group.name = "New analysis group";
				groups.add(group);
				new GroupPanel(this, group).render(groupComp, tk);
				body.layout();
				form.reflow(true);
				groupsChanged = true;
			});
		}

		private Color colorOf(AnalysisGroup group) {
			var c = group != null ? group.color : null;
			if (Strings.notEmpty(c))
				return Colors.fromHex(c);
			return editor.getTheme() != null && editor.getTheme().isDark()
					? Colors.fromHex("#FFFDE7")
					: Colors.fromHex("#212121");
		}

		private record GroupPanel(Dialog dialog, AnalysisGroup group) {

			void render(Composite parent, FormToolkit tk) {

				var radio = tk.createButton(parent, "", SWT.RADIO);
				radio.setSelection(Objects.equals(group, dialog.selected));
				Controls.onSelect(radio, $ -> {
					if (radio.getSelection()) {
						dialog.selected = group;
					}
				});

				var comp = tk.createComposite(parent);
				UI.fillHorizontal(comp);
				UI.gridLayout(comp, group == null ? 1 : 3, 10, 1);

				if (group == null) {
					UI.label(comp, tk, M.None);
					return;
				}

				var text = UI.text(comp, tk);
				UI.fillHorizontal(text);
				if (group.name != null) {
					text.setText(group.name);
				}
				text.addModifyListener($ -> {
					group.name = text.getText();
					dialog.groupsChanged = true;
				});

				// color selector
				var colorBtn = tk.createButton(comp, "□□", SWT.NONE);
				var color = dialog.colorOf(group);
				colorBtn.setForeground(color);
				colorBtn.setBackground(color);
				colorBtn.setToolTipText("Select a group color");
				Controls.onSelect(colorBtn, $ -> {
					var cd = new ColorDialog(dialog.getShell());
					cd.setRGB(color.getRGB());
					var next = cd.open();
					if (next != null) {
						var nextColor = Colors.get(next);
						colorBtn.setForeground(nextColor);
						colorBtn.setBackground(nextColor);
						group.color = String.format(
								"#%02x%02x%02x", next.red, next.green, next.blue);
						dialog.groupsChanged = true;
					}
				});

				var delete = UI.imageHyperlink(comp, tk);
				delete.setImage(Icon.DELETE_DISABLED.get());
				delete.setHoverImage(Icon.DELETE.get());
				delete.setToolTipText("Delete analysis group");

				Controls.onClick(delete, $ -> {
					dialog.groups.remove(group);
					dialog.groupsChanged = true;
					radio.dispose();
					comp.dispose();
					parent.layout();
					parent.getParent().layout();
				});
			}
		}
	}

	static class GroupSync {

		private final ProductSystem owner;
		private final List<AnalysisGroup> groups;
		private final List<AnalysisGroup> newGroups;

		GroupSync(Dialog dialog) {
			this.owner = dialog.editor.getProductSystem();
			this.groups = dialog.groups;
			this.newGroups = new ArrayList<>();
		}

		static GroupSync sync(Dialog dialog, ProductSystem system) {
			var sync = new GroupSync(dialog);
			sync.sync(system);
			return sync;
		}

		static GroupSync syncEn15804(Dialog dialog) {
			var groups = List.of(

					groupOf("A1-A3", "#311B92"),
					groupOf("A1", "#673AB7"),
					groupOf("A2", "#5E35B1"),
					groupOf("A3", "#512DA8"),

					groupOf("A4-A5", "#1A237E"),
					groupOf("A4", "#3F51B5"),
					groupOf("A5", "#3949AB"),

					groupOf("B1-B7", "#004D40"),
					groupOf("B1", "#80CBC4"),
					groupOf("B2", "#4DB6AC"),
					groupOf("B3", "#26C6DA"),
					groupOf("B4", "#009688"),
					groupOf("B5", "#00897B"),
					groupOf("B6", "#00796B"),
					groupOf("B7", "#00695C"),

					groupOf("C1-C4", "#3E2723"),
					groupOf("C1", "#795548"),
					groupOf("C2", "#6D4C41"),
					groupOf("C3", "#5D4037"),
					groupOf("C4", "#4E342E"),

					groupOf("D", "#455A64"));

			var sync = new GroupSync(dialog);
			for (var g : groups) {
				sync.sync(g);
			}
			return sync;
		}

		private static AnalysisGroup groupOf(String name, String color) {
			var g = new AnalysisGroup();
			g.name = name;
			g.color = color;
			return g;
		}

		private void sync(ProductSystem system) {
			if (system == null || system.analysisGroups.isEmpty())
				return;
			for (var g : system.analysisGroups) {
				sync(g);
			}
		}

		private void sync(AnalysisGroup next) {
			if (next == null || Strings.nullOrEmpty(next.name))
				return;
			for (var g : groups) {
				if (Strings.nullOrEqual(g.name, next.name)) {
					copyAttributes(next, g);
					return;
				}
			}
			var g = new AnalysisGroup();
			g.name = next.name;
			copyAttributes(next, g);
			groups.add(g);
			newGroups.add(g);
		}

		private void copyAttributes(AnalysisGroup source, AnalysisGroup target) {
			if (Strings.nullOrEmpty(target.color)) {
				target.color = source.color;
			}
			for (var p : source.processes) {
				if (target.processes.contains(p)
						|| !owner.processes.contains(p))
					continue;
				target.processes.add(p);
			}
		}
	}
}

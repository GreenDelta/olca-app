package org.openlca.app.editors.graphical.model.commands;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

public class SetProcessGroupCommand extends Command {

	private final NodeEditPart node;
	private final RootDescriptor process;

	public SetProcessGroupCommand(NodeEditPart node) {
		this.node = node;
		this.process = node.getModel() != null
				? node.getModel().descriptor
				: null;
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
		return process != null;
	}

	@Override
	public void execute() {
		var editor = node.getParent() instanceof GraphEditPart gp
				? gp.getModel().editor
				: null;
		if (editor == null)
			return;
		var groups = editor.getProductSystem().analysisGroups;
		groups.sort((g1, g2) -> Strings.compare(g1.name, g2.name));
		var current = findCurrent(groups);
		var dialog = new Dialog(editor, groups, current);

		if (dialog.open() == Window.OK
				&& !Objects.equals(current, dialog.selected)) {
			if (current != null) {
				current.processes.remove(process.id);
			}
			if (dialog.selected != null) {
				dialog.selected.processes.add(process.id);
			}
			node.propertyChange(new PropertyChangeEvent(
					this, Node.GROUP_PROP, current, dialog.selected));
			editor.setDirty();
		}

		if (dialog.groupsChanged) {
			// TODO: notify affected nodes
			editor.setDirty();
		}
	}

	private AnalysisGroup findCurrent(List<AnalysisGroup> groups) {
		for (var g : groups) {
			if (g.processes.contains(process.id))
				return g;
		}
		return null;
	}

	private static class Dialog extends FormDialog {

		private final GraphEditor editor;
		private final List<AnalysisGroup> groups;
		private AnalysisGroup selected;
		private boolean groupsChanged;

		Dialog(
				GraphEditor editor,
				List<AnalysisGroup> groups,
				AnalysisGroup selected) {
			super(UI.shell());
			this.editor = editor;
			this.groups = groups;
			this.selected = selected;
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

			var addBtn = tk.createButton(body, "Add analysis group", SWT.NONE);
			addBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
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

		private static class GroupPanel {

			final Dialog dialog;
			final AnalysisGroup group;

			GroupPanel(Dialog dialog, AnalysisGroup group) {
				this.dialog = dialog;
				this.group = group;
			}

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
}

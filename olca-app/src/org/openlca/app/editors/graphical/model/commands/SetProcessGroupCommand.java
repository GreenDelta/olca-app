package org.openlca.app.editors.graphical.model.commands;

import java.util.List;
import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.descriptors.RootDescriptor;

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
		var current = findCurrent(groups);
		var dialog = new Dialog(groups, current);

		if (dialog.open() == Window.OK
				&& !Objects.equals(current, dialog.selected)) {
			if (current != null) {
				current.processes.remove(process.id);
			}
			if (dialog.selected != null) {
				dialog.selected.processes.add(process.id);
			}
			editor.setDirty();
		}

		if (dialog.groupsChanged) {
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

		private final List<AnalysisGroup> groups;
		private AnalysisGroup selected;
		private boolean groupsChanged;

		Dialog(List<AnalysisGroup> groups, AnalysisGroup selected) {
			super(UI.shell());
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
			new GroupPanel(this, null).render(body, tk);
			for (var group : groups) {
				new GroupPanel(this, group).render(body, tk);
			}
		}

		private static class GroupPanel {

			final Dialog dialog;
			final AnalysisGroup group;

			GroupPanel(Dialog dialog, AnalysisGroup group) {
				this.dialog = dialog;
				this.group = group;
			}

			void render(Composite body, FormToolkit tk) {
				var comp = tk.createComposite(body);
				UI.fillHorizontal(comp);
				UI.gridLayout(comp, group == null ? 2 : 4);

				var radio = tk.createButton(comp, "", SWT.RADIO);
				radio.setSelection(Objects.equals(group, dialog.selected));
				Controls.onSelect(radio, $ -> {
					if (radio.getSelection()) {
						var str = group != null ? group.name : "None";
						System.out.println("selected: " + str);
						dialog.selected = group;
					}
				});

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

				var color = tk.createButton(comp, "O", SWT.NONE);
				color.setForeground(Colors.black());
				color.setBackground(Colors.black());

				var delete = tk.createButton(comp, "", SWT.NONE);
				delete.setImage(Icon.DELETE.get());
				Controls.onSelect(delete, $ -> {
					dialog.groups.remove(group);
					dialog.groupsChanged = true;
					comp.dispose();
				});
			}
		}
	}
}

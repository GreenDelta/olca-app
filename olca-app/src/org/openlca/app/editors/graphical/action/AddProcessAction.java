package org.openlca.app.editors.graphical.action;

import java.util.Date;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.command.CreateProcessCommand;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

public class AddProcessAction extends Action {

	public static final String ID = "AddProcessAction";
	private final GraphEditor editor;

	public AddProcessAction(GraphEditor editor) {
		this.editor = editor;
		setId(ID);
		setText("Add a process");
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

	@Override
	public void run() {
		var d = new Dialog();
		d.open();
	}

	private class Dialog extends FormDialog {

		final int _CREATE = 8;
		final int _SELECT = 16;
		final int _CANCEL = 32;

		TreeViewer tree;
		Text text;

		Dialog() {
			super(UI.shell());
			setBlockOnOpen(true);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1);
			var nameLabel = UI.formLabel(body, tk, "Create with name");
			nameLabel.setFont(UI.boldFont());
			text = UI.formText(body, SWT.NONE);
			var selectLabel = UI.formLabel(body, tk, "Or select existing");
			selectLabel.setFont(UI.boldFont());
			tree = NavigationTree.forSingleSelection(body, ModelType.PROCESS);
			UI.gridData(tree.getControl(), true, true);
			tree.addFilter(new ModelTextFilter(text, tree));

			// enable/disable select button
			tree.addSelectionChangedListener(e -> {
				var obj = Selections.firstOf(e);
				if (obj instanceof CategorizedDescriptor) {
					var d = (CategorizedDescriptor) obj;
					if (d.type == ModelType.PROCESS
							|| d.type == ModelType.PRODUCT_SYSTEM) {
						getButton(_SELECT).setEnabled(true);
						return;
					}
				}
				getButton(_SELECT).setEnabled(false);
			});

			// add process on double click
			tree.addDoubleClickListener(e -> {
				var obj = Selections.firstOf(e);
				if (obj instanceof CategorizedDescriptor) {
					var d = (CategorizedDescriptor) obj;
					if (d.type == ModelType.PROCESS
							|| d.type == ModelType.PRODUCT_SYSTEM) {
						addProcess(d);
					}
				}
			});

			// enable disable create button
			text.addModifyListener(e -> {
				var name = text.getText().trim();
				getButton(_CREATE).setEnabled(Strings.notEmpty(name));
			});
		}

		@Override
		protected Point getInitialSize() {
			var shell = getShell().getDisplay().getBounds();
			int width = shell.x > 0 && shell.x < 600
					? shell.x
					: 600;
			int height = shell.y > 0 && shell.y < 600
					? shell.y
					: 600;
			return new Point(width, height);
		}

		@Override
		protected void createButtonsForButtonBar(Composite comp) {
			createButton(comp, _CREATE, "Create new", false)
					.setEnabled(false);
			createButton(comp, _SELECT, "Select extisting", false)
					.setEnabled(false);
			createButton(comp, _CANCEL, M.Cancel, true);
		}

		@Override
		protected void buttonPressed(int button) {
			// cancel
			if (button == _CANCEL) {
				cancelPressed();
				return;
			}

			// select existing
			if (button == _SELECT) {
				var obj = Viewers.getFirstSelected(tree);
				if (!(obj instanceof CategorizedDescriptor)) {
					cancelPressed();
					return;
				}
				addProcess((CategorizedDescriptor) obj);
				return;
			}

			// create a new process
			if (button == _CREATE) {
				var name = text.getText().trim();
				if (Strings.nullOrEmpty(name)) {
					cancelPressed();
					return;
				}
				var process = new Process();
				process.name = name;
				process.refId = UUID.randomUUID().toString();
				process.processType = ProcessType.UNIT_PROCESS;
				process.lastChange = new Date().getTime();
				process = Database.get().insert(process);
				addProcess(Descriptor.of(process));
			}
		}

		private void addProcess(CategorizedDescriptor d) {
			if (d.type != ModelType.PROCESS
					&& d.type != ModelType.PRODUCT_SYSTEM) {
				return;
			}
			var system = editor.getModel().getProductSystem();
			if (system.processes.contains(d.id)) {
				MsgBox.info("The product system already"
						+ " contains process `"
						+ Labels.name(d) + "`.");
				cancelPressed();
			}
			var cmd = new CreateProcessCommand(
					editor.getModel(), d);
			editor.getCommandStack().execute(cmd);
			super.okPressed();
		}
	}
}

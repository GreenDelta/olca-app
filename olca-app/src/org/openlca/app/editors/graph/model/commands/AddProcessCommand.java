package org.openlca.app.editors.graph.model.commands;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.app.editors.graph.layouts.NodeLayoutInfo;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

import java.util.Date;
import java.util.UUID;

public class AddProcessCommand extends Command {

	private final GraphEditor editor;
	private final Graph graph;
	private Point cursorLocation;

	public AddProcessCommand(Graph graph) {
		this.graph = graph;
		this.editor = graph.editor;
	}

	@Override
	public boolean canExecute() {
		cursorLocation = Display.getCurrent().getCursorLocation();
		return editor != null && editor.getProductSystem() != null;
	}

	@Override
	public boolean canUndo() {
		// TODO (francois) Implement undo.
		return false;
	}

	@Override
	public void execute() {
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

			// create new text
			var nameLabel = UI.formLabel(body, tk, "Create with name");
			nameLabel.setFont(UI.boldFont());
			text = UI.formText(body, SWT.NONE);
			text.addModifyListener(e -> {
				var name = text.getText().trim();
				getButton(_CREATE).setEnabled(Strings.notEmpty(name));
			});

			// tree
			var selectLabel = UI.formLabel(body, tk, "Or select existing");
			selectLabel.setFont(UI.boldFont());
			tree = NavigationTree.forSingleSelection(body, ModelType.PROCESS);
			UI.gridData(tree.getControl(), true, true);
			tree.addFilter(new ModelTextFilter(text, tree));

			// enable/disable select button
			tree.addSelectionChangedListener(e -> {
				var d = unwrap(e.getSelection());
				getButton(_SELECT).setEnabled(d != null);
			});

			// add process on double click
			tree.addDoubleClickListener(e -> {
				var d = unwrap(e.getSelection());
				if (d != null) {
					addProcess(d);
				}
			});
		}

		private RootDescriptor unwrap(ISelection s) {
			var obj = Selections.firstOf(s);
			RootDescriptor d = null;
			if (obj instanceof RootDescriptor) {
				d = (RootDescriptor) obj;
			} else if (obj instanceof ModelElement) {
				d = ((ModelElement) obj).getContent();
			}
			if (d == null)
				return null;
			var matches = d.type == ModelType.PROCESS
				|| d.type == ModelType.PRODUCT_SYSTEM;
			return matches ? d : null;
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 600, 600);
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
				if (!(obj instanceof ModelElement element)) {
					cancelPressed();
					return;
				}
				addProcess(element.getContent());
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
				Navigator.refresh();
			}
		}

		private void addProcess(RootDescriptor d) {
			if (d.type != ModelType.PROCESS
				&& d.type != ModelType.PRODUCT_SYSTEM) {
				return;
			}

			// add the process to the product system
			var system = graph.getProductSystem();
			if (system.processes.contains(d.id)) {
				MsgBox.info("The product system already"
					+ " contains process `"
					+ Labels.name(d) + "`.");
				cancelPressed();
				return;
			}
			system.processes.add(d.id);

			// create the process node
			var viewer = (GraphicalViewer) editor.getAdapter(
				GraphicalViewer.class);
			var location = viewer.getControl().toControl(cursorLocation);
			var info = new NodeLayoutInfo(location, null, true, false, false);
			var node = editor.getGraphFactory().createNode(d, info);
			graph.addChild(node);

			editor.setDirty();
			super.okPressed();
		}

	}

}

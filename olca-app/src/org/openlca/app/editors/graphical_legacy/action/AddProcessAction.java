package org.openlca.app.editors.graphical_legacy.action;

import java.util.Date;
import java.util.UUID;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
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
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Images;
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

public class AddProcessAction extends Action implements GraphAction {

	private ProductSystemNode systemNode;
	private Point location;

	public AddProcessAction() {
		setId("AddProcessAction");
		setText("Add a process");
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

	@Override
	public boolean accepts(GraphEditor editor) {
		systemNode = GraphActions.firstSelectedOf(
				editor, ProductSystemNode.class);
		if (systemNode == null
				|| systemNode.getProductSystem() == null)
			return false;
		var displayLoc = Display.getCurrent()
				.getCursorLocation();
		this.location = editor.getGraphicalViewer()
				.getControl()
				.toControl(displayLoc);
		return true;
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
				if (!(obj instanceof RootDescriptor)) {
					cancelPressed();
					return;
				}
				addProcess((RootDescriptor) obj);
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
			var system = systemNode.getProductSystem();
			if (system.processes.contains(d.id)) {
				MsgBox.info("The product system already"
						+ " contains process `"
						+ Labels.name(d) + "`.");
				cancelPressed();
				return;
			}
			system.processes.add(d.id);

			// create the process node
			var editor = systemNode.editor;
			var processNode = new ProcessNode(editor, d);
			systemNode.add(processNode);
			if (editor.getOutline() != null) {
				editor.getOutline().refresh();
			}

			// set the node position
			processNode.maximize();
			if (location != null) {
				var rect = new Rectangle(
						location.x,
						location.y,
						Math.max(processNode.getMinimumWidth(), 250),
						Math.max(processNode.getMinimumHeight(), 150));
				processNode.setBox(rect);
			}

			editor.setDirty();
			super.okPressed();
		}
	}
}

package org.openlca.app.editors.graphical.actions;

import java.util.Date;
import java.util.UUID;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.requests.GraphRequest;
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

import static org.eclipse.gef.RequestConstants.REQ_CREATE;

public class AddProcessAction extends WorkbenchPartAction {

	private final Graph graph;
	private final GraphEditor editor;
	private org.eclipse.swt.graphics.Point cursorLocation;

	public AddProcessAction(GraphEditor part) {
		super(part);
		editor = part;
		graph = part.getModel();
		setId(ActionIds.ADD_PROCESS);
		setText(M.AddProcess);
		setImageDescriptor(Images.descriptor(ModelType.PROCESS));
	}

	@Override
	public void run() {
		var d = new Dialog();
		d.open();
	}

	@Override
	protected boolean calculateEnabled() {
		cursorLocation = Display.getCurrent().getCursorLocation();
		return editor != null && editor.getProductSystem() != null;
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
		protected org.eclipse.swt.graphics.Point getInitialSize() {
			return UI.initialSizeOf(this, 600, 600);
		}

		@Override
		protected void createButtonsForButtonBar(Composite comp) {
			createButton(comp, _CREATE, "Create new", false)
				.setEnabled(false);
			createButton(comp, _SELECT, "Select existing", false)
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
			var system = graph.getProductSystem();
			if (system.processes.contains(d.id)) {
				MsgBox.info("The product system already"
					+ " contains process `"
					+ Labels.name(d) + "`.");
				cancelPressed();
				return;
			}

			var viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(
				GraphicalViewer.class);
			var registry = viewer.getEditPartRegistry();
			var graphEditPart = (EditPart) registry.get(graph);
			if (graphEditPart == null)
				return;

			var cursorLocationInViewport = new Point(viewer.getControl()
				.toControl(cursorLocation));
			var request = new GraphRequest(REQ_CREATE);
			request.setDescriptors(d);
			request.setLocation(cursorLocationInViewport);
			// Getting the command via GraphXYLayoutEditPolicy and executing it.
			var command = graphEditPart.getCommand(request);
			if (command.canExecute())
				execute(command);
			else {
				MsgBox.info("This item cannot be added to the product system `"
					+ Labels.name(d) + "`.");
				cancelPressed();
			}

			super.okPressed();
		}

	}

}

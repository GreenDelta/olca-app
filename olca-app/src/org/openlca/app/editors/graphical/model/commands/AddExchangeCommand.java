package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.filters.FlowTypeFilter;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.combo.FlowPropertyCombo;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

public class AddExchangeCommand extends Command {

	private final IDatabase db = Database.get();
	private final IOPane pane;
	private final boolean forInput;
	private final GraphEditor editor;

	public AddExchangeCommand(IOPane pane, boolean forInput) {
		this.pane = pane;
		this.forInput = forInput;
		this.editor = pane.getGraph().getEditor();
	}

	@Override
	public boolean canExecute() {
		return pane != null && pane.getNode().isEditable();
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		redo();
	}

	@Override
	public void redo() {
		var node = pane.getNode();
		if (node == null)
			return;

		if (!(node.getEntity() instanceof Process process))
			return;

		// add the flow
		var dialog = new Dialog();
		if (dialog.open() != Window.OK || dialog.flow == null)
			return;
		var flow = dialog.flow;

		if (!pane.accepts(flow)) {
			MsgBox.info((pane.isForInputs()
					? M.FlowCannotBeAddedTwiceToInputs
					: M.FlowCannotBeAddedTwiceToOutputs)
					+ "\r\n " + Labels.name(node.descriptor));
			return;
		}

		// create the exchange and add to the process.
		var exchange = forInput
				? Exchange.input(flow, 1.0)
				: Exchange.output(flow, 1.0);
		if (!ExchangeDialog.open(exchange))
			return;
		process.add(exchange);

		// If an elementary flow was added, make sure that our graph shows
		// elementary flow.
		// Note that we need to do this, before we create the IOPane in order to
		// avoid recreation of that node later.
		if (flow.flowType == FlowType.ELEMENTARY_FLOW)
			editor.config.setShowElementaryFlows(true);

		var ioPane = forInput ? node.getInputIOPane() : node.getOutputIOPane();
		ioPane.addChild(new ExchangeItem(exchange));

		editor.setDirty(process);
	}

	class Dialog extends FormDialog {

		final int _CREATE = 8;
		final int _SELECT = 16;
		final int _CANCEL = 32;

		TreeViewer tree;
		Text text;
		Flow flow;
		FlowType type = FlowType.PRODUCT_FLOW;
		FlowProperty quantity;

		private final FlowTypeFilter wasteFilter = new FlowTypeFilter(
				FlowType.ELEMENTARY_FLOW, FlowType.PRODUCT_FLOW);
		private final FlowTypeFilter productFilter = new FlowTypeFilter(
				FlowType.ELEMENTARY_FLOW, FlowType.WASTE_FLOW);
		private final FlowTypeFilter elemFilter = new FlowTypeFilter(
				FlowType.PRODUCT_FLOW, FlowType.WASTE_FLOW);

		Dialog() {
			super(UI.shell());
			setBlockOnOpen(true);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var body = UI.dialogBody(form.getForm(), tk);
			UI.gridLayout(body, 1);

			// create new text
			var nameLabel = UI.label(body, tk, M.CreateANewFlow);
			nameLabel.setFont(UI.boldFont());
			text = UI.text(body, SWT.NONE);
			text.addModifyListener(e -> {
				var name = text.getText().trim();
				getButton(_CREATE).setEnabled(Strings.notEmpty(name));
			});

			// flow type selection
			var typeComp = UI.composite(body, tk);
			UI.gridData(typeComp, true, false);
			UI.gridLayout(typeComp, 3, 5, 0).makeColumnsEqualWidth = true;
			var types = new FlowType[]{
				FlowType.PRODUCT_FLOW,
				FlowType.WASTE_FLOW,
				FlowType.ELEMENTARY_FLOW,
			};
			for (var type : types) {
				var btn = UI.radio(typeComp, tk, Labels.of(type));
				if (type == FlowType.PRODUCT_FLOW) {
					btn.setSelection(true);
				}
				Controls.onSelect(btn, e -> {
					this.type = type;
					tree.removeFilter(productFilter);
					tree.removeFilter(wasteFilter);
					tree.removeFilter(elemFilter);
					if (type == FlowType.PRODUCT_FLOW) {
						tree.addFilter(productFilter);
					} else if (type == FlowType.ELEMENTARY_FLOW) {
						tree.addFilter(elemFilter);
					} else if (type == FlowType.WASTE_FLOW) {
						tree.addFilter(wasteFilter);
					}
				});
			}

			// flow property
			var propComp = UI.composite(body, tk);
			UI.gridData(propComp, true, false);
			UI.gridLayout(propComp, 1, 5, 0);
			var propViewer = new FlowPropertyCombo(propComp);
			propViewer.setInput(db);
			propViewer.selectFirst();
			this.quantity = propViewer.getSelected();
			propViewer.addSelectionChangedListener(
				prop -> this.quantity = prop);

			// tree
			var selectLabel = UI.label(body, tk, M.OrSelectExisting);
			selectLabel.setFont(UI.boldFont());
			tree = NavigationTree.forSingleSelection(body, ModelType.FLOW);
			tree.addFilter(productFilter);
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
				if (d == null)
					return;
				var flow = Database.get().get(Flow.class, d.id);
				if (flow != null) {
					this.flow = flow;
					okPressed();
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
			return d.type == ModelType.FLOW ? d : null;
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 600, 600);
		}

		@Override
		protected void createButtonsForButtonBar(Composite comp) {
			createButton(comp, _CREATE, M.CreateNew, false)
				.setEnabled(false);
			createButton(comp, _SELECT, M.SelectExisting, false)
				.setEnabled(false);
			createButton(comp, _CANCEL, M.Cancel, true);
		}

		@Override
		protected void buttonPressed(int button) {
			if (button == _CANCEL) {
				cancelPressed();
				return;
			}

			if (button == _SELECT) {
				var d = unwrap(tree.getSelection());
				if (d == null) {
					cancelPressed();
					return;
				}
				flow = db.get(Flow.class, d.id);
				if (flow == null) {
					cancelPressed();
				} else {
					okPressed();
				}
				return;
			}

			if (button != _CREATE)
				return;
			flow = createFlow();
			if (flow == null) {
				cancelPressed();
			} else {
				okPressed();
			}
		}

		private Flow createFlow() {
			var prop = quantity != null
				? db.get(FlowProperty.class, quantity.id)
				: null;
			if (prop == null) {
				MsgBox.error(M.CannotCreateAFlowWithoutQuantity);
				return null;
			}
			var name = this.text.getText().trim();
			if (Strings.nullOrEmpty(name)) {
				MsgBox.error(M.ANameIsRequired);
				return null;
			}
			var type = this.type == null
				? FlowType.PRODUCT_FLOW
				: this.type;
			var flow = db.insert(Flow.of(name, type, prop));
			editor.getModel().flows.reload(db);
			Navigator.refresh();
			return flow;
		}

	}

}

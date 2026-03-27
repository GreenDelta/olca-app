package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ModelLink;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.SystemAddCmd;
import org.openlca.app.editors.sd.editor.graph.edit.SystemUpdateCmd;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ProductSystem;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.SystemBinding;
import org.openlca.sd.model.VarBinding;

public class SystemEditDialog extends FormDialog {

	private final SdGraphEditor editor;
	private final SdModel model;
	private final SystemBinding working;
	private final SystemNode origin;
	private final Point location;

	private Text amountText;

	public static void edit(SdGraphEditor editor, SystemNode node) {
		if (editor == null || node == null)
			return;
		new SystemEditDialog(editor, node).open();
	}

	public static void create(
		SdGraphEditor editor, SystemBinding binding, Point location
	) {
		if (editor == null || binding == null)
			return;
		var point = location != null ? location : new Point(250, 250);
		new SystemEditDialog(editor, binding, point).open();
	}

	private SystemEditDialog(SdGraphEditor editor, SystemNode node) {
		super(UI.shell());
		this.editor = editor;
		this.model = editor.graph().model();
		this.origin = node;
		this.location = null;
		var b = node.binding();
		this.working = new SystemBinding(b.system());
		working.setAmount(b.amount());
		working.setAmountVar(b.amountVar());
		working.varBindings().addAll(b.varBindings());
	}

	private SystemEditDialog(
		SdGraphEditor editor, SystemBinding binding, Point location
	) {
		super(UI.shell());
		this.editor = editor;
		this.model = editor.graph().model();
		this.working = binding;
		this.origin = null;
		this.location = location;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		createRefAmountSection(body, tk);
		createBindingsSection(body, tk);
		mForm.getForm().reflow(true);
	}

	private void createRefAmountSection(Composite body, FormToolkit tk) {
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 3);
		UI.gridData(comp, true, false);

		var db = Database.get();
		var sysRef = working.system();
		var system = sysRef != null && db != null
			? db.get(ProductSystem.class, sysRef.refId())
			: null;

		// product system link
		UI.label(comp, tk, "Product system");
		ModelLink.of(ProductSystem.class)
			.setModel(system)
			.setEditable(false)
			.renderOn(comp, tk);
		UI.filler(comp, tk);

		// reference flow link
		var refFlow = system != null && system.referenceExchange != null
			? system.referenceExchange.flow
			: null;
		UI.label(comp, tk, "Reference flow");
		ModelLink.of(Flow.class)
			.setModel(refFlow)
			.setEditable(false)
			.renderOn(comp, tk);
		UI.filler(comp, tk);

		// amount text
		var amount = working.amountVar() != null
			? working.amountVar().label()
			: Double.toString(working.amount());
		amountText = UI.labeledText(comp, tk, "Reference amount");
		amountText.setText(amount);

		// unit label
		var unit = system != null && system.targetUnit != null
			? system.targetUnit.name
			: "?";
		UI.label(comp, tk, unit);

		// button for selecting a variable
		UI.filler(comp, tk);
		var varBtn = UI.button(comp, tk, "Select a variable");
		varBtn.setImage(Icon.FORMULA.get());
		varBtn.addListener(SWT.Selection, e ->
			VarSelectDialog.selectFrom(model)
				.ifPresent(id -> amountText.setText(id.label())));
	}

	private void createBindingsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Parameter bindings");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var table = Tables.createViewer(comp, "Model variable", "Parameter");
		Tables.bindColumnWidths2(table, 0.5, 0.5);
		var gd = UI.gridData(table.getTable(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;

		table.setLabelProvider(new VarBindingLabel());
		table.setInput(working.varBindings());

		var onAdd = Actions.create(
			"Add parameter binding",
			Icon.ADD.descriptor(),
			() -> VarBindingDialog.create(model, working)
				.ifPresent(vb -> {
					working.varBindings().add(vb);
					table.setInput(working.varBindings());
				}));

		var onDel = Actions.create(
			"Remove parameter binding",
			Icon.DELETE.descriptor(),
			() -> {
				VarBinding vb = Viewers.getFirstSelected(table);
				if (vb != null) {
					working.varBindings().remove(vb);
					table.setInput(working.varBindings());
				}
			});

		Actions.bind(table, onAdd, onDel);
		Actions.bind(section, onAdd, onDel);
	}

	@Override
	protected void okPressed() {

		// parse & check the reference amount
		var amount = amountText.getText() != null
			? amountText.getText().trim()
			: "";
		try {
			working.setAmount(Double.parseDouble(amount));
			working.setAmountVar(null);
		} catch (Exception ignored) {
			var variable = Id.of(amount);
			if (!Vars.contains(model, variable)) {
				MsgBox.error("Invalid reference amount",
					"Enter a number or the name of an existing variable.");
				return;
			}
			working.setAmountVar(variable);
		}

		if (origin == null) {
			var node = new SystemNode(working);
			node.moveTo(new Rectangle(
				location.x - 40, location.y - 20, 80, 40));
			var cmd = new SystemAddCmd(editor.graph(), node);
			editor.exec(cmd);
		} else {
			var cmd = new SystemUpdateCmd(editor.graph(), origin, working);
			editor.exec(cmd);
		}
		super.okPressed();
	}

	private static class VarBindingLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof VarBinding vb))
				return "";
			return switch (col) {
				case 0 -> vb.varId() != null ? vb.varId().label() : "";
				case 1 -> vb.parameter() != null ? vb.parameter() : "";
				default -> "";
			};
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.FORMULA.get();
		}
	}
}

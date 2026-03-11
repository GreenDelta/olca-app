package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.VarBindingDialog;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.editors.sd.editor.graph.edit.AddSystemCmd;
import org.openlca.app.editors.sd.editor.graph.edit.UpdateSystemCmd;
import org.openlca.app.editors.sd.editor.graph.model.SystemNode;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ProductSystem;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SystemBinding;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.VarBinding;

import java.util.ArrayList;
import java.util.List;

public class SystemEditDialog extends FormDialog {

	private final SdGraphEditor editor;
	private final SystemBinding working;
	private final SystemNode origin;
	private final Point location;

	private Text amountText;
	private Combo amountVarCombo;
	private List<Var> vars;
	private List<Id> varIds;

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
		vars = editor.parent().vars();
		createInfoSection(body, tk);
		createBindingsSection(body, tk);
		mForm.getForm().reflow(true);
	}

	private void createInfoSection(Composite body, FormToolkit tk) {
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 3);
		UI.gridData(comp, true, false);

		var db = Database.get();
		var sysRef = working.system();
		var system = sysRef != null && db != null
			? db.get(ProductSystem.class, sysRef.refId())
			: null;

		// product system (read-only)
		UI.label(comp, tk, "Product system");
		ModelLink.of(ProductSystem.class)
			.setModel(system)
			.setEditable(false)
			.renderOn(comp, tk);
		UI.filler(comp, tk);

		// reference flow (read-only)
		var refFlow = system != null && system.referenceExchange != null
			? system.referenceExchange.flow
			: null;
		UI.label(comp, tk, "Reference flow");
		ModelLink.of(Flow.class)
			.setModel(refFlow)
			.setEditable(false)
			.renderOn(comp, tk);
		UI.filler(comp, tk);

		// amount
		var unit = system != null && system.targetUnit != null
			? system.targetUnit.name
			: "?";
		amountText = UI.labeledText(comp, tk, M.Amount);
		amountText.setText(Double.toString(working.amount()));
		UI.label(comp, tk, unit);

		// amount variable
		UI.label(comp, tk, "Amount variable");
		amountVarCombo = new Combo(comp, SWT.READ_ONLY);
		tk.adapt(amountVarCombo);
		UI.gridData(amountVarCombo, true, false);
		UI.filler(comp, tk);

		varIds = new ArrayList<>();
		amountVarCombo.add("");
		varIds.add(null);
		int selectedIndex = 0;
		for (int i = 0; i < vars.size(); i++) {
			var v = vars.get(i);
			var label = v.name() != null ? v.name().label() : "";
			amountVarCombo.add(label);
			varIds.add(v.name());
			if (working.amountVar() != null
				&& working.amountVar().equals(v.name())) {
				selectedIndex = i + 1;
			}
		}
		amountVarCombo.select(selectedIndex);
	}

	private void createBindingsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Parameter bindings");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk);

		var table = Tables.createViewer(comp, "Model variable", "Parameter");
		Tables.bindColumnWidths(table, 0.5, 0.5);
		table.setLabelProvider(new VarBindingLabel());
		table.setInput(working.varBindings());

		var onAdd = Actions.create(
			"Add parameter binding",
			Icon.ADD.descriptor(),
			() -> VarBindingDialog.create(vars, working)
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
		try {
			working.setAmount(Double.parseDouble(amountText.getText()));
		} catch (Exception ignored) {
		}

		int idx = amountVarCombo.getSelectionIndex();
		working.setAmountVar(idx > 0 ? varIds.get(idx) : null);

		if (origin == null) {
			var node = new SystemNode(working);
			node.moveTo(new Rectangle(
				location.x - 40, location.y - 20, 80, 40));
			var cmd = new AddSystemCmd(editor.graph(), node);
			editor.exec(cmd);
		} else {
			var cmd = new UpdateSystemCmd(
				origin,
				working.amount(),
				working.amountVar(),
				new ArrayList<>(working.varBindings()));
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

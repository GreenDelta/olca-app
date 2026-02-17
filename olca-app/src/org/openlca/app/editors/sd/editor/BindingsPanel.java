package org.openlca.app.editors.sd.editor;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.editors.sd.interop.SimulationSetup;
import org.openlca.app.editors.sd.interop.SystemBinding;
import org.openlca.app.editors.sd.interop.VarBinding;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

class BindingsPanel {

	private final IDatabase db;
	private final SdModelEditor editor;
	private final SimulationSetup setup;
	private final ScrolledForm form;
	private final FormToolkit tk;
	private final Composite body;
	private final AddButton addButton;

	BindingsPanel(
		Composite body, SdModelEditor editor, FormToolkit tk, ScrolledForm form
	) {
		this.editor = editor;
		this.setup = editor.setup();
		this.db = editor.db();
		this.tk = tk;
		this.form = form;
		this.body = UI.composite(body, tk);
		UI.gridLayout(this.body, 1);
		UI.gridData(this.body, true, false);

		var bindings = setup.systemBindings();
		for (int i = 0; i < bindings.size(); i++) {
			new BindingSection(i, bindings.get(i));
		}
		addButton = new AddButton();
	}

	private void addSectionOf(SystemBinding binding) {
		if (binding == null)
			return;
		var pos = setup.systemBindings().size();
		setup.systemBindings().add(binding);
		new BindingSection(pos, binding);
		addButton.render();
		form.reflow(true);
		editor.setDirty();
	}

	private class BindingSection {

		final int pos;
		final SystemBinding binding;

		BindingSection(int pos, SystemBinding binding) {
			this.pos = pos;
			this.binding = binding;
			render();
		}

		private void render() {
			var name = binding.system() != null
				? binding.system().name
				: "System binding " + (pos + 1);
			var section = UI.section(body, tk, name);
			UI.gridData(section, true, false);

			var onDelete = Actions.create("Delete", Icon.DELETE.descriptor(), () -> {
				setup.systemBindings().remove(binding);
				section.dispose();
				form.reflow(true);
				editor.setDirty();
			});
			Actions.bind(section, onDelete);

			var comp = UI.sectionClient(section, tk);
			UI.gridLayout(comp, 1);
			createQRefSection(comp);
			createParamTable(comp);
		}

		private void createQRefSection(Composite parent) {
			var comp = UI.formSection(parent, tk, "Quantitative reference", 3);

			// Product system selection
			UI.label(comp, tk, "Product system");
			ModelLink.of(ProductSystem.class)
				.setModel(binding.system())
				.setEditable(false)
				.renderOn(comp, tk);
			UI.filler(comp, tk);

			UI.label(comp, tk, "Reference flow");
			ModelLink.of(Flow.class)
				.setModel(binding.flow())
				.setEditable(false)
				.renderOn(comp, tk);
			UI.filler(comp, tk);

			var unit = binding.unit();
			var unitSymbol = unit != null && Strings.isNotBlank(unit.name)
				? unit.name
				: "?";
			var amountText = UI.labeledText(comp, tk, M.Amount);
			amountText.setText(Double.toString(binding.amount()));
			UI.label(comp, tk, unitSymbol);

			amountText.addModifyListener(e -> {
				try {
					double v = Double.parseDouble(amountText.getText());
					binding.amount(v);
					editor.setDirty();
				} catch (Exception ignored) {
				}
			});
		}

		private void createParamTable(Composite parent) {
			var section = UI.section(parent, tk, "Parameter bindings");
			var comp = UI.sectionClient(section, tk);

			var table = Tables.createViewer(comp, "Model variable", "Parameter");
			Tables.bindColumnWidths(table, 0.5, 0.5);
			table.setLabelProvider(new VarBindingLabelProvider());

			var onAdd = Actions.create(
				"Add parameter binding",
				Icon.ADD.descriptor(),
				() -> VarBindingDialog.create(editor.vars(), binding)
					.ifPresent(vb -> {
						binding.varBindings().add(vb);
						table.setInput(binding.varBindings());
						editor.setDirty();
					}));

			var onDel = Actions.create(
				"Remove parameter binding", Icon.DELETE.descriptor(), () -> {
					VarBinding vb = Viewers.getFirstSelected(table);
					if (vb != null) {
						binding.varBindings().remove(vb);
						table.setInput(binding.varBindings());
						editor.setDirty();
					}
				});

			Actions.bind(table, onAdd, onDel);
			Actions.bind(section, onAdd, onDel);
			table.setInput(binding.varBindings());
		}
	}

	class AddButton {

		Composite comp;

		AddButton() {
			render();
		}

		void render() {
			if (comp != null) {
				comp.dispose();
			}
			comp = UI.composite(body, tk);
			UI.fillHorizontal(comp);
			var grid = UI.gridLayout(comp, 1);
			grid.marginLeft = 0;
			grid.marginTop = 0;
			grid.marginBottom = 10;
			var btn = UI.button(comp, tk, "Add product system");
			btn.setImage(Icon.ADD.get());
			UI.gridData(btn, false, false).horizontalAlignment = SWT.CENTER;
			Controls.onSelect(btn, e -> {
				var d = ModelSelector.select(ModelType.PRODUCT_SYSTEM);
				if (d == null)
					return;
				var sys = db.get(ProductSystem.class, d.id);
				if (sys == null)
					return;
				var binding = new SystemBinding().system(sys);
				addSectionOf(binding);
			});
		}
	}

	private static class VarBindingLabelProvider extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof VarBinding vb))
				return "";
			return switch (col) {
				case 0 -> vb.varId() != null
					? vb.varId().label()
					: "";
				case 1 -> vb.parameter() != null
					? vb.parameter().name
					: "";
				default -> "";
			};
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.FORMULA.get();
		}
	}
}

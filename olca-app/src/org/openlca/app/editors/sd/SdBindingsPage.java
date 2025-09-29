package org.openlca.app.editors.sd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.interop.SimulationSetup;
import org.openlca.app.editors.sd.interop.SystemBinding;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

class SdBindingsPage extends FormPage {

	private final IDatabase db;
	private final SdModelEditor editor;
	private final SimulationSetup setup;

	private final List<BindingSection> sections = new ArrayList<>();
	private ScrolledForm form;
	private FormToolkit tk;
	private Composite body;
	private AddButton addButton;

	SdBindingsPage(SdModelEditor editor) {
		super(editor, "SdBindingsPage", "Bindings");
		this.editor = editor;
		this.setup = editor.setup();
		this.db = Database.get();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		form = UI.header(mForm, "System bindings: " + editor.modelName());
		tk = mForm.getToolkit();
		body = UI.body(form, tk);
		createSetupSection();
		createBindingSections();
		form.reflow(true);
	}

	private void createSetupSection() {
		var comp = UI.formSection(body, tk, "Calculation setup");
		UI.gridLayout(comp, 2);

		// select impact method
		UI.label(comp, tk, M.ImpactAssessmentMethod);
		var combo = new ImpactMethodViewer(comp);
		var methods = new ImpactMethodDao(Database.get())
				.getDescriptors()
				.stream()
				.sorted((m1, m2) -> Strings.compare(m1.name, m2.name))
				.toList();
		combo.setInput(methods);

		if (setup.method() != null) {
			combo.select(Descriptor.of(setup.method()));
		}
		combo.addSelectionChangedListener(d -> {
			if (d == null)
				return;
			setup.method(db.get(ImpactMethod.class, d.id));
			editor.setDirty();
		});
	}

	private void createBindingSections() {
		var bindings = setup.systemBindings();
		for (int i = 0; i < bindings.size(); i++) {
			sections.add(new BindingSection(i, bindings.get(i)));
		}
		addButton = new AddButton();
	}

	private void addNew(SystemBinding binding) {
		if (binding == null)
			return;
		var pos = setup.systemBindings().size();
		setup.systemBindings().add(binding);
		sections.add(new BindingSection(pos, binding));
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
				sections.remove(this);
				section.dispose();
				form.reflow(true);
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
			var unitSymbol = unit != null && Strings.notEmpty(unit.name)
					? unit.name
					: "?";
			var amountText = UI.labeledText(comp, tk, M.Amount);
			amountText.setText(Double.toString(binding.amount()));
			UI.label(comp, tk, unitSymbol);

			amountText.addModifyListener(e -> {
				try {
					double v = Double.parseDouble(amountText.getText());
					binding.amount(v);
				} catch (Exception ignored) {
				}
			});
		}

		private void createParamTable(Composite parent) {
			var comp = UI.formSection(parent, tk, "Parameter bindings");

			var table = Tables.createViewer(comp, "Model variable", "Parameter");
			Tables.bindColumnWidths(table, 0.5, 0.5);

			// Add actions for parameter bindings
			var addParam = Actions.create("Add parameter binding", Icon.ADD.descriptor(), () -> {
			  SdVarBindingDialog.create(editor.xmile(), binding);
			});
			var deleteParam = Actions.create("Delete parameter binding", Icon.DELETE.descriptor(), () -> {
				// TODO: Implement parameter binding deletion
			});

			Actions.bind(table, addParam, deleteParam);

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
				addNew(binding);
			});
		}
	}
}

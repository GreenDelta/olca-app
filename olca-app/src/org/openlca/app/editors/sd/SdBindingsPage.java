package org.openlca.app.editors.sd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.interop.SimulationSetup;
import org.openlca.app.editors.sd.interop.SystemBinding;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyFactorViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.UnitCombo;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

class SdBindingsPage extends FormPage {

	private final SdModelEditor editor;
	private final List<SystemBindingSection> bindingSections = new ArrayList<>();
	private ScrolledForm form;
	private FormToolkit tk;
	private Composite body;

	SdBindingsPage(SdModelEditor editor) {
		super(editor, "SdBindingsPage", "Bindings");
		this.editor = editor;
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
		var methodCombo = new ImpactMethodViewer(comp);
		var methods = new ImpactMethodDao(Database.get())
				.getDescriptors()
				.stream()
				.sorted((m1, m2) -> Strings.compare(m1.name, m2.name))
				.toList();
		methodCombo.setInput(methods);

	}

	private void createBindingSections() {

		var setup = getSimulationSetup();
		var bindings = setup.systemBindings();
		for (int i = 0; i < bindings.size(); i++) {
			bindingSections.add(new SystemBindingSection(i, bindings.get(i)));
		}

		// TODO: add AddButton
	}

	private SimulationSetup getSimulationSetup() {
		// TODO ...
		return new SimulationSetup();
	}

	private class SystemBindingSection {

		private final int index;
		private final SystemBinding binding;

		SystemBindingSection(int index, SystemBinding binding) {
			this.index = index;
			this.binding = binding;
			createSection();
		}

		private void createSection() {
			var sectionName = binding.system() != null
					? binding.system().name
					: "System binding " + (index + 1);

			var section = UI.section(body, tk, sectionName);
			UI.gridData(section, true, false);

			var deleteAction = Actions.create("Delete", Icon.DELETE.descriptor(), () -> {
				getSimulationSetup().systemBindings().remove(binding);
				bindingSections.remove(this);
				section.dispose();
				form.reflow(true);
			});
			Actions.bind(section, deleteAction);

			var comp = UI.sectionClient(section, tk);
			createQuantitativeReferenceSection(comp);
			createParameterBindingsTable(comp);
		}

		private void createQuantitativeReferenceSection(Composite parent) {
			var comp = UI.formSection(parent, tk, "Quantitative reference", 2);

			// Product system selection
			ModelLink.of(ProductSystem.class)
					.setModel(binding.system());

			// TODO: Reference flow/product


			// Flow property
			UI.label(comp, tk, M.FlowProperty);
			var propertyCombo = new FlowPropertyFactorViewer(comp);
			propertyCombo.addSelectionChangedListener(factor -> {
				if (factor != null) {
					binding.property(factor.flowProperty);
					// TODO: Update unit options
				}
			});
			UI.filler(comp, tk);

			// Unit
			UI.label(comp, tk, M.Unit);
			var unitCombo = new UnitCombo(comp);
			unitCombo.addSelectionChangedListener(binding::unit);
			UI.filler(comp, tk);

			// Amount
			var amountText = UI.labeledText(comp, tk, "Amount");
			if (binding.amount() != null) {
				amountText.setText(binding.amount().toString());
			}
			// TODO: Add proper binding for amount
			UI.filler(comp, tk);
		}

		private void createParameterBindingsTable(Composite parent) {
			var comp = UI.formSection(parent, tk, "Parameter bindings");

			var table = Tables.createViewer(comp,
					"Parameter",
					"Value",
					"Description");
			UI.gridData(table.getControl(), true, true).minimumHeight = 150;

			Tables.bindColumnWidths(table, 0.4, 0.3, 0.3);

			// TODO: Implement table content provider and label provider
			// TODO: Add actions for adding/editing/removing parameter bindings


			// Add actions for parameter bindings
			var addParam = Actions.create("Add parameter binding", Icon.ADD.descriptor(), () -> {
				// TODO: Implement parameter binding creation dialog
			});

			var editParam = Actions.create("Edit parameter binding", Icon.EDIT.descriptor(), () -> {
				// TODO: Implement parameter binding edit dialog
			});

			var deleteParam = Actions.create("Delete parameter binding", Icon.DELETE.descriptor(), () -> {
				// TODO: Implement parameter binding deletion
			});

			Actions.bind(table, addParam, editParam, deleteParam);

		}
	}
}

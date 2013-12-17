package org.openlca.app.projects;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProjectSetupPage extends ModelPage<Project> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProjectEditor editor;
	private IDatabase database = Database.get();

	private Project project;
	private List<ProjectVariant> variants;
	private TableViewer variantViewer;
	private ScrolledForm form;
	private ProjectParameterTable parameterTable;

	ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", "Project setup");
		this.editor = editor;
		project = editor.getModel();
		variants = project.getVariants();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Project + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createSettingsSection(infoSection.getContainer());
		createVariantsSection(body);
		createParameterSection(body);
		initialInput();

		body.setFocus();
		form.reflow(true);
	}

	private void initialInput() {
		Collections.sort(variants, new Comparator<ProjectVariant>() {
			@Override
			public int compare(ProjectVariant v1, ProjectVariant v2) {
				return Strings.compare(v1.getName(), v2.getName());
			}
		});
		variantViewer.setInput(variants);
	}

	private void createSettingsSection(Composite composite) {
		UI.formLabel(composite, toolkit, "LCIA Method");
		ImpactMethodViewer impactMethodViewer = new ImpactMethodViewer(
				composite);
		impactMethodViewer.setNullable(true);
		impactMethodViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ImpactMethodDescriptor>() {
					@Override
					public void selectionChanged(
							ImpactMethodDescriptor selection) {
						handleMethodChange(selection);
					}
				});
		impactMethodViewer.setInput(database);
		if (project.getImpactMethodId() != null) {
			ImpactMethodDescriptor d = Cache.getEntityCache().get(
					ImpactMethodDescriptor.class, project.getImpactMethodId());
			impactMethodViewer.select(d);
		}
		// TODO: add nw-sets
		// UI.formLabel(client, toolkit, "Normalisation and Weighting");
		// new NormalizationWeightingSetViewer(client);
		createCalculationButton(composite);
	}

	private void createCalculationButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, Messages.Calculate,
				SWT.NONE);
		button.setImage(ImageType.CALCULATE_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Calculation.run(getModel());
			}
		});
	}

	private void handleMethodChange(ImpactMethodDescriptor selection) {
		if (selection == null && project.getImpactMethodId() == null)
			return;
		if (selection != null
				&& Objects.equals(selection.getId(),
						project.getImpactMethodId()))
			return;
		project.setNwSetId(null);
		if (selection == null)
			project.setImpactMethodId(null);
		else
			project.setImpactMethodId(selection.getId());
		editor.setDirty(true);
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, "Variants");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Name, Messages.ProductSystem,
				Messages.AllocationMethod, Messages.Flow, Messages.Amount,
				Messages.Unit };
		variantViewer = Tables.createViewer(composite, properties);
		variantViewer.setLabelProvider(new VariantLabelProvider());
		Tables.bindColumnWidths(variantViewer, 0.2, 0.2, 0.2, 0.2, 0.1, 0.1);
		ModifySupport<ProjectVariant> support = new ModifySupport<>(
				variantViewer);
		support.bind(Messages.Name, new VariantNameEditor());
		support.bind(Messages.AllocationMethod, new VariantAllocationEditor());
		support.bind(Messages.Amount, new VariantAmountEditor());
		support.bind(Messages.Unit, new VariantUnitEditor());
		addVariantActions(variantViewer, section);
		UI.gridData(variantViewer.getTable(), true, true).minimumHeight = 150;
	}

	private void createParameterSection(Composite body) {
		Section section = UI.section(body, toolkit, "Parameters");
		parameterTable = new ProjectParameterTable(editor);
		parameterTable.render(section, toolkit);
	}

	private void addVariantActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(new Runnable() {
			public void run() {
				addVariant();
			}
		});
		Action remove = Actions.onRemove(new Runnable() {
			public void run() {
				removeVariant();
			}
		});
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove);
	}

	private void addVariant() {
		log.trace("add variabt");
		BaseDescriptor d = ModelSelectionDialog
				.select(ModelType.PRODUCT_SYSTEM);
		if (d == null)
			return;
		ProductSystemDao dao = new ProductSystemDao(database);
		ProductSystem system = dao.getForId(d.getId());
		if (system == null) {
			log.error("failed to load product system");
			return;
		}
		ProjectVariant variant = createVariant(system);
		variants.add(variant);
		variantViewer.setInput(variants);
		editor.setDirty(true);
		parameterTable.addVariant(variant);
		form.reflow(true);
	}

	private ProjectVariant createVariant(ProductSystem system) {
		ProjectVariant variant = new ProjectVariant();
		variant.setProductSystem(system);
		variant.setName("Variant " + (variants.size() + 1));
		variant.setAllocationMethod(AllocationMethod.NONE);
		variant.setAmount(system.getTargetAmount());
		variant.setFlowPropertyFactor(system.getTargetFlowPropertyFactor());
		variant.setUnit(system.getTargetUnit());
		for (ParameterRedef redef : system.getParameterRedefs())
			variant.getParameterRedefs().add(redef.clone());
		return variant;
	}

	private void removeVariant() {
		log.trace("remove variant");
		List<ProjectVariant> selection = Viewers.getAllSelected(variantViewer);
		if (selection == null || selection.isEmpty())
			return;
		for (ProjectVariant var : selection) {
			variants.remove(var);
			parameterTable.removeVariant(var);
		}
		variantViewer.setInput(variants);
		editor.setDirty(true);
		form.reflow(true);
	}

	private class VariantNameEditor extends TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variant.getName();
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			if (Objects.equals(text, variant.getName()))
				return;
			variant.setName(text);
			parameterTable.updateVariant(variant);
			editor.setDirty(true);
		}
	}

	private class VariantAmountEditor extends TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return Double.toString(variant.getAmount());
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			try {
				double val = Double.parseDouble(text);
				variant.setAmount(val);
				editor.setDirty(true);
			} catch (Exception e) {
				Error.showBox("Invalid number", text + " is not a valid number");
			}
		}
	}

	private class VariantAllocationEditor extends
			ComboBoxCellModifier<ProjectVariant, AllocationMethod> {
		@Override
		protected AllocationMethod getItem(ProjectVariant var) {
			return var.getAllocationMethod() != null ? var
					.getAllocationMethod() : AllocationMethod.NONE;
		}

		@Override
		protected AllocationMethod[] getItems(ProjectVariant element) {
			return AllocationMethod.values();
		}

		@Override
		protected String getText(AllocationMethod value) {
			return Labels.getEnumText(value);
		}

		@Override
		protected void setItem(ProjectVariant var, AllocationMethod item) {
			var.setAllocationMethod(item);
			editor.setDirty(true);
		}
	}

	private class VariantUnitEditor extends
			ComboBoxCellModifier<ProjectVariant, Unit> {
		@Override
		protected Unit getItem(ProjectVariant var) {
			return var.getUnit();
		}

		@Override
		protected Unit[] getItems(ProjectVariant var) {
			FlowPropertyFactor fac = var.getFlowPropertyFactor();
			if (fac == null || fac.getFlowProperty() == null
					|| fac.getFlowProperty().getUnitGroup() == null)
				return new Unit[0];
			UnitGroup unitGroup = fac.getFlowProperty().getUnitGroup();
			Unit[] units = unitGroup.getUnits().toArray(
					new Unit[unitGroup.getUnits().size()]);
			Arrays.sort(units, new Comparator<Unit>() {
				@Override
				public int compare(Unit u1, Unit u2) {
					if (u1 == null || u2 == null)
						return 0;
					return Strings.compare(u1.getName(), u2.getName());
				}
			});
			return units;
		}

		@Override
		protected String getText(Unit unit) {
			if (unit == null)
				return "";
			return unit.getName();
		}

		@Override
		protected void setItem(ProjectVariant var, Unit unit) {
			var.setUnit(unit);
			editor.setDirty(true);
		}
	}

	private class VariantLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ProjectVariant))
				return null;
			ProjectVariant variant = (ProjectVariant) element;
			ProductSystem system = variant.getProductSystem();
			if (system == null)
				return null;
			switch (columnIndex) {
			case 0:
				return variant.getName();
			case 1:
				return system.getName();
			case 2:
				return Labels.getEnumText(variant.getAllocationMethod());
			case 3:
				return getFlowText(system);
			case 4:
				return Double.toString(variant.getAmount());
			case 5:
				Unit unit = variant.getUnit();
				return unit == null ? null : unit.getName();
			default:
				return null;
			}
		}

		private String getFlowText(ProductSystem system) {
			if (system == null || system.getReferenceExchange() == null)
				return null;
			Exchange refExchange = system.getReferenceExchange();
			return Labels.getDisplayName(refExchange.getFlow());
		}

	}

}

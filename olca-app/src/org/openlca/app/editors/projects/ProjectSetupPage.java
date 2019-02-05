package org.openlca.app.editors.projects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.editors.reports.Reports;
import org.openlca.app.editors.reports.model.ReportCalculator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProjectSetupPage extends ModelPage<Project> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProjectEditor editor;
	private IDatabase database = Database.get();

	private Project project;
	private TableViewer variantViewer;
	private ProjectParameterTable parameterTable;
	private ReportVariantSync variantSync;
	private ScrolledForm form;

	ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", M.ProjectSetup);
		this.editor = editor;
		project = editor.getModel();
		variantSync = new ReportVariantSync(editor);
		editor.onSaved(() -> {
			project = editor.getModel();
			if (variantViewer != null)
				variantViewer.setInput(project.variants);
		});
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createButton(infoSection.getContainer());
		new ImpactSection(editor).render(body, toolkit);
		createVariantsSection(body);
		Section section = UI.section(body, toolkit, M.Parameters);
		parameterTable = new ProjectParameterTable(editor);
		parameterTable.render(section, toolkit);
		initialInput();
		new ProcessContributionSection(editor).create(body, toolkit);
		body.setFocus();
		form.reflow(true);
	}

	private void initialInput() {
		List<ProjectVariant> variants = project.variants;
		Collections.sort(variants, (v1, v2) -> Strings.compare(v1.name, v2.name));
		variantViewer.setInput(variants);
	}

	private void createButton(Composite comp) {
		toolkit.createLabel(comp, "");
		Composite c = toolkit.createComposite(comp);
		UI.gridLayout(c, 2).marginHeight = 5;
		Button b = toolkit.createButton(c, M.Report, SWT.NONE);
		UI.gridData(b, false, false).widthHint = 100;
		b.setImage(Images.get(ModelType.PROJECT));
		Controls.onSelect(b, e -> {
			ReportCalculator calc = new ReportCalculator(
					getModel(), editor.getReport());
			App.runWithProgress(M.Calculate, calc, () -> {
				Reports.save(getModel(), editor.getReport(), database);
				ReportViewer.open(editor.getReport());
			});
		});
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, M.Variants);
		Composite comp = UI.sectionClient(section, toolkit, 1);
		variantViewer = Tables.createViewer(comp,
				M.Name, M.ProductSystem, M.Display,
				M.AllocationMethod, M.Flow, M.Amount,
				M.Unit, M.Description, "");
		variantViewer.setLabelProvider(new VariantLabelProvider());
		ModifySupport<ProjectVariant> ms = new ModifySupport<>(variantViewer);
		ms.bind(M.Name, new VariantNameEditor());
		ms.bind(M.Display, new DisplayModifier());
		ms.bind(M.AllocationMethod, new VariantAllocationEditor());
		ms.bind(M.Amount, new DoubleModifier<>(editor, "amount"));
		ms.bind(M.Unit, new VariantUnitEditor());
		ms.bind(M.Description, new VariantDescriptionEditor());
		ms.bind("", new CommentDialogModifier<ProjectVariant>(
				editor.getComments(), v -> CommentPaths.get(v)));
		double w = 1.0 / 8.0;
		Tables.bindColumnWidths(variantViewer, w, w, w, w, w, w, w, w);
		addVariantActions(variantViewer, section);
	}

	private void addVariantActions(TableViewer table, Section section) {
		Action onOpen = Actions.onOpen(() -> {
			ProjectVariant v = Viewers.getFirstSelected(table);
			if (v != null) {
				App.openEditor(v.productSystem);
			}
		});
		Action add = Actions.onAdd(() -> {
			BaseDescriptor[] ds = ModelSelectionDialog.multiSelect(
					ModelType.PRODUCT_SYSTEM);
			addVariants(ds);
		});
		Action remove = Actions.onRemove(this::removeVariant);
		Action copy = TableClipboard.onCopy(table);
		CommentAction.bindTo(section, "variants",
				editor.getComments(), add, remove);
		Actions.bind(table, onOpen, add, remove, copy);
		Tables.onDoubleClick(table, (event) -> {
			TableItem item = Tables.getItem(table, event);
			if (item == null) {
				add.run();
			} else {
				onOpen.run();
			}
		});
		Tables.onDrop(table, descriptors -> {
			if (descriptors != null) {
				addVariants(descriptors.toArray(
						new BaseDescriptor[descriptors.size()]));
			}
		});
	}

	private void addVariants(BaseDescriptor[] descriptors) {
		if (descriptors == null || descriptors.length == 0)
			return;
		for (BaseDescriptor d : descriptors) {
			if (d == null)
				continue;
			ProductSystemDao dao = new ProductSystemDao(database);
			ProductSystem system = dao.getForId(d.id);
			if (system == null) {
				log.error("failed to load product system " + d);
				continue;
			}
			List<ProjectVariant> variants = project.variants;
			ProjectVariant variant = createVariant(
					system, variants.size() + 1);
			variants.add(variant);
			variantViewer.setInput(variants);
			parameterTable.addVariant(variant);
			variantSync.variantAdded(variant);
		}
		editor.setDirty(true);
	}

	private ProjectVariant createVariant(ProductSystem system, int i) {
		ProjectVariant v = new ProjectVariant();
		v.productSystem = system;
		v.name = M.Variant + i;
		v.allocationMethod = AllocationMethod.NONE;
		v.amount = system.targetAmount;
		v.flowPropertyFactor = system.targetFlowPropertyFactor;
		v.unit = system.targetUnit;
		for (ParameterRedef redef : system.parameterRedefs) {
			v.parameterRedefs.add(redef.clone());
		}
		return v;
	}

	private void removeVariant() {
		log.trace("remove variant");
		List<ProjectVariant> selection = Viewers.getAllSelected(variantViewer);
		if (selection == null || selection.isEmpty())
			return;
		List<ProjectVariant> variants = project.variants;
		for (ProjectVariant var : selection) {
			variants.remove(var);
			parameterTable.removeVariant(var);
		}
		variantViewer.setInput(variants);
		variantSync.variantsRemoved(selection);
		editor.setDirty(true);
	}

	private class VariantNameEditor extends TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variant.name;
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			if (Objects.equals(text, variant.name))
				return;
			variantSync.updateName(variant, text);
			variant.name = text;
			parameterTable.updateVariant(variant);
			editor.setDirty(true);
		}
	}

	private class VariantDescriptionEditor extends
			TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variantSync.getDescription(variant);
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			String oldText = variantSync.getDescription(variant);
			if (Objects.equals(text, oldText))
				return;
			variantSync.updateDescription(variant, text);
			editor.setDirty(true);
		}
	}

	private class VariantAllocationEditor extends
			ComboBoxCellModifier<ProjectVariant, AllocationMethod> {
		@Override
		protected AllocationMethod getItem(ProjectVariant var) {
			return var.allocationMethod != null
					? var.allocationMethod
					: AllocationMethod.NONE;
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
			var.allocationMethod = item;
			editor.setDirty(true);
		}
	}

	private class VariantUnitEditor extends
			ComboBoxCellModifier<ProjectVariant, Unit> {
		@Override
		protected Unit getItem(ProjectVariant var) {
			return var.unit;
		}

		@Override
		protected Unit[] getItems(ProjectVariant var) {
			FlowPropertyFactor fac = var.flowPropertyFactor;
			if (fac == null || fac.flowProperty == null
					|| fac.flowProperty.unitGroup == null)
				return new Unit[0];
			UnitGroup unitGroup = fac.flowProperty.unitGroup;
			Unit[] units = unitGroup.units.toArray(
					new Unit[unitGroup.units.size()]);
			Arrays.sort(units, (u1, u2) -> {
				if (u1 == null || u2 == null)
					return 0;
				return Strings.compare(u1.name, u2.name);
			});
			return units;
		}

		@Override
		protected String getText(Unit unit) {
			if (unit == null)
				return "";
			return unit.name;
		}

		@Override
		protected void setItem(ProjectVariant var, Unit unit) {
			var.unit = unit;
			editor.setDirty(true);
		}
	}

	private class DisplayModifier extends CheckBoxCellModifier<ProjectVariant> {
		@Override
		protected boolean isChecked(ProjectVariant v) {
			return !v.isDisabled;
		}

		@Override
		protected void setChecked(ProjectVariant v, boolean b) {
			if (v.isDisabled != b)
				return;
			v.isDisabled = !b;
			variantSync.updateDisabled(v);
			editor.setDirty(true);
		}
	}

	private class VariantLabelProvider extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProjectVariant))
				return null;
			ProjectVariant v = (ProjectVariant) obj;
			switch (col) {
			case 1:
				return Images.get(ModelType.PRODUCT_SYSTEM);
			case 2:
				return v.isDisabled
						? Icon.CHECK_FALSE.get()
						: Icon.CHECK_TRUE.get();
			case 4:
				return Images.get(FlowType.PRODUCT_FLOW);
			case 6:
				return Images.get(ModelType.UNIT);
			case 8:
				return Images.get(editor.getComments(), CommentPaths.get(v));
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProjectVariant))
				return null;
			ProjectVariant variant = (ProjectVariant) obj;
			ProductSystem system = variant.productSystem;
			if (system == null)
				return null;
			switch (col) {
			case 0:
				return variant.name;
			case 1:
				return system.name;
			case 3:
				return Labels.getEnumText(variant.allocationMethod);
			case 4:
				return getFlowText(system);
			case 5:
				return Double.toString(variant.amount);
			case 6:
				Unit unit = variant.unit;
				return unit == null ? null : unit.name;
			case 7:
				return variantSync.getDescription(variant);
			default:
				return null;
			}
		}

		private String getFlowText(ProductSystem system) {
			if (system == null || system.referenceExchange == null)
				return null;
			Exchange refExchange = system.referenceExchange;
			return Labels.getDisplayName(refExchange.flow);
		}
	}
}

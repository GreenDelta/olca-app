package org.openlca.app.editors.projects;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.app.viewers.tables.modify.field.DoubleModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

class ProjectSetupPage extends ModelPage<Project> {

	private FormToolkit toolkit;
	private final ProjectEditor editor;
	private final IDatabase database = Database.get();

	private Project project;
	private TableViewer variantViewer;
	private ProjectParameterTable parameterTable;

	ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", M.ProjectSetup);
		this.editor = editor;
		project = editor.getModel();
		editor.onSaved(() -> {
			project = editor.getModel();
			if (variantViewer != null) {
				variantViewer.setInput(project.variants);
			}
		});
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createCalculationButton(infoSection.composite());
		new CalculationSetupSection(editor).render(body, toolkit);
		createVariantsSection(body);
		Section section = UI.section(body, toolkit, M.Parameters);
		parameterTable = new ProjectParameterTable(editor);
		parameterTable.render(section, toolkit);
		initialInput();
		body.setFocus();
		form.reflow(true);
	}

	private void initialInput() {
		List<ProjectVariant> variants = project.variants;
		variants.sort((v1, v2) -> Strings.compare(v1.name, v2.name));
		variantViewer.setInput(variants);
	}

	private void createCalculationButton(Composite parent) {
		UI.filler(parent, toolkit);
		var comp = toolkit.createComposite(parent);
		UI.gridLayout(comp, 1, 0, 0).marginHeight = 5;
		var button = toolkit.createButton(comp, M.Calculate, SWT.NONE);
		UI.gridData(button, false, false).widthHint = 100;
		button.setImage(Images.get(ModelType.PROJECT));
		Controls.onSelect(button,
			e -> ProjectEditorToolBar.calculate(editor));
	}

	private void createVariantsSection(Composite body) {
		var section = UI.section(body, toolkit, M.Variants);
		var comp = UI.sectionClient(section, toolkit, 1);
		variantViewer = Tables.createViewer(comp,
			M.Name, M.ProductSystem, M.Display,
			M.AllocationMethod, M.Flow, M.Amount,
			M.Unit, M.Description, "");
		variantViewer.setLabelProvider(new VariantLabelProvider());
		new ModifySupport<ProjectVariant>(variantViewer)
			.bind(M.Name, new VariantNameEditor())
			.bind(M.Display, new DisplayModifier())
			.bind(M.AllocationMethod, new VariantAllocationEditor())
			.bind(M.Amount, new DoubleModifier<>(editor, "amount"))
			.bind(M.Unit, new VariantUnitEditor())
			.bind(M.Description, new VariantDescriptionEditor())
			.bind("", new CommentDialogModifier<>(
				editor.getComments(), CommentPaths::get));
		double w = 1.0 / 8.1;
		Tables.bindColumnWidths(variantViewer, w, w, w, w, w, w, w, w);
		addVariantActions(variantViewer, section);
	}

	private void addVariantActions(TableViewer table, Section section) {
		Action onOpen = Actions.onOpen(() -> {
			ProjectVariant v = Viewers.getFirstSelected(table);
			if (v != null) {
				App.open(v.productSystem);
			}
		});
		Action add = Actions.onAdd(() -> {
			var ds = ModelSelector.multiSelect(ModelType.PRODUCT_SYSTEM);
			addVariants(ds);
		});
		Action remove = Actions.onRemove(this::removeVariant);
		Action copy = TableClipboard.onCopySelected(table);
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
		ModelTransfer.onDrop(table.getTable(), this::addVariants);
	}

	private void addVariants(List<? extends Descriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (var d : descriptors) {
			if (d == null || d.type != ModelType.PRODUCT_SYSTEM)
				continue;
			ProductSystemDao dao = new ProductSystemDao(database);
			ProductSystem system = dao.getForId(d.id);
			if (system == null) {
				continue;
			}
			List<ProjectVariant> variants = project.variants;
			ProjectVariant variant = createVariant(
				system, variants.size() + 1);
			variants.add(variant);
			variantViewer.setInput(variants);
			parameterTable.addVariant(variant);
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
		var redefSet = system.parameterSets.stream()
			.filter(s -> s.isBaseline)
			.findAny();
		if (redefSet.isPresent()) {
			for (var redef : redefSet.get().parameters) {
				v.parameterRedefs.add(redef.copy());
			}
		}
		return v;
	}

	private void removeVariant() {
		List<ProjectVariant> selection = Viewers.getAllSelected(variantViewer);
		if (selection.isEmpty())
			return;
		List<ProjectVariant> variants = project.variants;
		for (ProjectVariant var : selection) {
			variants.remove(var);
			parameterTable.removeVariant(var);
		}
		variantViewer.setInput(variants);
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
			variant.name = text;
			parameterTable.updateVariant(variant);
			editor.setDirty(true);
		}
	}

	private class VariantDescriptionEditor extends
		TextCellModifier<ProjectVariant> {

		@Override
		protected String getText(ProjectVariant variant) {
			return variant.description;
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			if (Objects.equals(text, variant.description))
				return;
			variant.description = text;
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
			return Labels.of(value);
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
			Unit[] units = unitGroup.units.toArray(new Unit[0]);
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
			editor.setDirty(true);
		}
	}

	private class VariantLabelProvider extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProjectVariant))
				return null;
			var v = (ProjectVariant) obj;
			return switch (col) {
				case 1 -> Images.get(ModelType.PRODUCT_SYSTEM);
				case 2 -> v.isDisabled
					? Icon.CHECK_FALSE.get()
					: Icon.CHECK_TRUE.get();
				case 4 -> Images.get(FlowType.PRODUCT_FLOW);
				case 6 -> Images.get(ModelType.UNIT);
				case 8 -> Images.get(editor.getComments(), CommentPaths.get(v));
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProjectVariant))
				return null;
			var variant = (ProjectVariant) obj;
			var system = variant.productSystem;
			return switch (col) {
				case 0 -> variant.name;
				case 1 -> system != null ? system.name : null;
				case 3 -> Labels.of(variant.allocationMethod);
				case 4 -> system != null && system.referenceExchange != null
					? Labels.name(system.referenceExchange.flow)
					: null;
				case 5 -> Double.toString(variant.amount);
				case 6 -> variant.unit == null ? null : variant.unit.name;
				case 7 -> variant.description;
				default -> null;
			};
		}
	}
}

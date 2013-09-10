package org.openlca.app.projects;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NormalizationWeightingSetViewer;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectSetupPage extends ModelPage<Project> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProjectEditor editor;

	private IDatabase database = Database.get();

	private List<ProjectVariant> variants;
	private TableViewer variantViewer;
	private Composite body;
	private ScrolledForm form;
	private ProjectParameterTable parameterTable;

	public ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", "Calculation setup");
		this.editor = editor;
		variants = editor.getModel().getVariants();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, Messages.Project + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		createSettingsSection(body);
		createVariantsSection(body);
		createParameterSection(body);
		initialInput();
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

	private void createSettingsSection(Composite body) {
		Composite client = UI.formSection(body, toolkit, "Settings");
		UI.formLabel(client, toolkit, "LCIA Method");
		new ImpactMethodViewer(client);
		UI.formLabel(client, toolkit, "Normalisation and Weighting");
		new NormalizationWeightingSetViewer(client);
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, "Variants");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Name, Messages.ProductSystem,
				Messages.FlowProperty, Messages.Amount, Messages.Unit };
		variantViewer = Tables.createViewer(composite, properties);
		variantViewer.setLabelProvider(new VariantLabelProvider());
		Tables.bindColumnWidths(variantViewer, 0.25, 0.25, 0.20, 0.15, 0.15);
		ModifySupport<ProjectVariant> support = new ModifySupport<>(
				variantViewer);
		support.bind(Messages.Name, new VariantNameEditor());
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
		BaseDescriptor d = ObjectDialog.select(ModelType.PRODUCT_SYSTEM);
		if (d == null)
			return;
		ProductSystemDao dao = new ProductSystemDao(database);
		ProductSystem system = dao.getForId(d.getId());
		if (system == null) {
			log.error("failed to load product system");
			return;
		}
		ProjectVariant variant = new ProjectVariant();
		variant.setProductSystem(system);
		variant.setName("Variant " + (variants.size() + 1));
		// TODO: parameter redefs
		for (ParameterRedef redef : system.getParameterRedefs())
			variant.getParameterRedefs().add(redef.clone());
		variants.add(variant);
		variantViewer.setInput(variants);
		editor.setDirty(true);
		parameterTable.addVariant(variant);
		form.reflow(true);
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
				FlowPropertyFactor fac = system.getTargetFlowPropertyFactor();
				FlowProperty prop = fac == null ? null : fac.getFlowProperty();
				return prop == null ? null : prop.getName();
			case 3:
				return Numbers.format(system.getTargetAmount());
			case 4:
				Unit unit = system.getTargetUnit();
				return unit == null ? null : unit.getName();
			default:
				return null;
			}
		}
	}

}

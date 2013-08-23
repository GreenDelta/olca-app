package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang3.tuple.Pair;
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
import org.openlca.app.util.Actions;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NormalizationWeightingSetViewer;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class ProjectSetupPage extends ModelPage<Project> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProjectEditor editor;

	private IDatabase database = Database.get();

	// TODO: live reference to the project - variant - list
	private List<ProjectVariant> variants = new ArrayList<>();
	private TableViewer variantViewer;
	private Composite body;
	private List<Pair<ProjectVariant, Section>> parameterSections = new ArrayList<>();
	private ScrolledForm form;

	public ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", "Calculation setup");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, Messages.Project + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		createSettingsSection(body);
		createVariantsSection(body);
		// TODO initial input
		form.reflow(true);
	}

	private void createSettingsSection(Composite body) {
		Composite client = UI.formSection(body, toolkit, "Settings");
		UI.formLabel(client, toolkit, "Allocation method");
		new AllocationMethodViewer(client);
		UI.formLabel(client, toolkit, "LCIA Method");
		new ImpactMethodViewer(client);
		UI.formLabel(client, toolkit, "Normalisation and Weighting");
		new NormalizationWeightingSetViewer(client);
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, "Variants");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Name, Messages.ProductSystem };
		variantViewer = Tables.createViewer(composite, properties);
		variantViewer.setLabelProvider(new VariantLabelProvider());
		Tables.bindColumnWidths(variantViewer, 0.5, 0.5);
		ModifySupport<ProjectVariant> support = new ModifySupport<>(
				variantViewer);
		support.bind(Messages.Name, new VariantNameEditor());
		addVariantActions(variantViewer, section);
		UI.gridData(variantViewer.getTable(), true, false).heightHint = 150;
	}

	private void addVariantActions(TableViewer viewer, Section section) {
		Action add = Actions.createAdd(new Runnable() {
			public void run() {
				addVariant();
			}
		});
		Action remove = Actions.createRemove(new Runnable() {
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
		variants.add(variant);
		variantViewer.setInput(variants);
		editor.setDirty(true);
		Section section = UI.section(body, toolkit, variant.getName());
		UI.gridData(section, true, false).heightHint = 120;
		parameterSections.add(Pair.of(variant, section));
		form.reflow(true);
	}

	private void removeVariant() {
		log.trace("remove variant");
		List<ProjectVariant> selection = Viewers.getAllSelected(variantViewer);
		if (selection == null || selection.isEmpty())
			return;
		for (ProjectVariant var : selection) {
			variants.remove(var);
			Section section = findParameterSection(var);
			if (section != null)
				section.dispose();
		}
		variantViewer.setInput(variants);
		editor.setDirty(true);
		form.reflow(true);
	}

	private Section findParameterSection(ProjectVariant variant) {
		for (Pair<ProjectVariant, Section> pair : parameterSections) {
			if (Objects.equal(variant, pair.getLeft()))
				return pair.getRight();
		}
		return null;
	}

	private class VariantNameEditor extends TextCellModifier<ProjectVariant> {
		@Override
		protected String getText(ProjectVariant variant) {
			return variant.getName();
		}

		@Override
		protected void setText(ProjectVariant variant, String text) {
			variant.setName(text);
			Section section = findParameterSection(variant);
			section.setText(text);
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
			if (columnIndex == 0)
				return variant.getName();
			else {
				if (variant.getProductSystem() != null)
					return variant.getProductSystem().getName();
				else
					return null;
			}
		}
	}

	// TODO: move this to the core model
	@Entity
	@Table(name = "tbl_project_variants")
	static class ProjectVariant extends AbstractEntity {

		private String name;
		private ProductSystem productSystem;
		private List<ParameterRedef> parameterRedefs = new ArrayList<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ProductSystem getProductSystem() {
			return productSystem;
		}

		public void setProductSystem(ProductSystem productSystem) {
			this.productSystem = productSystem;
		}

		public List<ParameterRedef> getParameterRedefs() {
			return parameterRedefs;
		}

	}

}

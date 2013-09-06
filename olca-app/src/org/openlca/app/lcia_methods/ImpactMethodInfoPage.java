package org.openlca.app.lcia_methods;

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
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;

class ImpactMethodInfoPage extends ModelPage<ImpactMethod> {

	private final String NAME = Messages.Name;
	private final String DESCRIPTION = Messages.Description;
	private final String REFERENCE_UNIT = Messages.ReferenceUnit;

	private TableViewer viewer;
	private FormToolkit toolkit;
	private ImpactMethodEditor editor;

	ImpactMethodInfoPage(ImpactMethodEditor editor) {
		super(editor, "ImpactMethodInfoPage", Messages.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ImpactMethod
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createImpactCategoryViewer(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createImpactCategoryViewer(Composite body) {
		Section section = UI.section(body, toolkit, Messages.ImpactCategories);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		String[] properties = { NAME, DESCRIPTION, REFERENCE_UNIT };
		viewer = Tables.createViewer(client, properties);
		viewer.setLabelProvider(new CategoryLabelProvider());
		viewer.setInput(editor.getModel().getImpactCategories());
		Tables.bindColumnWidths(viewer, 0.5, 0.25, 0.25);
		ModifySupport<ImpactCategory> support = new ModifySupport<>(viewer);
		support.bind(NAME, new NameModifier());
		support.bind(DESCRIPTION, new DescriptionModifier());
		support.bind(REFERENCE_UNIT, new ReferenceUnitModifier());
		bindActions(viewer, section);
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(new Runnable() {
			public void run() {
				onAdd();
			}
		});
		Action remove = Actions.onRemove(new Runnable() {
			public void run() {
				onRemove();
			}
		});
		Actions.bind(viewer, add, remove);
		Actions.bind(section, add, remove);
	}

	private void onAdd() {
		ImpactMethod method = editor.getModel();
		ImpactCategory category = new ImpactCategory();
		category.setName("New impact category");
		method.getImpactCategories().add(category);
		viewer.setInput(method.getImpactCategories());
		fireCategoryChange();
	}

	private void onRemove() {
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> categories = Viewers.getAllSelected(viewer);
		for (ImpactCategory category : categories) {
			method.getImpactCategories().remove(category);
		}
		viewer.setInput(method.getImpactCategories());
		fireCategoryChange();
	}

	private void fireCategoryChange() {
		editor.postEvent(editor.IMPACT_CATEGORY_CHANGE, this);
		editor.setDirty(true);
	}

	private class CategoryLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ImpactCategory))
				return null;
			ImpactCategory category = (ImpactCategory) element;
			switch (columnIndex) {
			case 0:
				return category.getName();
			case 1:
				return category.getDescription();
			case 2:
				return category.getReferenceUnit();
			default:
				return null;
			}
		}
	}

	private class NameModifier extends TextCellModifier<ImpactCategory> {

		@Override
		protected String getText(ImpactCategory element) {
			return element.getName();
		}

		@Override
		protected void setText(ImpactCategory element, String text) {
			if (!Objects.equals(text, element.getName())) {
				element.setName(text);
				fireCategoryChange();
			}
		}
	}

	private class DescriptionModifier extends TextCellModifier<ImpactCategory> {

		@Override
		protected String getText(ImpactCategory element) {
			return element.getDescription();
		}

		@Override
		protected void setText(ImpactCategory element, String text) {
			if (!Objects.equals(text, element.getDescription())) {
				element.setDescription(text);
				fireCategoryChange();
			}
		}
	}

	private class ReferenceUnitModifier extends
			TextCellModifier<ImpactCategory> {

		@Override
		protected String getText(ImpactCategory element) {
			return element.getReferenceUnit();
		}

		@Override
		protected void setText(ImpactCategory element, String text) {
			if (!Objects.equals(text, element.getReferenceUnit())) {
				element.setReferenceUnit(text);
				fireCategoryChange();
			}
		}
	}

}

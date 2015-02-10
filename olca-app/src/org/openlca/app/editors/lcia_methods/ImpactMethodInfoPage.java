package org.openlca.app.editors.lcia_methods;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.util.Strings;

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
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.ImpactAssessmentMethod
						+ ": " + getModel().getName());
		if (FeatureFlag.SHOW_REFRESH_BUTTONS.isEnabled())
			Editors.addRefresh(form, editor);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
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
		viewer.setInput(getCategories(true));
		Tables.bindColumnWidths(viewer, 0.5, 0.25, 0.25);
		bindModifySupport();
		bindActions(viewer, section);
		editor.onSaved(() -> viewer.setInput(getCategories(false)));
	}

	private void bindModifySupport() {
		ModifySupport<ImpactCategory> support = new ModifySupport<>(viewer);
		support.bind(NAME, ImpactCategory::getName,
				(category, text) -> {
					category.setName(text);
					fireCategoryChange();
				});
		support.bind(DESCRIPTION, ImpactCategory::getDescription,
				(category, text) -> {
					category.setDescription(text);
					fireCategoryChange();
				});
		support.bind(REFERENCE_UNIT, ImpactCategory::getReferenceUnit,
				(category, text) -> {
					category.setReferenceUnit(text);
					fireCategoryChange();
				});
	}

	private List<ImpactCategory> getCategories(boolean sorted) {
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> categories = method.getImpactCategories();
		if (!sorted)
			return categories;
		Collections.sort(categories,
				(c1, c2) -> Strings.compare(c1.getName(), c2.getName()));
		return categories;
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(viewer, add, remove, copy);
		Actions.bind(section, add, remove);
		Tables.onDeletePressed(viewer, (event) -> onRemove());
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null) {
				onAdd();
			}
		});
	}

	private void onAdd() {
		ImpactMethod method = editor.getModel();
		ImpactCategory category = new ImpactCategory();
		category.setRefId(UUID.randomUUID().toString());
		category.setName(Messages.NewImpactCategory);
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
}

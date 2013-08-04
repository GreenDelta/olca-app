package org.openlca.app.viewers.table;

import java.util.Objects;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;

public class ImpactCategoryViewer extends AbstractTableViewer<ImpactCategory> {

	public ImpactCategoryViewer(Composite parent) {
		super(parent);
		getCellModifySupport().support(LABEL.NAME, new NameModifier());
		getCellModifySupport().support(LABEL.DESCRIPTION,
				new DescriptionModifier());
		getCellModifySupport().support(LABEL.REFERENCE_UNIT,
				new ReferenceUnitModifier());
	}

	private interface LABEL {
		String NAME = Messages.Name;
		String DESCRIPTION = Messages.Description;
		String REFERENCE_UNIT = Messages.ReferenceUnit;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.NAME,
			LABEL.DESCRIPTION, LABEL.REFERENCE_UNIT };

	private ImpactMethod method;

	public void setInput(ImpactMethod impactMethod) {
		this.method = impactMethod;
		if (method == null)
			setInput(new ImpactCategory[0]);
		else
			setInput(impactMethod.getImpactCategories().toArray(
					new ImpactCategory[impactMethod.getImpactCategories()
							.size()]));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new CategoryLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnCreate
	protected void onCreate() {
		ImpactCategory category = new ImpactCategory();
		category.setName("newImpactCategory");
		fireModelChanged(ModelChangeType.CREATE, category);
		setInput(method);
	}

	@OnRemove
	protected void onRemove() {
		for (ImpactCategory category : getAllSelected()) {
			method.getImpactCategories().remove(category);
			fireModelChanged(ModelChangeType.REMOVE, category);
		}
		setInput(method);
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
				fireModelChanged(ModelChangeType.CHANGE, element);
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
				fireModelChanged(ModelChangeType.CHANGE, element);
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
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

}

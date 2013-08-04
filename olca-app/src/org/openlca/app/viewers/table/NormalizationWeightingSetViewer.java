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
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NormalizationWeightingSet;

public class NormalizationWeightingSetViewer extends
		AbstractTableViewer<NormalizationWeightingSet> {

	public NormalizationWeightingSetViewer(Composite parent) {
		super(parent);
		getCellModifySupport().support(LABEL.REFERENCE_SYSTEM,
				new ReferenceSystemModifier());
		getCellModifySupport().support(LABEL.UNIT, new UnitModifier());
	}

	private interface LABEL {
		String REFERENCE_SYSTEM = Messages.Common_NormalizationWeightingSet;
		String UNIT = Messages.Common_ReferenceUnit;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.REFERENCE_SYSTEM,
			LABEL.UNIT };

	private ImpactMethod method;

	public void setInput(ImpactMethod impactMethod) {
		this.method = impactMethod;
		if (method == null)
			setInput(new NormalizationWeightingSet[0]);
		else
			setInput(impactMethod.getNormalizationWeightingSets().toArray(
					new NormalizationWeightingSet[impactMethod
							.getNormalizationWeightingSets().size()]));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new SetLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnCreate
	protected void onCreate() {
		NormalizationWeightingSet set = new NormalizationWeightingSet("newSet",
				method);
		fireModelChanged(ModelChangeType.CREATE, set);
		setInput(method);
	}

	@OnRemove
	protected void onRemove() {
		for (NormalizationWeightingSet set : getAllSelected()) {
			method.getNormalizationWeightingSets().remove(set);
			fireModelChanged(ModelChangeType.REMOVE, set);
		}
		setInput(method);
	}

	private class SetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof NormalizationWeightingSet))
				return null;
			NormalizationWeightingSet set = (NormalizationWeightingSet) element;
			switch (columnIndex) {
			case 0:
				return set.getReferenceSystem();
			case 1:
				return set.getUnit();
			default:
				return null;
			}
		}

	}

	private class ReferenceSystemModifier extends
			TextCellModifier<NormalizationWeightingSet> {

		@Override
		protected String getText(NormalizationWeightingSet element) {
			return element.getReferenceSystem();
		}

		@Override
		protected void setText(NormalizationWeightingSet element, String text) {
			if (!Objects.equals(text, element.getReferenceSystem())) {
				element.setReferenceSystem(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}
	}

	private class UnitModifier extends
			TextCellModifier<NormalizationWeightingSet> {

		@Override
		protected String getText(NormalizationWeightingSet element) {
			return element.getUnit();
		}

		@Override
		protected void setText(NormalizationWeightingSet element, String text) {
			if (!Objects.equals(text, element.getUnit())) {
				element.setUnit(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

}

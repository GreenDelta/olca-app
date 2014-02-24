package org.openlca.app.editors.lcia_methods;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class NwSetViewer extends AbstractTableViewer<NwSet> {

	public NwSetViewer(Composite parent) {
		super(parent);
		getCellModifySupport().bind(LABEL.NAME, new NameModifier());
		getCellModifySupport().bind(LABEL.UNIT, new UnitModifier());
	}

	private interface LABEL {
		String NAME = Messages.NormalizationWeightingSet;
		String UNIT = Messages.ReferenceUnit;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.NAME, LABEL.UNIT };

	private ImpactMethod method;

	public void setInput(ImpactMethod impactMethod) {
		this.method = impactMethod;
		if (method == null)
			setInput(new NwSet[0]);
		else
			setInput(impactMethod.getNwSets().toArray(
					new NwSet[impactMethod.getNwSets().size()]));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new SetLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnAdd
	protected void onCreate() {
		NwSet set = new NwSet();
		set.setName("newSet");
		set.setRefId(UUID.randomUUID().toString());
		fireModelChanged(ModelChangeType.CREATE, set);
		setInput(method);
	}

	@OnRemove
	protected void onRemove() {
		for (NwSet set : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, set);
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
			if (!(element instanceof NwSet))
				return null;
			NwSet set = (NwSet) element;
			switch (columnIndex) {
			case 0:
				return set.getName();
			case 1:
				return set.getWeightedScoreUnit();
			default:
				return null;
			}
		}

	}

	private class NameModifier extends TextCellModifier<NwSet> {

		@Override
		protected String getText(NwSet element) {
			return element.getName();
		}

		@Override
		protected void setText(NwSet element, String text) {
			if (!Objects.equals(text, element.getName())) {
				element.setName(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}
	}

	private class UnitModifier extends TextCellModifier<NwSet> {

		@Override
		protected String getText(NwSet element) {
			return element.getWeightedScoreUnit();
		}

		@Override
		protected void setText(NwSet element, String text) {
			if (!Objects.equals(text, element.getWeightedScoreUnit())) {
				element.setWeightedScoreUnit(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

}

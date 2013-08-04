package org.openlca.app.viewers.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ImpactFactorViewer extends AbstractTableViewer<ImpactFactor> {

	private interface LABEL {
		String FLOW = Messages.Flow;
		String FLOW_PROPERTY = Messages.FlowProperty;
		String UNIT = Messages.Unit;
		String FACTOR = Messages.Factor;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.FLOW,
			LABEL.FLOW_PROPERTY, LABEL.UNIT, LABEL.FACTOR };

	private ImpactCategory category;
	private final FlowDao flowDao;

	public ImpactFactorViewer(Composite parent, IDatabase database) {
		super(parent);
		flowDao = new FlowDao(database);
		getCellModifySupport().support(LABEL.FLOW_PROPERTY,
				new FlowPropertyModifier());
		getCellModifySupport().support(LABEL.UNIT, new UnitModifier());
		getCellModifySupport().support(LABEL.FACTOR, new FactorModifier());
	}

	public void setInput(ImpactCategory impactCategory) {
		this.category = impactCategory;
		if (category == null)
			setInput(new ImpactFactor[0]);
		else
			setInput(impactCategory.getImpactFactors().toArray(
					new ImpactFactor[impactCategory.getImpactFactors().size()]));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FactorLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnCreate
	protected void onCreate() {
		if (category != null) {
			BaseDescriptor descriptor = ObjectDialog.select(ModelType.FLOW);
			if (descriptor != null)
				add(descriptor);
		}
	}

	private void add(BaseDescriptor descriptor) {
		ImpactFactor factor = new ImpactFactor();
		factor.setFlow(flowDao.getForId(descriptor.getId()));
		factor.setFlowPropertyFactor(factor.getFlow().getReferenceFactor());
		factor.setUnit(factor.getFlowPropertyFactor().getFlowProperty()
				.getUnitGroup().getReferenceUnit());
		fireModelChanged(ModelChangeType.CREATE, factor);
		setInput(category);
	}

	@OnRemove
	protected void onRemove() {
		if (category != null) {
			for (ImpactFactor factor : getAllSelected())
				fireModelChanged(ModelChangeType.REMOVE, factor);
			setInput(category);
		}
	}

	@OnDrop
	protected void onDrop(FlowDescriptor descriptor) {
		if (category != null)
			if (descriptor != null)
				add(descriptor);
	}

	private class FactorLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ImpactFactor))
				return null;
			ImpactFactor factor = (ImpactFactor) element;
			switch (columnIndex) {
			case 0:
				return factor.getFlow().getName();
			case 1:
				return factor.getFlowPropertyFactor().getFlowProperty()
						.getName();
			case 2:
				return factor.getUnit().getName();
			case 3:
				return Double.toString(factor.getValue());
			default:
				return null;
			}
		}

	}

	private class FlowPropertyModifier extends
			ComboBoxCellModifier<ImpactFactor, FlowProperty> {

		@Override
		protected FlowProperty[] getItems(ImpactFactor element) {
			List<FlowProperty> items = new ArrayList<>();
			for (FlowPropertyFactor factor : element.getFlow()
					.getFlowPropertyFactors())
				items.add(factor.getFlowProperty());
			return items.toArray(new FlowProperty[items.size()]);
		}

		@Override
		protected FlowProperty getItem(ImpactFactor element) {
			return element.getFlowPropertyFactor().getFlowProperty();
		}

		@Override
		protected String getText(FlowProperty value) {
			return value.getName();
		}

		@Override
		protected void setItem(ImpactFactor element, FlowProperty item) {
			if (!Objects.equals(item, element.getFlowPropertyFactor()
					.getFlowProperty())) {
				FlowPropertyFactor factor = element.getFlow().getFactor(item);
				element.setFlowPropertyFactor(factor);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}
	}

	private class UnitModifier extends ComboBoxCellModifier<ImpactFactor, Unit> {

		@Override
		protected Unit[] getItems(ImpactFactor element) {
			List<Unit> items = new ArrayList<>();
			for (Unit unit : element.getFlowPropertyFactor().getFlowProperty()
					.getUnitGroup().getUnits())
				items.add(unit);
			return items.toArray(new Unit[items.size()]);
		}

		@Override
		protected Unit getItem(ImpactFactor element) {
			return element.getUnit();
		}

		@Override
		protected String getText(Unit value) {
			return value.getName();
		}

		@Override
		protected void setItem(ImpactFactor element, Unit item) {
			if (!Objects.equals(item, element.getUnit())) {
				element.setUnit(item);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

	private class FactorModifier extends TextCellModifier<ImpactFactor> {

		@Override
		protected String getText(ImpactFactor element) {
			return Double.toString(element.getValue());
		}

		@Override
		protected void setText(ImpactFactor element, String text) {
			try {
				double value = Double.parseDouble(text);
				if (value != element.getValue()) {
					element.setValue(value);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {

			}
		}

	}

}

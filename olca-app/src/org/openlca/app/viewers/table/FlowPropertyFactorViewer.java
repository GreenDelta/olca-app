package org.openlca.app.viewers.table;

import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.Cache;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;

public class FlowPropertyFactorViewer extends
		AbstractTableViewer<FlowPropertyFactor> {

	private interface LABEL {
		String NAME = Messages.Name;
		String CONVERSION_FACTOR = Messages.ConversionFactor;
		String REFERENCE_UNIT = Messages.ReferenceUnit;
		String IS_REFERENCE = Messages.IsReference;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.NAME,
			LABEL.CONVERSION_FACTOR, LABEL.REFERENCE_UNIT, LABEL.IS_REFERENCE };

	private Flow flow;
	private Cache cache;

	public FlowPropertyFactorViewer(Composite parent, Cache cache) {
		super(parent);
		getCellModifySupport().bind(LABEL.CONVERSION_FACTOR,
				new ConversionFactorModifier());
		getCellModifySupport().bind(LABEL.IS_REFERENCE,
				new ReferenceModifier());
		this.cache = cache;
	}

	public void setInput(Flow flow) {
		this.flow = flow;
		if (flow == null)
			setInput(new FlowPropertyFactor[0]);
		else
			setInput(flow.getFlowPropertyFactors()
					.toArray(
							new FlowPropertyFactor[flow
									.getFlowPropertyFactors().size()]));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FactorLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ObjectDialog
				.multiSelect(ModelType.FLOW_PROPERTY);
		if (descriptors != null)
			for (BaseDescriptor descriptor : descriptors)
				add((FlowPropertyDescriptor) descriptor);
	}

	private void add(FlowPropertyDescriptor descriptor) {
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.setFlowProperty(cache.getFlowProperty(descriptor.getId()));
		fireModelChanged(ModelChangeType.CREATE, factor);
		setInput(flow);
	}

	@OnRemove
	protected void onRemove() {
		for (FlowPropertyFactor factor : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, factor);
		setInput(flow);
	}

	@OnDrop
	protected void onDrop(FlowPropertyDescriptor descriptor) {
		if (descriptor != null)
			add(descriptor);
	}

	private class FactorLabelProvider extends LabelProvider implements
			ITableLabelProvider, ITableFontProvider {

		private Font boldFont;

		@Override
		public void dispose() {
			if (boldFont != null && !boldFont.isDisposed())
				boldFont.dispose();
		}

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column == 0)
				return ImageType.FLOW_PROPERTY_ICON.get();
			if (column != 3)
				return null;
			FlowPropertyFactor refFactor = flow != null ? flow
					.getReferenceFactor() : null;
			if (refFactor != null && refFactor.equals(element))
				return ImageType.CHECK_TRUE.get();
			return ImageType.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FlowPropertyFactor))
				return null;
			FlowPropertyFactor factor = (FlowPropertyFactor) element;
			switch (columnIndex) {
			case 0:
				return factor.getFlowProperty().getName();
			case 1:
				return Numbers.format(factor.getConversionFactor());
			case 2:
				return factor.getFlowProperty().getUnitGroup()
						.getReferenceUnit().getName();
			default:
				return null;
			}
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			FlowPropertyFactor refFactor = flow != null ? flow
					.getReferenceFactor() : null;
			if (refFactor != null && refFactor.equals(element)) {
				if (boldFont == null)
					boldFont = UI.boldFont(getViewer().getTable());
				return boldFont;
			}
			return null;
		}
	}

	private class ConversionFactorModifier extends
			TextCellModifier<FlowPropertyFactor> {

		@Override
		protected String getText(FlowPropertyFactor element) {
			return Double.toString(element.getConversionFactor());
		}

		@Override
		protected void setText(FlowPropertyFactor element, String text) {
			try {
				double value = Double.parseDouble(text);
				if (value != element.getConversionFactor()) {
					element.setConversionFactor(value);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

	private class ReferenceModifier extends
			CheckBoxCellModifier<FlowPropertyFactor> {

		@Override
		protected boolean isChecked(FlowPropertyFactor element) {
			return flow != null
					&& Objects.equals(flow.getReferenceFactor(), element);
		}

		@Override
		protected void setChecked(FlowPropertyFactor element, boolean value) {
			if (value) {
				if (!Objects.equals(flow.getReferenceFlowProperty(),
						element.getFlowProperty())) {
					flow.setReferenceFlowProperty(element.getFlowProperty());
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			}
		}
	}

}

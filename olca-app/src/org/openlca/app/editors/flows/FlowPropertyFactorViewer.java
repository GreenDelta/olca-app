package org.openlca.app.editors.flows;

import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;

class FlowPropertyFactorViewer extends AbstractTableViewer<FlowPropertyFactor> {

	private interface LABEL {
		String NAME = Messages.Name;
		String CONVERSION_FACTOR = Messages.ConversionFactor;
		String REFERENCE_UNIT = Messages.ReferenceUnit;
		String FORMULA = Messages.Formula;
		String IS_REFERENCE = Messages.IsReference;
	}

	private static final String[] COLUMN_HEADERS = { LABEL.NAME,
			LABEL.CONVERSION_FACTOR, LABEL.REFERENCE_UNIT, LABEL.FORMULA,
			LABEL.IS_REFERENCE };

	private EntityCache cache;
	private FlowEditor editor;

	public FlowPropertyFactorViewer(Composite parent, EntityCache cache,
			FlowEditor editor) {
		super(parent);
		getCellModifySupport().bind(LABEL.CONVERSION_FACTOR,
				new ConversionFactorModifier());
		getCellModifySupport()
				.bind(LABEL.IS_REFERENCE, new ReferenceModifier());
		this.cache = cache;
	}

	public void setInput(Flow flow) {
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
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.FLOW_PROPERTY);
		if (descriptors == null)
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof FlowPropertyDescriptor))
				continue;
			add((FlowPropertyDescriptor) descriptor);
		}
	}

	private void add(FlowPropertyDescriptor descriptor) {
		FlowPropertyFactor factor = new FlowPropertyFactor();
		FlowProperty property = cache.get(FlowProperty.class,
				descriptor.getId());
		factor.setFlowProperty(property);
		factor.setConversionFactor(1);
		Flow flow = editor.getModel();
		flow.getFlowPropertyFactors().add(factor);
		setInput(flow.getFlowPropertyFactors());
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		Flow flow = editor.getModel();
		for (FlowPropertyFactor factor : getAllSelected()) {
			if (Objects.equals(factor.getFlowProperty(),
					flow.getReferenceFlowProperty()))
				continue;
			flow.getFlowPropertyFactors().remove(factor);
		}
		setInput(flow.getFlowPropertyFactors());
		editor.setDirty(true);
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
			if (column != 4)
				return null;
			Flow flow = editor.getModel();
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
			case 3:
				return getFormula(factor);
			default:
				return null;
			}
		}

		private String getFormula(FlowPropertyFactor factor) {
			Flow flow = editor.getModel();
			FlowPropertyFactor refFactor = flow.getReferenceFactor();
			String refUnit = refFactor.getFlowProperty().getUnitGroup()
					.getReferenceUnit().getName();
			String unit = factor.getFlowProperty().getUnitGroup()
					.getReferenceUnit().getName();
			return "1.0 " + unit + " = "
					+ Numbers.format(factor.getConversionFactor()) + " "
					+ refUnit;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			Flow flow = editor.getModel();
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
					editor.setDirty(true);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

	private class ReferenceModifier extends
			CheckBoxCellModifier<FlowPropertyFactor> {

		@Override
		protected boolean isChecked(FlowPropertyFactor element) {
			Flow flow = editor.getModel();
			return flow != null
					&& Objects.equals(flow.getReferenceFactor(), element);
		}

		@Override
		protected void setChecked(FlowPropertyFactor element, boolean value) {
			Flow flow = editor.getModel();
			if (value) {
				if (!Objects.equals(flow.getReferenceFlowProperty(),
						element.getFlowProperty())) {
					flow.setReferenceFlowProperty(element.getFlowProperty());
					editor.setDirty(true);
				}
			}
		}
	}

}

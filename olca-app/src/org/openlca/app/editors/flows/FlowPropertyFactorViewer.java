package org.openlca.app.editors.flows;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.usage.FlowPropertyFactorUseSearch;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;

class FlowPropertyFactorViewer extends AbstractTableViewer<FlowPropertyFactor> {

	private static final String NAME = Messages.Name;
	private static final String CONVERSION_FACTOR = Messages.ConversionFactor;
	private static final String REFERENCE_UNIT = Messages.ReferenceUnit;
	private static final String FORMULA = Messages.Formula;
	private static final String IS_REFERENCE = Messages.IsReference;

	private final EntityCache cache;
	private final FlowEditor editor;

	public FlowPropertyFactorViewer(Composite parent, EntityCache cache,
			FlowEditor editor) {
		super(parent);
		getModifySupport().bind(CONVERSION_FACTOR,
				new ConversionFactorModifier());
		getModifySupport().bind(IS_REFERENCE, new ReferenceModifier());
		this.cache = cache;
		this.editor = editor;
		Tables.bindColumnWidths(getViewer(), 0.2, 0.2, 0.2, 0.2, 0.2);
		addDoubleClickHandler();
	}

	private void addDoubleClickHandler() {
		Tables.onDoubleClick(getViewer(), (event) -> {
			TableItem item = Tables.getItem(getViewer(), event);
			if (item == null) {
				onCreate();
				return;
			}
			FlowPropertyFactor factor = getSelected();
			if (factor != null)
				App.openEditor(factor.getFlowProperty());
		});
	}

	public void setInput(Flow flow) {
		if (flow == null)
			setInput(new FlowPropertyFactor[0]);
		else
			setInput(flow.getFlowPropertyFactors());
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FactorLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { NAME, CONVERSION_FACTOR, REFERENCE_UNIT, FORMULA,
				IS_REFERENCE };
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
		FlowPropertyFactor fac = getSelected();
		if (fac == null)
			return;
		Flow flow = editor.getModel();
		if (fac.equals(flow.getReferenceFactor())) {
			Error.showBox("@Cannot delete reference flow property",
					"@The reference flow property of a flow cannot be deleted.");
			return;
		}
		FlowPropertyFactorUseSearch search = new FlowPropertyFactorUseSearch(
				flow, Database.get());
		List<BaseDescriptor> list = search.findUses(fac);
		if (!list.isEmpty()) {
			Error.showBox("@Cannot delete flow property",
					"@The given flow property is used in processes or LCIA methods.");
			return;
		}
		flow.getFlowPropertyFactors().remove(fac);
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
				return Double.toString(factor.getConversionFactor());
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
			return "1.0 " + refUnit + " = "
					+ Double.toString(factor.getConversionFactor()) + " "
					+ unit;
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

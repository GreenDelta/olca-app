package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.usage.FlowPropertyFactorUseSearch;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;

class FlowPropertyFactorViewer extends AbstractTableViewer<FlowPropertyFactor> {

	private static final String NAME = M.Name;
	private static final String CONVERSION_FACTOR = M.ConversionFactor;
	private static final String REFERENCE_UNIT = M.ReferenceUnit;
	private static final String FORMULA = M.Formula;
	private static final String IS_REFERENCE = M.IsReference;

	private final EntityCache cache;
	private final FlowEditor editor;

	public FlowPropertyFactorViewer(Composite parent, EntityCache cache,
			FlowEditor editor) {
		super(parent);
		ModifySupport<FlowPropertyFactor> ms = getModifySupport();
		this.editor = editor;
		this.cache = cache;
		ms.bind(CONVERSION_FACTOR, new ConversionModifier());
		ms.bind(IS_REFERENCE, new ReferenceModifier());
		if (Database.isConnected()) {
			ms.bind("", new CommentDialogModifier<FlowPropertyFactor>(editor.getComments(), CommentPaths::get));
			Tables.bindColumnWidths(getViewer(), 0.2, 0.2, 0.2, 0.2, 0.17);
		} else {
			Tables.bindColumnWidths(getViewer(), 0.2, 0.2, 0.2, 0.2, 0.2);
		}
		addDoubleClickHandler();
		getViewer().getTable().getColumns()[1].setAlignment(SWT.RIGHT);
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
		String[] h = { NAME, CONVERSION_FACTOR, REFERENCE_UNIT, FORMULA, IS_REFERENCE };
		List<String> headers = new ArrayList<>(Arrays.asList(h));
		if (Database.isConnected())
			headers.add("");
		return headers.toArray(new String[headers.size()]);
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ModelSelectionDialog.multiSelect(ModelType.FLOW_PROPERTY);
		if (descriptors == null)
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof FlowPropertyDescriptor))
				continue;
			add((FlowPropertyDescriptor) descriptor);
		}
	}

	private void add(FlowPropertyDescriptor descriptor) {
		FlowProperty property = cache.get(FlowProperty.class, descriptor.getId());
		Flow flow = editor.getModel();
		if (flow.getFactor(property) != null)
			return;
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.setFlowProperty(property);
		factor.setConversionFactor(1);
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
			Error.showBox(M.CannotDeleteReferenceFlowProperty,
					M.ReferenceFlowPropertyCannotBeDeleted);
			return;
		}
		FlowPropertyFactorUseSearch search = new FlowPropertyFactorUseSearch(
				flow, Database.get());
		List<CategorizedDescriptor> list = search.findUses(fac);
		if (!list.isEmpty()) {
			Error.showBox(M.CannotDeleteFlowProperty,
					M.FlowPropertyIsUsed);
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

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column == 0)
				return Images.get(ModelType.FLOW_PROPERTY);
			if (column == 2)
				return Images.get(ModelType.UNIT_GROUP);
			if (column == 4) {
				Flow flow = editor.getModel();
				if (flow == null || flow.getReferenceFactor() == null)
					return Images.get(false);
				FlowPropertyFactor refFactor = flow.getReferenceFactor();
				boolean isRef = refFactor != null && refFactor.equals(element);
				return Images.get(isRef);
			} else if (column == 5) {
				String path = CommentPaths.get((FlowPropertyFactor) element);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FlowPropertyFactor))
				return null;
			FlowPropertyFactor factor = (FlowPropertyFactor) element;
			switch (columnIndex) {
			case 0:
				if (factor.getFlowProperty() == null)
					return null;
				return factor.getFlowProperty().getName();
			case 1:
				return Double.toString(factor.getConversionFactor());
			case 2:
				if (factor.getFlowProperty() == null)
					return null;
				if (factor.getFlowProperty().getUnitGroup() == null)
					return null;
				if (factor.getFlowProperty().getUnitGroup().getReferenceUnit() == null)
					return null;
				return factor.getFlowProperty().getUnitGroup().getReferenceUnit().getName();
			case 3:
				return getFormula(factor);
			default:
				return null;
			}
		}

		private String getFormula(FlowPropertyFactor factor) {
			Flow flow = editor.getModel();
			FlowPropertyFactor refFactor = flow.getReferenceFactor();
			Unit refUnit = getUnit(refFactor);
			Unit unit = getUnit(factor);
			if (unit == null || refUnit == null)
				return null;
			return "1.0 " + refUnit.getName()
					+ " = " + Double.toString(factor.getConversionFactor()) + " " + unit.getName();
		}

		private Unit getUnit(FlowPropertyFactor factor) {
			if (factor == null || factor.getFlowProperty() == null)
				return null;
			UnitGroup unitGroup = factor.getFlowProperty().getUnitGroup();
			if (unitGroup == null)
				return null;
			return unitGroup.getReferenceUnit();
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			Flow flow = editor.getModel();
			if (flow == null || flow.getReferenceFactor() == null)
				return null;
			if (!flow.getReferenceFactor().equals(element))
				return null;
			return UI.boldFont();
		}
	}

	private class ConversionModifier extends DoubleModifier<FlowPropertyFactor> {

		private ConversionModifier() {
			super(editor, "conversionFactor");
		}

		@Override
		public boolean canModify(FlowPropertyFactor element) {
			if (element == null)
				return false;
			return !element.equals(editor.getModel().getReferenceFactor());
		}
	}

	private class ReferenceModifier extends
			CheckBoxCellModifier<FlowPropertyFactor> {

		@Override
		protected boolean isChecked(FlowPropertyFactor element) {
			Flow flow = editor.getModel();
			return Objects.equals(flow.getReferenceFactor(), element);
		}

		@Override
		protected void setChecked(FlowPropertyFactor element, boolean value) {
			Flow flow = editor.getModel();
			if (!value)
				return;
			if (Objects.equals(flow.getReferenceFlowProperty(),
					element.getFlowProperty()))
				return;
			flow.setReferenceFlowProperty(element.getFlowProperty());
			double f = element.getConversionFactor();
			for (FlowPropertyFactor fpFactor : flow.getFlowPropertyFactors()) {
				double factor = fpFactor.getConversionFactor() / f;
				fpFactor.setConversionFactor(factor);
			}
			editor.setDirty(true);
		}

		@Override
		public boolean affectsOtherElements() {
			return true;
		}

	}

}

package org.openlca.app.editors.flows;

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

class FlowPropertyFactorViewer extends AbstractTableViewer<FlowPropertyFactor> {

	private final EntityCache cache;
	private final FlowEditor editor;

	public FlowPropertyFactorViewer(Composite parent, EntityCache cache,
			FlowEditor editor) {
		super(parent);
		ModifySupport<FlowPropertyFactor> ms = getModifySupport();
		this.editor = editor;
		this.cache = cache;
		ms.bind(M.ConversionFactor, new ConversionModifier());
		ms.bind(M.IsReference, new ReferenceModifier());
		ms.bind("", new CommentDialogModifier<FlowPropertyFactor>(
				editor.getComments(), CommentPaths::get));
		Tables.bindColumnWidths(getViewer(), 0.2, 0.2, 0.2, 0.2, 0.17);
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
				App.openEditor(factor.flowProperty);
		});
	}

	public void setInput(Flow flow) {
		if (flow == null)
			setInput(new FlowPropertyFactor[0]);
		else
			setInput(flow.flowPropertyFactors);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FactorLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] {
				M.Name, M.ConversionFactor, M.ReferenceUnit,
				M.Formula, M.IsReference, "" };
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.FLOW_PROPERTY);
		if (descriptors == null)
			return;
		for (BaseDescriptor d : descriptors) {
			add(d);
		}
	}

	private void add(BaseDescriptor d) {
		if (d == null)
			return;
		FlowProperty prop = cache.get(FlowProperty.class, d.id);
		if (prop == null)
			return;
		Flow flow = editor.getModel();
		if (flow.getFactor(prop) != null)
			return;
		FlowPropertyFactor factor = new FlowPropertyFactor();
		factor.flowProperty = prop;
		factor.conversionFactor = 1;
		flow.flowPropertyFactors.add(factor);
		setInput(flow.flowPropertyFactors);
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
		flow.flowPropertyFactors.remove(fac);
		setInput(flow.flowPropertyFactors);
		editor.setDirty(true);
	}

	@OnDrop
	protected void onDrop(BaseDescriptor d) {
		if (d != null)
			add(d);
	}

	private class FactorLabelProvider extends LabelProvider implements
			ITableLabelProvider, ITableFontProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(ModelType.FLOW_PROPERTY);
			if (col == 2)
				return Images.get(ModelType.UNIT_GROUP);
			if (col == 4) {
				Flow flow = editor.getModel();
				if (flow == null || flow.getReferenceFactor() == null)
					return Images.get(false);
				FlowPropertyFactor refFactor = flow.getReferenceFactor();
				boolean isRef = refFactor != null && refFactor.equals(obj);
				return Images.get(isRef);
			} else if (col == 5) {
				String path = CommentPaths.get((FlowPropertyFactor) obj);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof FlowPropertyFactor))
				return null;
			FlowPropertyFactor f = (FlowPropertyFactor) obj;
			switch (col) {
			case 0:
				if (f.flowProperty == null)
					return null;
				return f.flowProperty.name;
			case 1:
				return Double.toString(f.conversionFactor);
			case 2:
				if (f.flowProperty == null)
					return null;
				if (f.flowProperty.unitGroup == null)
					return null;
				if (f.flowProperty.unitGroup.referenceUnit == null)
					return null;
				return f.flowProperty.unitGroup.referenceUnit.name;
			case 3:
				return getFormula(f);
			default:
				return null;
			}
		}

		private String getFormula(FlowPropertyFactor f) {
			Flow flow = editor.getModel();
			FlowPropertyFactor refFactor = flow.getReferenceFactor();
			Unit refUnit = getUnit(refFactor);
			Unit unit = getUnit(f);
			if (unit == null || refUnit == null)
				return null;
			return "1.0 " + refUnit.name
					+ " = " + Double.toString(f.conversionFactor)
					+ " " + unit.name;
		}

		private Unit getUnit(FlowPropertyFactor factor) {
			if (factor == null || factor.flowProperty == null)
				return null;
			UnitGroup unitGroup = factor.flowProperty.unitGroup;
			if (unitGroup == null)
				return null;
			return unitGroup.referenceUnit;
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
			if (Objects.equals(
					flow.referenceFlowProperty,
					element.flowProperty))
				return;
			flow.referenceFlowProperty = element.flowProperty;
			double f = element.conversionFactor;
			for (FlowPropertyFactor fpFactor : flow.flowPropertyFactors) {
				double factor = fpFactor.conversionFactor / f;
				fpFactor.conversionFactor = factor;
			}
			editor.setDirty(true);
		}

		@Override
		public boolean affectsOtherElements() {
			return true;
		}

	}

}

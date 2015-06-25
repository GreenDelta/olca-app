package org.openlca.app.editors.lcia_methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ImpactFactorTable {

	private final String FLOW = Messages.Flow;
	private final String CATEGORY = Messages.Category;
	private final String FLOW_PROPERTY = Messages.FlowProperty;
	private final String UNIT = Messages.Unit;
	private final String FACTOR = Messages.Factor;
	private final String UNCERTAINTY = Messages.Uncertainty;

	private boolean showFormulas = true;
	private IDatabase database = Database.get();
	private ImpactMethodEditor editor;
	private ImpactCategory category;
	private TableViewer viewer;

	public ImpactFactorTable(ImpactMethodEditor editor) {
		this.editor = editor;
		editor.getParameterSupport().afterEvaluation(() -> viewer.refresh());
	}

	public void render(Composite parent, Section section) {
		viewer = Tables.createViewer(parent, new String[] { FLOW, CATEGORY,
				FLOW_PROPERTY, UNIT, FACTOR, UNCERTAINTY });
		viewer.setLabelProvider(new FactorLabelProvider());
		Tables.bindColumnWidths(viewer, 0.2, 0.2, 0.15, 0.15, 0.15, 0.15);
		ModifySupport<ImpactFactor> support = new ModifySupport<>(viewer);
		support.bind(FLOW_PROPERTY, new FlowPropertyModifier());
		support.bind(UNIT, new UnitModifier());
		support.bind(FACTOR, new ValueModifier());
		support.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(),
				editor));
		bindActions(viewer, section);
	}

	void setImpactCategory(ImpactCategory impactCategory, boolean sort) {
		if (impactCategory == null) {
			viewer.setInput(Collections.emptyList());
			this.category = null;
			return;
		}
		this.category = impactCategory;
		List<ImpactFactor> factors = impactCategory.getImpactFactors();
		if (sort)
			sortFactors(factors);
		viewer.setInput(factors);
	}

	private void sortFactors(List<ImpactFactor> factors) {
		Collections.sort(factors, (o1, o2) -> {
			Flow f1 = o1.getFlow();
			Flow f2 = o2.getFlow();
			int c = Strings.compare(f1.getName(), f2.getName());
			if (c != 0)
				return c;
			String cat1 = CategoryPath.getShort(f1.getCategory());
			String cat2 = CategoryPath.getShort(f2.getCategory());
			return Strings.compare(cat1, cat2);
		});
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action formulaSwitch = new FormulaSwitchAction();
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDeletePressed(viewer, (e) -> onRemove());
		Tables.addDropSupport(viewer,
				(descriptors) -> createFactors(descriptors));
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private void onAdd() {
		if (category == null)
			return;
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect((ModelType.FLOW));
		if (descriptors != null)
			createFactors(Arrays.asList(descriptors));
	}

	private void createFactors(List<BaseDescriptor> descriptors) {
		if (descriptors == null || descriptors.isEmpty())
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (descriptors == null
					|| descriptor.getModelType() != ModelType.FLOW)
				continue;
			Flow flow = database.createDao(Flow.class).getForId(
					descriptor.getId());
			ImpactFactor factor = new ImpactFactor();
			factor.setFlow(flow);
			factor.setFlowPropertyFactor(flow.getReferenceFactor());
			factor.setUnit(flow.getReferenceFactor().getFlowProperty()
					.getUnitGroup().getReferenceUnit());
			factor.setValue(1d);
			category.getImpactFactors().add(factor);
		}
		viewer.setInput(category.getImpactFactors());
		editor.setDirty(true);
	}

	private void onRemove() {
		if (category == null)
			return;
		List<ImpactFactor> factors = Viewers.getAllSelected(viewer);
		for (ImpactFactor factor : factors)
			category.getImpactFactors().remove(factor);
		viewer.setInput(category.getImpactFactors());
		editor.setDirty(true);
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
				return CategoryPath.getShort(factor.getFlow().getCategory());
			case 2:
				return factor.getFlowPropertyFactor().getFlowProperty()
						.getName();
			case 3:
				return getFactorUnit(factor);
			case 4:
				if (factor.getFormula() == null || !showFormulas)
					return Double.toString(factor.getValue());
				else
					return factor.getFormula();
			case 5:
				return UncertaintyLabel.get(factor.getUncertainty());
			default:
				return null;
			}
		}

		private String getFactorUnit(ImpactFactor factor) {
			if (factor.getUnit() == null || category == null)
				return null;
			String impactUnit = category.getReferenceUnit();
			if (Strings.notEmpty(impactUnit))
				return impactUnit + "/" + factor.getUnit().getName();
			else
				return "1/" + factor.getUnit().getName();
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
				editor.setDirty(true);
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
				editor.setDirty(true);
			}
		}
	}

	private class ValueModifier extends TextCellModifier<ImpactFactor> {

		@Override
		protected String getText(ImpactFactor factor) {
			if (factor.getFormula() == null)
				return Double.toString(factor.getValue());
			else
				return factor.getFormula();
		}

		@Override
		protected void setText(ImpactFactor factor, String text) {
			try {
				double value = Double.parseDouble(text);
				if (value == factor.getValue() && factor.getFormula() == null)
					return; // nothing changed
				factor.setValue(value);
				factor.setFormula(null);
				editor.setDirty(true);
			} catch (NumberFormatException e) {
				try {
					factor.setFormula(text);
					editor.setDirty(true);
					editor.getParameterSupport().evaluate();
				} catch (Exception ex) {
					Error.showBox(Messages.InvalidFormula, text
							+ " " + Messages.IsInvalidFormula);
				}
			}
		}
	}

	private class FormulaSwitchAction extends Action {
		public FormulaSwitchAction() {
			setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
			setText(Messages.ShowValues);
		}

		@Override
		public void run() {
			showFormulas = !showFormulas;
			if (showFormulas) {
				setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
				setText(Messages.ShowValues);
			} else {
				setImageDescriptor(ImageType.FORMULA_ICON.getDescriptor());
				setText(Messages.ShowFormulas);
			}
			viewer.refresh();
		}
	}

}

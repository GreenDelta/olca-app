package org.openlca.app.editors.lcia_methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.IModelDropHandler;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ParameterPageListener;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Error;
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
import org.openlca.util.Strings;

class ImpactFactorTable implements ParameterPageListener {

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
		editor.getParameterSupport().addListener(this);
	}

	@Override
	public void parameterChanged() {
		viewer.refresh();
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

	public void setImpactCategory(ImpactCategory impactCategory) {
		this.category = impactCategory;
		if (category == null) {
			viewer.setInput(Collections.emptyList());
			return;
		}
		List<ImpactFactor> factors = impactCategory.getImpactFactors();
		Collections.sort(factors, new Comparator<ImpactFactor>() {
			@Override
			public int compare(ImpactFactor o1, ImpactFactor o2) {
				Flow f1 = o1.getFlow();
				Flow f2 = o2.getFlow();
				int c = Strings.compare(f1.getName(), f2.getName());
				if (c != 0)
					return c;
				String cat1 = CategoryPath.getShort(f1.getCategory());
				String cat2 = CategoryPath.getShort(f2.getCategory());
				return Strings.compare(cat1, cat2);
			}
		});
		viewer.setInput(factors);
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(new Runnable() {
			public void run() {
				onAdd();
			}
		});
		Action remove = Actions.onRemove(new Runnable() {
			public void run() {
				onRemove();
			}
		});
		Tables.addDropSupport(viewer, new IModelDropHandler() {
			@Override
			public void handleDrop(List<BaseDescriptor> descriptors) {
				createFactors(descriptors);
			}
		});
		Action formulaSwitch = new FormulaSwitchAction();
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, formulaSwitch);
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
		fireChange();
	}

	private void onRemove() {
		if (category == null)
			return;
		List<ImpactFactor> factors = Viewers.getAllSelected(viewer);
		for (ImpactFactor factor : factors)
			category.getImpactFactors().remove(factor);
		viewer.setInput(category.getImpactFactors());
		fireChange();
	}

	private void fireChange() {
		editor.postEvent(editor.IMPACT_FACTOR_CHANGE, this);
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
				return factor.getUnit().getName();
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
				fireChange();
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
				fireChange();
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
				fireChange();
			} catch (NumberFormatException e) {
				try {
					double val = editor.getParameterSupport().eval(text);
					factor.setValue(val);
					factor.setFormula(text);
					fireChange();
				} catch (Exception ex) {
					Error.showBox("Invalid formula", text
							+ " is an invalid formula");
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

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
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

public class UnitViewer extends AbstractTableViewer<Unit> {

	private interface LABEL {

		String CONVERSION_FACTOR = Messages.ConversionFactor;
		String DESCRIPTION = Messages.Description;
		String FORMULA = Messages.Formula;
		String IS_REFERENCE = Messages.IsReference;
		String NAME = Messages.Name;
		String SYNONYMS = Messages.Synonyms;

	}

	private static final String[] COLUMN_HEADERS = { LABEL.NAME,
			LABEL.DESCRIPTION, LABEL.SYNONYMS, LABEL.CONVERSION_FACTOR,
			LABEL.FORMULA, LABEL.IS_REFERENCE };

	private UnitGroup unitGroup;

	public UnitViewer(Composite parent) {
		super(parent);
		getCellModifySupport().bind(LABEL.NAME, new NameModifier());
		getCellModifySupport().bind(LABEL.DESCRIPTION,
				new DescriptionModifier());
		getCellModifySupport().bind(LABEL.SYNONYMS, new SynonymsModifier());
		getCellModifySupport().bind(LABEL.CONVERSION_FACTOR,
				new ConversionFactorModifier());
		getCellModifySupport().bind(LABEL.IS_REFERENCE,
				new ReferenceModifier());
		getViewer().refresh(true);
	}

	public void setInput(UnitGroup unitGroup) {
		this.unitGroup = unitGroup;
		if (unitGroup == null)
			setInput(new Unit[0]);
		else
			setInput(unitGroup.getUnits().toArray(
					new Unit[unitGroup.getUnits().size()]));
		getViewer().refresh(true);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new UnitLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnAdd
	protected void onCreate() {
		Unit unit = new Unit();
		unit.setName("newUnit");
		unit.setConversionFactor(1d);
		fireModelChanged(ModelChangeType.CREATE, unit);
		setInput(unitGroup);
	}

	@OnRemove
	protected void onRemove() {
		for (Unit unit : getAllSelected())
			if (!Objects.equals(unitGroup.getReferenceUnit(), unit))
				fireModelChanged(ModelChangeType.REMOVE, unit);
		setInput(unitGroup);
	}

	private class UnitLabelProvider extends LabelProvider implements
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
				return ImageType.UNIT_GROUP_ICON.get();
			if (column != 5)
				return null;
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit != null && refUnit.equals(element))
				return ImageType.CHECK_TRUE.get();
			return ImageType.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Unit))
				return null;
			Unit unit = (Unit) element;
			switch (columnIndex) {
			case 0:
				return unit.getName();
			case 1:
				return unit.getDescription();
			case 2:
				return unit.getSynonyms();
			case 3:
				return Numbers.format(unit.getConversionFactor());
			case 4:
				return getFormulaText(unit);
			default:
				return null;
			}
		}

		private String getFormulaText(Unit unit) {
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit == null)
				return null;
			String amount = "[" + unit.getName() + "]";
			String refAmount = "[" + refUnit.getName() + "]";
			String factor = Numbers.format(unit.getConversionFactor());
			return amount + " = " + factor + " * " + refAmount;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit != null && refUnit.equals(element)) {
				if (boldFont == null)
					boldFont = UI.boldFont(getViewer().getTable());
				return boldFont;
			}
			return null;
		}

	}

	private class NameModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getName();
		}

		@Override
		protected void setText(Unit element, String text) {
			if (!Objects.equals(text, element.getName())) {
				element.setName(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

	private class DescriptionModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getDescription();
		}

		@Override
		protected void setText(Unit element, String text) {
			if (!Objects.equals(text, element.getDescription())) {
				element.setDescription(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}
	}

	private class SynonymsModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getSynonyms();
		}

		@Override
		protected void setText(Unit element, String text) {
			if (!Objects.equals(text, element.getSynonyms())) {
				element.setSynonyms(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

	private class ConversionFactorModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return Double.toString(element.getConversionFactor());
		}

		@Override
		protected void setText(Unit element, String text) {
			try {
				double value = Double.parseDouble(text);
				if (value != element.getConversionFactor()) {
					element.setConversionFactor(Double.parseDouble(text));
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {

			}
		}
	}

	private class ReferenceModifier extends CheckBoxCellModifier<Unit> {

		@Override
		protected boolean isChecked(Unit element) {
			return unitGroup != null
					&& Objects.equals(unitGroup.getReferenceUnit(), element);
		}

		@Override
		protected void setChecked(Unit element, boolean value) {
			if (value) {
				if (!Objects.equals(element, unitGroup.getReferenceUnit())) {
					unitGroup.setReferenceUnit(element);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			}
		}

	}

}

package org.openlca.app.editors.units;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.app.viewers.table.modify.field.StringModifier;
import org.openlca.core.database.UnitDao;
import org.openlca.core.database.usage.UnitUseSearch;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

class UnitViewer extends AbstractTableViewer<Unit> {

	private static final String CONVERSION_FACTOR = Messages.ConversionFactor;
	private static final String DESCRIPTION = Messages.Description;
	private static final String FORMULA = Messages.Formula;
	private static final String IS_REFERENCE = Messages.IsReference;
	private static final String NAME = Messages.Name;
	private static final String SYNONYMS = Messages.Synonyms;

	private final UnitGroupEditor editor;

	public UnitViewer(Composite parent, UnitGroupEditor editor) {
		super(parent);
		this.editor = editor;
		ModifySupport<Unit> ms = getModifySupport();
		ms.bind(NAME, new NameModifier());
		ms.bind(DESCRIPTION, new StringModifier<>(editor, "description"));
		ms.bind(SYNONYMS, new StringModifier<>(editor, "synonyms"));
		ms.bind(CONVERSION_FACTOR, new ConversionModifier());
		ms.bind(IS_REFERENCE, new ReferenceModifier());
		getViewer().refresh(true);
		Tables.bindColumnWidths(getViewer(), 0.25, 0.15, 0.15, 0.15, 0.15, 0.15);
		Tables.onDoubleClick(getViewer(), (event) -> {
			TableItem item = Tables.getItem(getViewer(), event);
			if (item == null)
				onCreate();
		});
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new UnitLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { NAME, DESCRIPTION, SYNONYMS, CONVERSION_FACTOR,
				FORMULA, IS_REFERENCE };
	}

	@OnAdd
	protected void onCreate() {
		UnitDao dao = new UnitDao(Database.get());
		Unit unit = new Unit();
		String name = "new unit";
		UnitGroup group = editor.getModel();
		int i = 2;
		while (!dao.getForName(name).isEmpty() || group.getUnit(name) != null)
			name = "new unit " + i++;
		unit.setName(name);
		unit.setRefId(UUID.randomUUID().toString());
		unit.setConversionFactor(1d);
		group.getUnits().add(unit);
		setInput(group.getUnits());
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		UnitGroup group = editor.getModel();
		for (Unit unit : getAllSelected()) {
			if (Objects.equals(group.getReferenceUnit(), unit)) {
				Error.showBox("@Cannot delete reference unit",
						"@The reference unit of a unit group cannot be deleted.");
				continue;
			}
			UnitUseSearch usage = new UnitUseSearch(Database.get());
			if (!usage.findUses(unit).isEmpty()) {
				Error.showBox("@Cannot delete unit",
						"@The given unit is used in processes, impact methods or social indicators.");
				continue;
			}
			group.getUnits().remove(unit);
		}
		setInput(group.getUnits());
		editor.setDirty(true);
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
			UnitGroup group = editor.getModel();
			Unit refUnit = group != null ? group.getReferenceUnit() : null;
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
			UnitGroup group = editor.getModel();
			Unit refUnit = group != null ? group.getReferenceUnit() : null;
			if (refUnit == null)
				return null;
			String amount = "1.0 " + unit.getName();
			String factor = Numbers.format(unit.getConversionFactor());
			String refAmount = factor + " " + refUnit.getName();
			return amount + " = " + refAmount;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			UnitGroup group = editor.getModel();
			Unit refUnit = group != null ? group.getReferenceUnit() : null;
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
			if (Objects.equals(element.getName(), text))
				return;
			if (!new UnitDao(Database.get()).getForName(text).isEmpty() || editor.getModel().getUnit(text) != null) {
				Error.showBox("A unit with the name '" + text + "' already exists");
				return;
			}
			element.setName(text);
		}

	}

	private class ConversionModifier extends DoubleModifier<Unit> {

		private ConversionModifier() {
			super(editor, "conversionFactor");
		}

		@Override
		public boolean canModify(Unit element) {
			if (element == null)
				return false;
			return !element.equals(editor.getModel().getReferenceUnit());
		}
	}

	private class ReferenceModifier extends CheckBoxCellModifier<Unit> {

		@Override
		protected boolean isChecked(Unit element) {
			UnitGroup group = editor.getModel();
			return Objects.equals(group.getReferenceUnit(), element);
		}

		@Override
		protected void setChecked(Unit element, boolean value) {
			UnitGroup group = editor.getModel();
			if (!value)
				return;
			if (Objects.equals(element, group.getReferenceUnit()))
				return;
			group.setReferenceUnit(element);
			double f = element.getConversionFactor();
			for (Unit unit : group.getUnits()) {
				double factor = unit.getConversionFactor() / f;
				unit.setConversionFactor(factor);
			}
			editor.setDirty(true);
		}

		@Override
		public boolean affectsOtherElements() {
			return true;
		}

	}
}

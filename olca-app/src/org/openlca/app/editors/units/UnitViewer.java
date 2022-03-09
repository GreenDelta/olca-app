package org.openlca.app.editors.units;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.DoubleCellModifier;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.app.viewers.tables.modify.field.StringModifier;
import org.openlca.core.database.UnitDao;
import org.openlca.core.database.usage.UnitUsageSearch;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

class UnitViewer extends AbstractTableViewer<Unit> {

	private static final String CONVERSION_FACTOR = M.ConversionFactor;
	private static final String DESCRIPTION = M.Description;
	private static final String FORMULA = M.Formula;
	private static final String IS_REFERENCE = M.IsReference;
	private static final String NAME = M.Name;
	private static final String SYNONYMS = M.Synonyms;
	private static final String COMMENT = "";

	private final UnitGroupEditor editor;

	public UnitViewer(Composite parent, UnitGroupEditor editor) {
		super(parent);
		this.editor = editor;
		getViewer().refresh(true);
		Tables.onDoubleClick(getViewer(), (event) -> {
			TableItem item = Tables.getItem(getViewer(), event);
			if (item == null)
				onCreate();
		});
		getViewer().getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		Tables.bindColumnWidths(getViewer(), 0.25, 0.15, 0.15, 0.15, 0.15, 0.12);

		// cell modifiers
		if (!editor.isEditable())
			return;
		getModifySupport()
				.bind(NAME, new NameModifier())
				.bind(DESCRIPTION, new StringModifier<>(editor, "description"))
				.bind(SYNONYMS, new StringModifier<>(editor, "synonyms"))
				.bind(CONVERSION_FACTOR, new FactorModifier(editor))
				.bind(IS_REFERENCE, new ReferenceModifier())
				.bind("", new CommentDialogModifier<>(
						editor.getComments(), CommentPaths::get));
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new UnitLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] {
				NAME,
				DESCRIPTION,
				SYNONYMS,
				CONVERSION_FACTOR,
				FORMULA,
				IS_REFERENCE,
				COMMENT
		};
	}

	@OnAdd
	protected void onCreate() {
		if (!editor.isEditable())
			return;
		UnitDao dao = new UnitDao(Database.get());
		Unit unit = new Unit();
		String name = "new unit";
		UnitGroup group = editor.getModel();
		int i = 2;
		while (!dao.getForName(name).isEmpty() || group.getUnit(name) != null)
			name = "new unit " + i++;
		unit.name = name;
		unit.refId = UUID.randomUUID().toString();
		unit.conversionFactor = 1d;
		group.units.add(unit);
		setInput(group.units);
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		if (!editor.isEditable())
			return;
		UnitGroup group = editor.getModel();
		for (Unit unit : getAllSelected()) {
			if (Objects.equals(group.referenceUnit, unit)) {
				MsgBox.error(M.CannotDeleteReferenceUnit,
						M.ReferenceUnitCannotBeDeleted);
				continue;
			}
			var usage = new UnitUsageSearch(Database.get(), unit).run();
			if (!usage.isEmpty()) {
				MsgBox.error(M.CannotDeleteUnit, M.UnitIsUsed);
				continue;
			}
			group.units.remove(unit);
		}
		setInput(group.units);
		editor.setDirty(true);
	}

	private class UnitLabelProvider extends LabelProvider implements
			ITableLabelProvider, ITableFontProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column < 5)
				return null;
			if (column == 5) {
				UnitGroup group = editor.getModel();
				Unit refUnit = group != null ? group.referenceUnit : null;
				boolean isRef = refUnit != null && refUnit.equals(element);
				return Images.get(isRef);
			} else if (column == 6) {
				String path = CommentPaths.get((Unit) element);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Unit))
				return null;
			Unit unit = (Unit) obj;
			return switch (col) {
				case 0 -> unit.name;
				case 1 -> unit.description;
				case 2 -> unit.synonyms;
				case 3 -> Numbers.format(unit.conversionFactor);
				case 4 -> getFormulaText(unit);
				default -> null;
			};
		}

		private String getFormulaText(Unit unit) {
			UnitGroup group = editor.getModel();
			Unit refUnit = group != null ? group.referenceUnit : null;
			if (refUnit == null)
				return null;
			String amount = "1.0 " + unit.name;
			String factor = Numbers.format(unit.conversionFactor);
			String refAmount = factor + " " + refUnit.name;
			return amount + " = " + refAmount;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			UnitGroup group = editor.getModel();
			Unit refUnit = group != null ? group.referenceUnit : null;
			if (refUnit != null && refUnit.equals(element)) {
				return UI.boldFont();
			}
			return null;
		}

	}

	private class NameModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit unit) {
			return unit.name;
		}

		@Override
		protected void setText(Unit unit, String text) {
			if (Objects.equals(unit.name, text))
				return;
			if (!new UnitDao(Database.get()).getForName(text).isEmpty()
					|| editor.getModel().getUnit(text) != null) {
				MsgBox.error("A unit with the name '" + text + "' already exists");
				return;
			}
			unit.name = text;
			editor.setDirty(true);
		}

	}

	private static class FactorModifier extends DoubleCellModifier<Unit> {

		private final UnitGroupEditor editor;

		FactorModifier(UnitGroupEditor editor) {
			this.editor = editor;
		}

		@Override
		public boolean canModify(Unit unit) {
			if (unit == null)
				return false;
			var refUnit = editor.getModel().referenceUnit;
			return !unit.equals(refUnit);
		}

		@Override
		public Double getDouble(Unit unit) {
			return unit.conversionFactor;
		}

		@Override
		public void setDouble(Unit unit, Double value) {
			double d = value == null ? 0 : value;
			if (Double.compare(d, unit.conversionFactor) == 0)
				return;
			unit.conversionFactor = d;
			editor.setDirty();
		}
	}

	private class ReferenceModifier extends CheckBoxCellModifier<Unit> {

		@Override
		protected boolean isChecked(Unit element) {
			UnitGroup group = editor.getModel();
			return Objects.equals(group.referenceUnit, element);
		}

		@Override
		protected void setChecked(Unit u, boolean value) {
			UnitGroup group = editor.getModel();
			if (!value)
				return;
			if (Objects.equals(u, group.referenceUnit))
				return;
			group.referenceUnit = u;
			double f = u.conversionFactor;
			for (Unit unit : group.units) {
				unit.conversionFactor /= f;
			}
			editor.setDirty(true);
		}

		@Override
		public boolean affectsOtherElements() {
			return true;
		}

	}
}

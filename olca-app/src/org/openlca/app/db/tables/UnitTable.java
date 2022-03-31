package org.openlca.app.db.tables;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UnitTable extends SimpleFormEditor {

	private List<UnitItem> unitItems;

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var id = "DbUnitTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			var items = new ArrayList<UnitItem>();
			for (var group : Database.get().getAll(UnitGroup.class)) {
				for (var unit : group.units) {
					items.add(new UnitItem(group, unit));
				}
			}
			Collections.sort(items);
			unitItems = items;
		} catch (Exception e) {
			ErrorReporter.on("failed to load units", e);
		}
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<UnitItem> unitItems;

		Page(UnitTable table) {
			super(table, "DbUnitTable", M.Units);
			unitItems = table.unitItems != null
				? table.unitItems
				: Collections.emptyList();
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.Units);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var filterComp = tk.createComposite(body);
			UI.gridLayout(filterComp, 2);
			UI.gridData(filterComp, true, false);
			var filter = UI.formText(filterComp, tk, M.Filter);

			var table = Tables.createViewer(body,
				M.UnitGroup,
				M.Name,
				M.Description,
				M.Synonyms,
				M.ReferenceUnit,
				"ID");
			Tables.bindColumnWidths(table, 0.2, 0.1, 0.2, 0.2, 0.1, 0.2);

			var label = new UnitTable.Label();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 2, 3, 4, 5, 6);
			table.setInput(unitItems);
			TextFilter.on(table, filter);
			Actions.forRootEntities(table);
		}
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UnitItem))
				return null;
			return switch (col) {
				case 0 -> Images.get(ModelType.UNIT_GROUP);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UnitItem unitItem))
				return null;
			return switch (col) {
				case 0 -> Labels.name(unitItem.group);
				case 1 -> Labels.name(unitItem.unit);
				case 2 -> unitItem.unit.description;
				case 3 -> unitItem.unit.synonyms;
				case 4 -> Labels.name(unitItem.group.referenceUnit);
				case 5 -> unitItem.unit.refId;
				default -> null;
			};
		}
	}

	record UnitItem(UnitGroup group, Unit unit) implements Comparable<UnitItem> {

		@Override
		public int compareTo(UnitItem other) {
			if (other == null)
				return 1;
			if (Objects.equals(this.group, other.group)
				|| this.group == null
				|| this.unit == null
				|| other.group == null
				|| other.unit == null)
				return Strings.compare(
					Labels.name(this.unit), Labels.name(other.unit));
			return Strings.compare(
				Labels.name(this.group),
				Labels.name(other.group));
		}
	}
}

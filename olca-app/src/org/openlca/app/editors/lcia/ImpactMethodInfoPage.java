package org.openlca.app.editors.lcia;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.Source;
import org.openlca.util.Strings;

class ImpactMethodInfoPage extends ModelPage<ImpactMethod> {

	private TableViewer indicatorTable;
	private final ImpactMethodEditor editor;

	ImpactMethodInfoPage(ImpactMethodEditor editor) {
		super(editor, "ImpactMethodInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.formHeader(this);
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);

		// info section with source link
		var info = new InfoSection(getEditor()).render(body, tk);
		var comp = info.composite();
		ModelLink.of(Source.class)
			.renderOn(comp, tk, M.Source)
			.setModel(editor.getModel().source)
			.onChange(source -> {
				var method = editor.getModel();
				method.source = source;
				editor.setDirty();
			});
		UI.filler(comp, tk);

		// code
		var codeText = UI.formText(comp, tk, "Code");
		Controls.set(codeText, getModel().code, code -> {
			getModel().code = code;
			getEditor().setDirty();
		});
		UI.filler(comp, tk);

		createIndicatorTable(tk, body);
		body.setFocus();
		form.reflow(true);
	}

	private void createIndicatorTable(FormToolkit tk, Composite body) {
		Section section = UI.section(body, tk, M.ImpactCategories);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk, 1);
		indicatorTable = Tables.createViewer(comp,
			M.Name, M.Description, M.ReferenceUnit, "");
		indicatorTable.setLabelProvider(new CategoryLabelProvider());
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> impacts = method.impactCategories;
		impacts.sort((c1, c2) -> Strings.compare(c1.name, c2.name));
		indicatorTable.setInput(impacts);
		Tables.bindColumnWidths(indicatorTable, 0.5, 0.25, 0.22);
		bindActions(indicatorTable, section);
	}

	private void bindActions(TableViewer indicatorTable, Section section) {
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action copy = TableClipboard.onCopySelected(indicatorTable);

		Action open = Actions.onOpen(() -> {
			ImpactCategory i = Viewers.getFirstSelected(indicatorTable);
			if (i != null) {
				App.open(i);
			}
		});
		Actions.bind(indicatorTable, add, remove, open, copy);
		CommentAction.bindTo(section, "impactCategories",
			editor.getComments(), add, remove);
		Tables.onDeletePressed(indicatorTable, _e -> onRemove());
		Tables.onDoubleClick(indicatorTable, _e -> open.run());
	}

	private void onAdd() {
		ImpactMethod method = editor.getModel();
		var d = ModelSelector.select(ModelType.IMPACT_CATEGORY);
		if (d == null)
			return;
		ImpactCategoryDao dao = new ImpactCategoryDao(Database.get());
		ImpactCategory impact = dao.getForId(d.id);
		if (impact == null)
			return;
		method.impactCategories.add(impact);
		indicatorTable.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void onRemove() {
		ImpactMethod method = editor.getModel();
		List<ImpactCategory> impacts = Viewers.getAllSelected(indicatorTable);
		for (ImpactCategory impact : impacts) {
			method.impactCategories.remove(impact);
			for (NwSet set : method.nwSets) {
				NwFactor factor = set.getFactor(impact);
				if (factor != null) {
					set.factors.remove(factor);
				}
			}
		}
		indicatorTable.setInput(method.impactCategories);
		fireCategoryChange();
	}

	private void fireCategoryChange() {
		editor.postEvent(editor.IMPACT_CATEGORY_CHANGE, this);
		editor.setDirty(true);
	}

	private class CategoryLabelProvider extends LabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImpactCategory impact))
				return null;
			if (col == 0)
				return Images.get(ModelType.IMPACT_CATEGORY);
			if (col == 3)
				return Images.get(editor.getComments(), CommentPaths.get(impact));
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactCategory impact))
				return null;
			return switch (col) {
				case 0 -> Labels.name(impact);
				case 1 -> impact.description;
				case 2 -> impact.referenceUnit;
				default -> null;
			};
		}
	}
}

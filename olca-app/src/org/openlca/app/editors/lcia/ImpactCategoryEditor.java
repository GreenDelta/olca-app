package org.openlca.app.editors.lcia;

import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.geo.GeoPage;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.parameters.ParameterChangeSupport;
import org.openlca.app.editors.parameters.ParameterPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.LibraryUtil;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.usage.UsageSearch;
import org.openlca.core.model.Category;
import org.openlca.core.model.Direction;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class ImpactCategoryEditor extends ModelEditor<ImpactCategory> {

	public final String FACTORS_CHANGED_EVENT = "FACTORS_CHANGED_EVENT";

	private ParameterChangeSupport parameterSupport;

	public ImpactCategoryEditor() {
		super(ImpactCategory.class);
	}

	public ParameterChangeSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		// evalFormulas() takes quite long; we skip this here
		parameterSupport = new ParameterChangeSupport();
		parameterSupport.onEvaluation(this::evalFormulas);
		var impact = getModel();
		if (impact.isFromLibrary()) {
			LibraryUtil.fillFactorsOf(impact);
		}
	}

	private void evalFormulas() {
		var errors = Formulas.eval(Database.get(), getModel());
		if (!errors.isEmpty()) {
			var message = errors.get(0);
			if (errors.size() > 1)
				message += " (" + (errors.size() - 1) + " more)";
			MsgBox.error(M.FormulaEvaluationFailed, message);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new ImpactFactorPage(this));
			if (!getModel().isFromLibrary()) {
				addPage(ParameterPage.create(this));
				addPage(new GeoPage(this));
				addPage(new ImpactSimilaritiesPage(this));
				addCommentPage();
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to init pages", e);
		}
	}

	static class InfoPage extends ModelPage<ImpactCategory> {

		InfoPage(ImpactCategoryEditor editor) {
			super(editor, "ImpactCategoryInfo", M.GeneralInformation);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(this);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var info = new InfoSection(getEditor()).render(body, tk);
			var comp = info.composite();

			// source, code, reference unit
			modelLink(comp, M.Source, "source");
			text(comp, "Code", "code");
			text(comp, M.ReferenceUnit, "referenceUnit");

			// impact direction
			var combo = UI.formCombo(comp, tk, "Impact direction");
			combo.setItems("Unspecified", M.Input, M.Output);
			UI.gridData(combo, false, false).widthHint = 150;
			var dir = getModel().direction;
			combo.select(dir != null
				? dir == Direction.INPUT ? 1 : 2
				: 0);
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				getModel().direction = switch (idx) {
					case 1 -> Direction.INPUT;
					case 2 -> Direction.OUTPUT;
					default -> null;
				};
				getEditor().setDirty();
			});

			// usage
			createUsageTable(tk, body);

			body.setFocus();
			form.reflow(true);
		}

		private void createUsageTable(FormToolkit tk, Composite body) {
			var section = UI.section(body, tk,
				"Used in impact assessment methods");
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var table = Tables.createViewer(comp, M.Name, M.Category);
			table.setLabelProvider(new MethodLabel());
			Tables.bindColumnWidths(table, 0.5, 0.5);

			// bind actions
			var onCopy = TableClipboard.onCopySelected(table);
			var onOpen = Actions.onOpen(() -> {
				ImpactMethodDescriptor m = Viewers.getFirstSelected(table);
				if (m != null) {
					App.open(m);
				}
			});
			Actions.bind(table, onCopy, onOpen);
			Tables.onDoubleClick(table, _e -> onOpen.run());

			// set input
			var methods = UsageSearch.find(Database.get(), getModel())
				.stream()
				.filter(m -> m.type == ModelType.IMPACT_METHOD)
				.sorted((m1, m2) -> Strings.compare(m1.name, m2.name))
				.collect(Collectors.toList());
			table.setInput(methods);
		}

		private static class MethodLabel extends LabelProvider
			implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				if (!(obj instanceof ImpactMethodDescriptor m))
					return null;
				if (col == 0)
					return Images.get(m);
				if (col == 1 && m.category != null)
					return Images.getForCategory(ModelType.IMPACT_METHOD);
				return null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof ImpactMethodDescriptor m))
					return null;
				if (col == 0)
					return Labels.name(m);
				if (col != 1 || m.category == null)
					return null;
				var category = Cache.getEntityCache()
					.get(Category.class, m.category);
				return category == null
					? null
					: CategoryPath.getFull(category);
			}
		}
	}

}

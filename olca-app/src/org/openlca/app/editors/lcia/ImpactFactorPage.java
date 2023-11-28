package org.openlca.app.editors.lcia;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.DialogCellEditor;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

class ImpactFactorPage extends ModelPage<ImpactCategory> {

	private final ImpactCategoryEditor editor;

	private boolean showFormulas = true;
	private final IDatabase database = Database.get();
	private TableViewer viewer;

	ImpactFactorPage(ImpactCategoryEditor editor) {
		super(editor, "ImpactFactorPage", M.ImpactFactors);
		this.editor = editor;
		editor.getParameterSupport().afterEvaluation(() -> {
			if (viewer != null) {
				viewer.refresh();
			}
		});
		editor.onEvent(editor.FACTORS_CHANGED_EVENT, () -> {
			if (viewer != null) {
				viewer.setInput(impact().impactFactors);
			}
		});
	}

	private ImpactCategory impact() {
		return editor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(this);
		var tk = mform.getToolkit();
		var body = UI.body(form, tk);
		var section = UI.section(body, tk, M.ImpactFactors);
		UI.gridData(section, true, true);
		var client = UI.composite(section, tk);
		section.setClient(client);
		UI.gridLayout(client, 1);
		render(client, section);
		var factors = impact().impactFactors;
		sortFactors(factors);
		viewer.setInput(factors);
		form.reflow(true);
	}

	public void render(Composite parent, Section section) {
		viewer = Tables.createViewer(parent,
			M.Flow,
			M.Category,
			M.Factor,
			M.Unit,
			M.Uncertainty,
			M.Location,
			"" /* comment */);
		var label = new FactorLabel();
		Viewers.sortByLabels(viewer, label, 0, 1, 3, 4, 5);
		Viewers.sortByDouble(viewer, (ImpactFactor f) -> f.value, 2);
		viewer.setLabelProvider(label);

		if (!getModel().isFromLibrary()) {
			bindModifySupport(viewer);
		}
		Tables.bindColumnWidths(viewer, 0.2, 0.2, 0.125, 0.125, 0.125, 0.125);
		bindActions(viewer, section);
		viewer.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
	}

	private void bindModifySupport(TableViewer viewer) {
		var support = new ModifySupport<ImpactFactor>(viewer);
		support.bind(M.Unit, new UnitCell(editor))
			.bind(M.Uncertainty, new UncertaintyCellEditor(
				viewer.getTable(), editor))
			.bind(M.Location, new LocationModifier(
				viewer.getTable(), editor))
			.bind("", new CommentDialogModifier<>(
				editor.getComments(),
				f -> CommentPaths.get(impact(), f)));

		// factor editor with auto-completion support for parameter names
		var factorEditor = new FormulaCellEditor(
			viewer, () -> editor.getModel().parameters);
		support.bind(M.Factor, factorEditor);
		factorEditor.onEdited((obj, factor) -> {
			if (!(obj instanceof ImpactFactor f))
				return;
			try {
				double value = Double.parseDouble(factor);
				f.formula = null;
				f.value = value;
			} catch (NumberFormatException ex) {
				f.formula = factor;
				editor.getParameterSupport().evaluate();
			}
			editor.setDirty(true);
			viewer.refresh();
		});
	}

	private void sortFactors(List<ImpactFactor> factors) {
		factors.sort((o1, o2) -> {
			Flow f1 = o1.flow;
			Flow f2 = o2.flow;
			int c = Strings.compare(f1.name, f2.name);
			if (c != 0)
				return c;
			String cat1 = CategoryPath.getShort(f1.category);
			String cat2 = CategoryPath.getShort(f2.category);
			return Strings.compare(cat1, cat2);
		});
	}

	void bindActions(TableViewer viewer, Section section) {
		Tables.onDoubleClick(viewer, (event) -> {
			ImpactFactor factor = Viewers.getFirstSelected(viewer);
			if (factor != null && factor.flow != null) {
				App.open(factor.flow);
			}
		});

		// only copy for library models
		if (getModel().isFromLibrary()) {
			Actions.bind(viewer, TableClipboard.onCopySelected(viewer));
			return;
		}

		var add = Actions.onAdd(this::onAdd);
		var remove = Actions.onRemove(this::onRemove);
		var copy = TableClipboard.onCopySelected(viewer);
		var paste = TableClipboard.onPaste(viewer, this::onPaste);
		var formulaSwitch = new FormulaSwitchAction();

		Actions.bind(viewer, add, remove, copy, paste);
		Tables.onDeletePressed(viewer, _e -> onRemove());
		ModelTransfer.onDrop(viewer.getTable(), this::createFactors);
		Actions.bind(section, add, remove, formulaSwitch);
	}

	private void onAdd() {
		var flows = ModelSelector.multiSelect(ModelType.FLOW);
		if (flows.isEmpty())
			return;
		createFactors(flows);
	}

	private void createFactors(List<? extends Descriptor> flows) {
		if (flows == null || flows.isEmpty())
			return;
		for (var d : flows) {
			if (d == null || d.type != ModelType.FLOW)
				continue;
			Flow flow = new FlowDao(database).getForId(d.id);
			if (flow == null)
				continue;
			impact().factor(flow, 1);
		}
		viewer.setInput(impact().impactFactors);
		editor.setDirty(true);
	}

	private void onRemove() {
		List<ImpactFactor> factors = Viewers.getAllSelected(viewer);
		for (ImpactFactor factor : factors) {
			impact().impactFactors.remove(factor);
		}
		viewer.setInput(impact().impactFactors);
		editor.setDirty(true);
	}

	private void onPaste(String text) {
		List<ImpactFactor> factors = App.exec(
			"Parse factors", () -> FactorClipboard.read(text));
		if (factors.isEmpty())
			return;
		impact().impactFactors.addAll(factors);
		viewer.setInput(impact().impactFactors);
		editor.setDirty(true);
	}

	private class FactorLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ImpactFactor f))
				return null;
			if (col == 0)
				return Images.get(f.flow);
			if (col == 6)
				return Images.get(editor.getComments(),
					CommentPaths.get(impact(), f));
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactFactor f))
				return null;
			return switch (col) {
				case 0 -> Labels.name(f.flow);
				case 1 -> Labels.category(f.flow);
				case 2 -> f.formula == null || !showFormulas
					? Double.toString(f.value)
					: f.formula;
				case 3 -> getFactorUnit(f);
				case 4 -> Uncertainty.string(f.uncertainty);
				case 5 -> Labels.code(f.location);
				default -> null;
			};
		}

		private String getFactorUnit(ImpactFactor factor) {
			if (factor.unit == null)
				return null;
			String impactUnit = impact().referenceUnit;
			if (Strings.notEmpty(impactUnit))
				return impactUnit + "/" + factor.unit.name;
			else
				return "1/" + factor.unit.name;
		}
	}

	private static class LocationModifier extends DialogCellEditor {

		private final ImpactCategoryEditor editor;
		private ImpactFactor factor;

		LocationModifier(Composite parent, ImpactCategoryEditor editor) {
			super(parent);
			this.editor = editor;
		}

		@Override
		protected void doSetValue(Object value) {
			if (!(value instanceof ImpactFactor)) {
				super.doSetValue("");
				factor = null;
				return;
			}
			factor = (ImpactFactor) value;
			String s = Labels.name(factor.location);
			super.doSetValue(s == null ? "" : s);
		}

		@Override
		protected Object openDialogBox(Control control) {
			if (factor == null)
				return null;
			ModelSelector dialog = new ModelSelector(
				ModelType.LOCATION);
			dialog.isEmptyOk = true;
			if (dialog.open() != Window.OK)
				return null;

			var loc = dialog.first();

			// clear the location
			if (loc == null) {
				if (factor.location == null)
					return null;
				// delete the location
				factor.location = null;
				editor.setDirty(true);
				return factor;
			}

			// the same location was selected again
			if (factor.location != null
					&& factor.location.id == loc.id)
				return null;

			// a new location was selected
			LocationDao dao = new LocationDao(Database.get());
			factor.location = dao.getForId(loc.id);
			editor.setDirty(true);
			return factor;
		}
	}

	private class FormulaSwitchAction extends Action {
		public FormulaSwitchAction() {
			setImageDescriptor(Icon.NUMBER.descriptor());
			setText(M.ShowValues);
		}

		@Override
		public void run() {
			showFormulas = !showFormulas;
			if (showFormulas) {
				setImageDescriptor(Icon.NUMBER.descriptor());
				setText(M.ShowValues);
			} else {
				setImageDescriptor(Icon.FORMULA.descriptor());
				setText(M.ShowFormulas);
			}
			viewer.refresh();
		}
	}
}

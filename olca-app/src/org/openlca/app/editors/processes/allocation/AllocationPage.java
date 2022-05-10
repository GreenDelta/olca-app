package org.openlca.app.editors.processes.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.util.AllocationUtils;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocationPage extends ModelPage<Process> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	final ProcessEditor editor;
	private final boolean withComments;

	private TableViewer table;
	private CausalFactorTable causalTable;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", M.Allocation);
		this.editor = editor;
		withComments = editor.hasAnyComment("allocationFactors");
		editor.onEvent(ProcessEditor.EXCHANGES_CHANGED, () -> {
			log.trace("update allocation page");
			AllocationUtils.cleanup(process());
			setTableInputs();
		});
		editor.onSaved(this::setTableInputs);
	}

	/**
	 * Update the given allocation factor with the given value. Returns true if it
	 * was updated and false when the value is the same as before or invalid.
	 */
	boolean update(AllocationFactor factor, String value) {
		if (factor == null)
			return false;
		if (Strings.nullOrEmpty(value)) {
			MsgBox.error(
					M.InvalidAllocationFactor,
					"An empty value is not allowed");
			return false;
		}

		// check if it is a number
		try {
			double val = Double.parseDouble(value);
			if (Double.compare(val, factor.value) == 0) {
				// do nothing if the value is the same
				// and no formula was set before
				if (Strings.nullOrEmpty(factor.formula)) {
					return false;
				}
			}
			factor.value = val;
			factor.formula = null;
			return true;
		} catch (Exception ignored) {
		}

		// check if it is a valid formula
		try {
			var scope = Formulas.createScope(Database.get(), process());
			double val = scope.eval(value);
			if (Strings.nullOrEqual(value, factor.formula)) {
				// do nothing when the formula is the same as before
				return false;
			}
			factor.formula = value;
			factor.value = val;
			return true;
		} catch (Exception e) {
			MsgBox.error(M.InvalidAllocationFactor, value + M.IsNotValidNumber);
			return false;
		}
	}

	private void setTableInputs() {
		if (table != null) {
			table.setInput(Row.all(this));
		}
		if (causalTable != null) {
			causalTable.refresh();
		}
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.formHeader(this);
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);
		var comp = UI.formComposite(body, tk);
		createDefaultCombo(comp, tk);
		createCalcButton(comp, tk);
		createPhysicalEconomicSection(body, tk);
		createCausalSection(body, tk);
		form.reflow(true);
	}

	private void createDefaultCombo(Composite comp, FormToolkit tk) {
		UI.formLabel(comp, tk, M.DefaultMethod);
		var combo = new AllocationCombo(comp,
			AllocationMethod.NONE,
			AllocationMethod.CAUSAL,
			AllocationMethod.ECONOMIC,
			AllocationMethod.PHYSICAL);
		var selected = process().defaultAllocationMethod;
		if (selected == null) {
			selected = AllocationMethod.NONE;
		}
		combo.select(selected);
		combo.addSelectionChangedListener(selection -> {
			process().defaultAllocationMethod = selection;
			editor.setDirty(true);
		});
		combo.setEnabled(isEditable());
	}

	private void createCalcButton(Composite comp, FormToolkit tk) {
		UI.filler(comp, tk);
		var btn = tk.createButton(comp, "Calculate factors", SWT.NONE);
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> {
			var refs = CalculationDialog.of(process());
			if (refs.isEmpty())
				return;
			var process = process();
			process.allocationFactors.clear();
			for (var ref : refs) {
				var factors = ref.apply(process);
				process.allocationFactors.addAll(factors);
			}
			table.refresh();
			causalTable.refresh();
			editor.setDirty(true);
		});
		btn.setEnabled(isEditable());
	}

	private void createPhysicalEconomicSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.PhysicalAndEconomicAllocation);
		var comp = UI.sectionClient(section, tk, 1);

		var columns = withComments
			? new String[]{M.Product, M.Physical, M.Physical + "-comment",
			M.Economic, M.Economic + "-comment"}
			: new String[]{M.Product, M.Physical, M.Economic};
		table = Tables.createViewer(comp, columns);
		table.setColumnProperties(columns);
		table.setLabelProvider(new FactorLabel());
		table.setInput(Row.all(this));
		table.getTable().getColumns()[1].setAlignment(SWT.CENTER);
		table.getTable().getColumns()[2].setAlignment(SWT.CENTER);

		if (!isEditable())
			return;

		// modifiers and actions
		var copy = TableClipboard.onCopySelected(table);
		var modifier = new ModifySupport<Row>(table)
			.bind(M.Physical, new ValueModifier(AllocationMethod.PHYSICAL))
			.bind(M.Economic, new ValueModifier(AllocationMethod.ECONOMIC));
		if (withComments) {
			modifier.bind(M.Physical + "-comment",
				commentModifier(AllocationMethod.PHYSICAL));
			modifier.bind(M.Economic + "-comment",
				commentModifier(AllocationMethod.ECONOMIC));
			Tables.bindColumnWidths(table, 0.3, 0.3, 0, 0.3, 0);
		} else {
			Tables.bindColumnWidths(table, 0.3, 0.3, 0.3);
		}
		CommentAction.bindTo(table, "allocationFactors", editor.getComments(), copy);

	}

	private CommentDialogModifier<Row> commentModifier(AllocationMethod method) {
		Function<Row, String> path = row -> {
			var factor = row.factorOf(method);
			return factor != null
				? CommentPaths.get(factor, row.product)
				: null;
		};
		return new CommentDialogModifier<>(editor.getComments(), path);
	}

	private void createCausalSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.CausalAllocation);
		UI.gridData(section, true, true);
		causalTable = new CausalFactorTable(this);
		causalTable.render(section, tk);
		CommentAction.bindTo(section, "allocationFactors", editor.getComments());
	}

	private Process process() {
		return editor.getModel();
	}

	private record Row(AllocationPage page, Exchange product) {

		static List<Row> all(AllocationPage page) {
			var products = AllocationUtils.getProviderFlows(page.process());
			if (products.size() < 2)
				return Collections.emptyList();
			var rows = new ArrayList<Row>(products.size() + 1);
			for (var p : products) {
				var item = new Row(page, p);
				rows.add(item);
			}
			rows.add(new Row(page, null));

			rows.sort((i1, i2) -> {
				if (i1.isSum())
					return 1;
				if (i2.isSum())
					return -1;
				return Strings.compare(i1.label(), i2.label());
			});
			return rows;
		}

		boolean isSum() {
			return product == null;
		}

		String label() {
			if (isSum())
				return "\u03a3"; // Sigma
			var name = Labels.name(product.flow);
			return product.unit != null
				? String.format("%s [%.2f %s]",
				name, product.amount, product.unit.name)
				: name;
		}

		AllocationFactor factorOf(AllocationMethod method) {
			if (product == null || product.flow == null)
				return null;
			for (var factor : page.process().allocationFactors) {
				if (factor.method == method
					&& factor.productId == product.flow.id)
					return factor;
			}
			return null;
		}

		String factorLabelOf(AllocationMethod method) {
			if (isSum())
				return Numbers.format(sumOf(method));
			var factor = factorOf(method);
			if (factor == null)
				return "";
			return Strings.nullOrEmpty(factor.formula)
				? Double.toString(factor.value)
				: factor.formula + " = " + factor.value;
		}

		double sumOf(AllocationMethod method) {
			return page.process().allocationFactors.stream()
				.filter(f -> Objects.equals(f.method, method))
				.mapToDouble(f -> f.value)
				.sum();
		}

	}

	private class FactorLabel extends LabelProvider implements
		ITableLabelProvider, ITableFontProvider, ITableColorProvider {

		@Override
		public Font getFont(Object obj, int col) {
			if (!(obj instanceof Row row))
				return null;
			return row.isSum()
				? UI.boldFont()
				: null;
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof Row row))
				return null;
			if (!row.isSum())
				return null;

			AllocationMethod method = col == 1
				? AllocationMethod.PHYSICAL
				: null;
			if ((withComments && col == 3)
				|| (!withComments && col == 2)) {
				method = AllocationMethod.ECONOMIC;
			}
			if (method == null)
				return null;

			var total = row.sumOf(method);
			return Math.abs(total - 1) > 1e-4
				? Colors.red()
				: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Row row))
				return null;
			return switch (col) {
				case 0 -> row.label();
				case 1 -> row.factorLabelOf(AllocationMethod.PHYSICAL);
				case 2 -> withComments
					? null
					: row.factorLabelOf(AllocationMethod.ECONOMIC);
				case 3 -> withComments
					? row.factorLabelOf(AllocationMethod.ECONOMIC)
					: null;
				default -> null;
			};
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Row row))
				return null;
			if (row.isSum())
				return null;
			if (col == 0)
				return Images.get(row.product.flow);

			if (withComments && (col == 2 || col == 4)) {
				var method = col == 2
					? AllocationMethod.PHYSICAL
					: AllocationMethod.ECONOMIC;
				var factor = row.factorOf(method);
				if (factor == null)
					return null;
				String path = CommentPaths.get(factor, row.product);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}
	}

	private class ValueModifier extends TextCellModifier<Row> {

		private final AllocationMethod method;

		public ValueModifier(AllocationMethod method) {
			this.method = method;
		}

		@Override
		protected String getText(Row row) {
			var factor = row.factorOf(method);
			if (factor == null)
				return "";
			return Strings.nullOrEmpty(factor.formula)
				? Double.toString(factor.value)
				: factor.formula;
		}

		@Override
		protected void setText(Row row, String text) {
			var factor = row.factorOf(method);
			boolean isNew = factor == null;
			if (isNew) {
				factor = new AllocationFactor();
				factor.method = method;
				factor.productId = row.product.flow.id;
			}
			if (update(factor, text)) {
				if (isNew) {
					process().allocationFactors.add(factor);
				}
				table.refresh();
				editor.setDirty(true);
			}
		}

		@Override
		public boolean canModify(Row row) {
			return !row.isSum() && row.product.flow != null;
		}
	}
}

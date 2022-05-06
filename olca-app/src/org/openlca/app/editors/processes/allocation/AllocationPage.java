package org.openlca.app.editors.processes.allocation;

import java.util.function.Function;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocationPage extends ModelPage<Process> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	final ProcessEditor editor;
	private FormToolkit tk;
	private TableViewer table;
	private CausalFactorTable causalTable;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", M.Allocation);
		this.editor = editor;
		editor.onEvent(ProcessEditor.EXCHANGES_CHANGED, () -> {
			log.trace("update allocation page");
			AllocationSync.updateFactors(process());
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
			MsgBox.error(M.InvalidAllocationFactor, value + M.IsNotValidNumber);
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
		if (table != null)
			table.setInput(Factors.getProviderFlows(process()));
		if (causalTable != null) {
			causalTable.refresh();
		}
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.formHeader(this);
		tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);
		var comp = UI.formComposite(body, tk);
		createDefaultCombo(comp);
		createCalcButton(comp);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
	}

	private void createDefaultCombo(Composite comp) {
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

	private void createCalcButton(Composite comp) {
		UI.filler(comp, tk);
		var btn = tk.createButton(comp, M.CalculateDefaultValues, SWT.NONE);
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> {
			var calc = CalculationDialog.of(process());
			if (calc.isEmpty())
				return;
			calc.run();
			// AllocationSync.calculateDefaults(process());
			table.refresh();
			causalTable.refresh();
			editor.setDirty(true);
		});
		btn.setEnabled(isEditable());
	}

	private void createPhysicalEconomicSection(Composite body) {
		var section = UI.section(body, tk, M.PhysicalAndEconomicAllocation);
		var comp = UI.sectionClient(section, tk, 1);

		var columns = editor.hasAnyComment("allocationFactors")
				? new String[] { M.Product, M.Physical, "", M.Economic, "" }
				: new String[] { M.Product, M.Physical, M.Economic };

		table = Tables.createViewer(comp, columns);

		// set keys for modifier binding
		if (editor.hasAnyComment("allocationFactors")) {
			columns[2] = M.Physical + "-comment";
			columns[4] = M.Economic + "-comment";
		}
		table.setColumnProperties(columns);
		table.setLabelProvider(new FactorLabel());
		table.setInput(Factors.getProviderFlows(process()));
		table.getTable().getColumns()[1].setAlignment(SWT.CENTER);
		table.getTable().getColumns()[2].setAlignment(SWT.CENTER);

		if (!isEditable())
			return;

		// modifiers and actions
		Action copy = TableClipboard.onCopySelected(table);
		var modifier = new ModifySupport<Exchange>(table)
				.bind(M.Physical, new ValueModifier(AllocationMethod.PHYSICAL))
				.bind(M.Economic, new ValueModifier(AllocationMethod.ECONOMIC));
		if (editor.hasComment("allocationFactors")) {
			modifier.bind(M.Physical + "-comment", commentModifier(AllocationMethod.PHYSICAL));
			modifier.bind(M.Economic + "-comment", commentModifier(AllocationMethod.ECONOMIC));
			Tables.bindColumnWidths(table, 0.3, 0.3, 0, 0.3, 0);
		} else {
			Tables.bindColumnWidths(table, 0.3, 0.3, 0.3);
		}
		CommentAction.bindTo(table, "allocationFactors", editor.getComments(), copy);

	}

	private CommentDialogModifier<Exchange> commentModifier(AllocationMethod method) {
		Function<Exchange, String> path = (Exchange e) -> {
			var factor = getFactor(e, method);
			return factor != null
					? CommentPaths.get(factor, e)
					: null;
		};
		return new CommentDialogModifier<>(editor.getComments(), path);
	}

	private void createCausalSection(Composite body) {
		var section = UI.section(body, tk, M.CausalAllocation);
		UI.gridData(section, true, true);
		causalTable = new CausalFactorTable(this);
		causalTable.render(section, tk);
		CommentAction.bindTo(section, "allocationFactors", editor.getComments());
	}

	private String productText(Exchange exchange) {
		String text = Labels.name(exchange.flow);
		text += " (" + Numbers.format(exchange.amount, 2) + " "
				+ exchange.unit.name + ")";
		return text;
	}

	private Process process() {
		return editor.getModel();
	}

	private AllocationFactor getFactor(Exchange e, AllocationMethod m) {
		if (e == null || m == null)
			return null;
		for (var factor : process().allocationFactors) {
			if (factor.method != m)
				continue;
			if (factor.productId != e.flow.id)
				continue;
			return factor;
		}
		return null;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Exchange e))
				return null;
			switch (col) {
			case 0:
				return productText(e);
			case 1:
				return getFactorLabel(e, AllocationMethod.PHYSICAL);
			case 2:
				if (editor.hasAnyComment("allocationFactors"))
					return null;
				return getFactorLabel(e, AllocationMethod.ECONOMIC);
			case 3:
				if (!editor.hasAnyComment("allocationFactors"))
					return null;
				return getFactorLabel(e, AllocationMethod.ECONOMIC);
			default:
				return null;
			}
		}

		private String getFactorLabel(Exchange e, AllocationMethod m) {
			var f = getFactor(e, m);
			if (f == null)
				return "1";
			return Strings.nullOrEmpty(f.formula)
					? Double.toString(f.value)
					: f.formula + " = " + f.value;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(FlowType.PRODUCT_FLOW);
			if (col == 2) {
				Exchange exchange = (Exchange) obj;
				var factor = getFactor(exchange, AllocationMethod.PHYSICAL);
				if (factor == null)
					return null;
				String path = CommentPaths.get(factor, exchange);
				return Images.get(editor.getComments(), path);
			}
			if (col == 4) {
				var exchange = (Exchange) obj;
				var factor = getFactor(exchange, AllocationMethod.ECONOMIC);
				if (factor == null)
					return null;
				var path = CommentPaths.get(factor, exchange);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}
	}

	private class ValueModifier extends TextCellModifier<Exchange> {

		private final AllocationMethod method;

		public ValueModifier(AllocationMethod method) {
			this.method = method;
		}

		@Override
		protected String getText(Exchange e) {
			var factor = getFactor(e, method);
			if (factor == null)
				return "1.0";
			return Strings.nullOrEmpty(factor.formula)
					? Double.toString(factor.value)
					: factor.formula;
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			var factor = getFactor(exchange, method);
			boolean isNew = factor == null;
			if (isNew) {
				factor = new AllocationFactor();
				factor.method = method;
				factor.productId = exchange.flow.id;
			}
			if (update(factor, text)) {
				if (isNew) {
					process().allocationFactors.add(factor);
				}
				editor.setDirty(true);
			}
		}

		@Override
		public boolean canModify(Exchange element) {
			return Factors.getProviderFlows(process()).size() > 1;
		}
	}
}

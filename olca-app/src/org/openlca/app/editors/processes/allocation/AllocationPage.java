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
import org.openlca.app.Event;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class AllocationPage extends ModelPage<Process> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ProcessEditor editor;
	private FormToolkit tk;
	private TableViewer table;
	private CausalFactorTable causalTable;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", M.Allocation);
		this.editor = editor;
		editor.getEventBus().register(this);
		editor.onSaved(this::setTableInputs);
	}

	static Double parseFactor(String text) {
		try {
			double val = Double.parseDouble(text);
			if (val < -0.0001 || val > 1.0001) {
				MsgBox.error(M.InvalidAllocationFactor,
						M.InvalidAllocationFactorMessage);
				return null;
			}
			return val;
		} catch (Exception e) {
			MsgBox.error(M.InvalidNumber, text + " "
					+ M.IsNotValidNumber);
			return null;
		}
	}

	@Subscribe
	public void handleExchangesChange(Event event) {
		if (!event.matches(ProcessEditor.EXCHANGES_CHANGED))
			return;
		log.trace("update allocation page");
		AllocationSync.updateFactors(process());
		setTableInputs();
	}

	private void setTableInputs() {
		if (table != null)
			table.setInput(Util.getProviderFlows(process()));
		if (causalTable != null) {
			causalTable.refresh();
		}
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		var comp = UI.formComposite(body, tk);
		createDefaultCombo(comp);
		createCalcButton(comp);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
		causalTable.setInitialInput();
	}

	private void createDefaultCombo(Composite comp) {
		UI.formLabel(comp, tk, M.DefaultMethod);
		AllocationMethod[] methods = {
				AllocationMethod.NONE,
				AllocationMethod.CAUSAL,
				AllocationMethod.ECONOMIC,
				AllocationMethod.PHYSICAL,
		};
		var combo = new AllocationMethodViewer(comp, methods);
		var selected = process().defaultAllocationMethod;
		if (selected == null) {
			selected = AllocationMethod.NONE;
		}
		combo.select(selected);
		combo.addSelectionChangedListener(selection -> {
			process().defaultAllocationMethod = selection;
			editor.setDirty(true);
		});
	}

	private void createCalcButton(Composite comp) {
		UI.filler(comp, tk);
		var button = tk.createButton(comp, M.CalculateDefaultValues, SWT.NONE);
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, e -> {
			AllocationSync.calculateDefaults(process());
			table.refresh();
			causalTable.refresh();
			editor.setDirty(true);
		});
	}

	private void createPhysicalEconomicSection(Composite body) {
		var section = UI.section(body, tk, M.PhysicalAndEconomicAllocation);
		var comp = UI.sectionClient(section, tk, 1);

		var columns = editor.hasAnyComment("allocationFactors")
				? new String[]{M.Product, M.Physical, "", M.Economic, ""}
				: new String[]{M.Product, M.Physical, M.Economic};

		table = Tables.createViewer(comp, columns);

		// set keys for modifier binding
		if (editor.hasAnyComment("allocationFactors")) {
			columns[2] = M.Physical + "-comment";
			columns[4] = M.Economic + "-comment";
		}
		table.setColumnProperties(columns);
		table.setLabelProvider(new FactorLabel());
		table.setInput(Util.getProviderFlows(process()));
		Action copy = TableClipboard.onCopy(table);

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
		table.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
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
		causalTable = new CausalFactorTable(editor);
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

	private String getFactorLabel(Exchange e, AllocationMethod m) {
		var f = getFactor(e, m);
		if (f == null)
			return Double.toString(1);
		return Strings.nullOrEmpty(f.formula)
				? Double.toString(f.value)
				: f.formula + " = " + f.value;
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Exchange))
				return null;
			var e = (Exchange) obj;
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
			return getFactorLabel(e, method);
		}

		@Override
		protected void setText(Exchange e, String text) {
			Double val = parseFactor(text);
			if (val == null)
				return;
			AllocationFactor factor = getFactor(e, method);
			if (factor == null) {
				factor = new AllocationFactor();
				factor.method = method;
				factor.productId = e.flow.id;
				process().allocationFactors.add(factor);
			}
			factor.value = val;
			editor.setDirty(true);
		}

		@Override
		public boolean canModify(Exchange element) {
			return Util.getProviderFlows(process()).size() > 1;
		}
	}
}

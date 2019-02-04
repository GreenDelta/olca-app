package org.openlca.app.editors.processes.allocation;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
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
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class AllocationPage extends ModelPage<Process> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ProcessEditor editor;
	private FormToolkit tk;
	private TableViewer factorViewer;
	private CausalFactorTable causalFactorTable;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", M.Allocation);
		this.editor = editor;
		editor.getEventBus().register(this);
		editor.onSaved(() -> setTableInputs());
	}

	static Double parseFactor(String text) {
		try {
			double val = Double.parseDouble(text);
			if (val < -0.0001 || val > 1.0001) {
				Error.showBox(M.InvalidAllocationFactor,
						M.InvalidAllocationFactorMessage);
				return null;
			}
			return val;
		} catch (Exception e) {
			Error.showBox(M.InvalidNumber, text + " "
					+ M.IsNotValidNumber);
			return null;
		}
	}

	@Subscribe
	public void handleExchangesChange(Event event) {
		if (!event.match(editor.EXCHANGES_CHANGED))
			return;
		log.trace("update allocation page");
		AllocationSync.updateFactors(process());
		setTableInputs();
	}

	private void setTableInputs() {
		if (factorViewer != null)
			factorViewer.setInput(Util.getProviderFlows(process()));
		if (causalFactorTable != null)
			causalFactorTable.refresh();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(this);
		tk = managedForm.getToolkit();
		Composite body = UI.formBody(form, tk);
		Composite composite = UI.formComposite(body, tk);
		createDefaultCombo(composite);
		createCalcButton(composite);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
		causalFactorTable.setInitialInput();
	}

	private void createDefaultCombo(Composite composite) {
		UI.formLabel(composite, tk, M.DefaultMethod);
		AllocationMethod[] methods = { AllocationMethod.NONE,
				AllocationMethod.CAUSAL, AllocationMethod.ECONOMIC,
				AllocationMethod.PHYSICAL, };
		AllocationMethodViewer viewer = new AllocationMethodViewer(composite,
				methods);
		AllocationMethod selected = process().defaultAllocationMethod;
		if (selected == null)
			selected = AllocationMethod.NONE;
		viewer.select(selected);
		viewer.addSelectionChangedListener(selection -> {
			process().defaultAllocationMethod = selection;
			editor.setDirty(true);
		});
	}

	private void createCalcButton(Composite comp) {
		UI.filler(comp, tk);
		Button button = tk.createButton(comp, M.CalculateDefaultValues, SWT.NONE);
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, e -> {
			AllocationSync.calculateDefaults(process());
			factorViewer.refresh();
			causalFactorTable.refresh();
			editor.setDirty(true);
		});
	}

	private void createPhysicalEconomicSection(Composite body) {
		Section section = UI.section(body, tk, M.PhysicalAndEconomicAllocation);
		Composite composite = UI.sectionClient(section, tk, 1);
		String[] colNames = { M.Product, M.Physical, M.Economic };
		if (editor.hasAnyComment("allocationFactors")) {
			colNames = new String[] { M.Product, M.Physical, "", M.Economic, "" };
		}
		factorViewer = Tables.createViewer(composite, colNames);
		// set keys for modifier binding
		if (editor.hasAnyComment("allocationFactors")) {
			colNames[2] = M.Physical + "-comment";
			colNames[4] = M.Economic + "-comment";
		}
		factorViewer.setColumnProperties(colNames);
		factorViewer.setLabelProvider(new FactorLabel());
		factorViewer.setInput(Util.getProviderFlows(process()));
		Action copy = TableClipboard.onCopy(factorViewer);
		ModifySupport<Exchange> modifySupport = new ModifySupport<>(factorViewer);
		modifySupport.bind(M.Physical, new ValueModifier(AllocationMethod.PHYSICAL));
		modifySupport.bind(M.Economic, new ValueModifier(AllocationMethod.ECONOMIC));
		if (editor.hasComment("allocationFactors")) {
			modifySupport.bind(M.Physical + "-comment", createCommentModifier(AllocationMethod.PHYSICAL));
			modifySupport.bind(M.Economic + "-comment", createCommentModifier(AllocationMethod.ECONOMIC));
			Tables.bindColumnWidths(factorViewer, 0.3, 0.3, 0, 0.3, 0);
		} else {
			Tables.bindColumnWidths(factorViewer, 0.3, 0.3, 0.3);
		}
		CommentAction.bindTo(factorViewer, "allocationFactors", editor.getComments(), copy);
		factorViewer.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		factorViewer.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
	}

	private CommentDialogModifier<Exchange> createCommentModifier(AllocationMethod method) {
		return new CommentDialogModifier<>(editor.getComments(), (e) -> getPath(e, method));
	}

	private String getPath(Exchange e, AllocationMethod m) {
		AllocationFactor factor = getFactor(e, m);
		if (factor == null)
			return null;
		return CommentPaths.get(factor, e);
	}

	private void createCausalSection(Composite body) {
		Section section = UI.section(body, tk, M.CausalAllocation);
		UI.gridData(section, true, true);
		causalFactorTable = new CausalFactorTable(editor);
		causalFactorTable.render(section, tk);
		CommentAction.bindTo(section, "allocationFactors", editor.getComments());
	}

	private String productText(Exchange exchange) {
		String text = Labels.getDisplayName(exchange.flow);
		text += " (" + Numbers.format(exchange.amount, 2) + " "
				+ exchange.unit.name + ")";
		return text;
	}

	private Process process() {
		return editor.getModel();
	}

	private AllocationFactor getFactor(Exchange exchange, AllocationMethod method) {
		if (exchange == null || method == null)
			return null;
		for (AllocationFactor factor : process().allocationFactors) {
			if (factor.method != method)
				continue;
			if (factor.productId != exchange.flow.id)
				continue;
			return factor;
		}
		return null;
	}

	private String getFactorLabel(Exchange exchange, AllocationMethod method) {
		AllocationFactor factor = getFactor(exchange, method);
		if (factor == null)
			return Double.toString(1);
		return Double.toString(factor.value);
	}

	private class FactorLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			switch (col) {
			case 0:
				return productText(exchange);
			case 1:
				return getFactorLabel(exchange, AllocationMethod.PHYSICAL);
			case 2:
				if (editor.hasAnyComment("allocationFactors"))
					return null;
				return getFactorLabel(exchange, AllocationMethod.ECONOMIC);
			case 3:
				if (!editor.hasAnyComment("allocationFactors"))
					return null;
				return getFactorLabel(exchange, AllocationMethod.ECONOMIC);
			default:
				return null;
			}
		}

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 0)
				return Images.get(FlowType.PRODUCT_FLOW);
			if (col == 2) {
				Exchange exchange = (Exchange) element;
				AllocationFactor factor = getFactor(exchange, AllocationMethod.PHYSICAL);
				if (factor == null)
					return null;
				String path = CommentPaths.get(factor, exchange);
				return Images.get(editor.getComments(), path);
			}
			if (col == 4) {
				Exchange exchange = (Exchange) element;
				AllocationFactor factor = getFactor(exchange, AllocationMethod.ECONOMIC);
				if (factor == null)
					return null;
				String path = CommentPaths.get(factor, exchange);
				return Images.get(editor.getComments(), path);
			}
			return null;
		}
	}

	private class ValueModifier extends TextCellModifier<Exchange> {

		private AllocationMethod method;

		public ValueModifier(AllocationMethod method) {
			this.method = method;
		}

		@Override
		protected String getText(Exchange exchange) {
			return getFactorLabel(exchange, method);
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			Double val = parseFactor(text);
			if (val == null)
				return;
			AllocationFactor factor = getFactor(exchange, method);
			if (factor == null) {
				factor = new AllocationFactor();
				factor.method = method;
				factor.productId = exchange.flow.id;
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

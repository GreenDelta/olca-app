package org.openlca.app.processes;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Event;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

class AllocationPage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Process process;
	private ProcessEditor editor;
	private FormToolkit toolkit;
	private TableViewer factorViewer;
	private CausalFactorTable causalFactorTable;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", "Allocation");
		this.process = editor.getModel();
		this.editor = editor;
		editor.getEventBus().register(this);
	}

	@Subscribe
	public void handleExchangesChange(Event event) {
		if (!event.match(editor.EXCHANGES_CHANGED))
			return;
		log.trace("update allocation page");
		AllocationSync.updateFactors(process);
		factorViewer.setInput(Processes.getOutputProducts(process));
		causalFactorTable.refresh();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Allocation");
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = UI.formComposite(body, toolkit);
		createDefaultCombo(composite);
		createCalcButton(composite);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
	}

	private void createDefaultCombo(Composite composite) {
		UI.formLabel(composite, toolkit, Messages.DefaultMethod);
		AllocationMethod[] methods = { AllocationMethod.NONE,
				AllocationMethod.CAUSAL, AllocationMethod.ECONOMIC,
				AllocationMethod.PHYSICAL, };
		AllocationMethodViewer viewer = new AllocationMethodViewer(composite,
				methods);
		AllocationMethod selected = process.getDefaultAllocationMethod();
		if (selected == null)
			selected = AllocationMethod.NONE;
		viewer.select(selected);
		viewer.addSelectionChangedListener(new ISelectionChangedListener<AllocationMethod>() {
			@Override
			public void selectionChanged(AllocationMethod selection) {
				process.setDefaultAllocationMethod(selection);
				editor.setDirty(true);
			}
		});
	}

	private void createCalcButton(Composite composite) {
		UI.formLabel(composite, toolkit, "");
		Button button = toolkit.createButton(composite,
				Messages.CalculateDefaults, SWT.NONE);
		button.setImage(ImageType.CALCULATE_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AllocationSync.calculateDefaults(process);
				factorViewer.refresh();
				causalFactorTable.refresh();
				editor.setDirty(true);
			}
		});
	}

	private void createPhysicalEconomicSection(Composite body) {
		Section section = UI.section(body, toolkit,
				"Physical & economic allocation");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] colNames = { Messages.Product, Messages.Physical,
				Messages.Economic };
		factorViewer = Tables.createViewer(composite, colNames);
		Tables.bindColumnWidths(factorViewer, 0.3, 0.3, 0.3);
		factorViewer.setLabelProvider(new FactorLabel());
		factorViewer.setInput(Processes.getOutputProducts(process));
	}

	private void createCausalSection(Composite body) {
		Section section = UI.section(body, toolkit, "Causal allocation");
		causalFactorTable = new CausalFactorTable(editor);
		causalFactorTable.render(section, toolkit);
	}

	private String productText(Exchange exchange) {
		String text = Labels.getDisplayName(exchange.getFlow());
		text += " (" + Numbers.format(exchange.getAmountValue(), 2) + " "
				+ exchange.getUnit().getName() + ")";
		return text;
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
				return Numbers.format(getFactor(exchange,
						AllocationMethod.PHYSICAL));
			case 2:
				return Numbers.format(getFactor(exchange,
						AllocationMethod.ECONOMIC));
			default:
				return null;
			}
		}

		private double getFactor(Exchange exchange, AllocationMethod method) {
			if (exchange == null || method == null)
				return 0;
			AllocationFactor factor = null;
			for (AllocationFactor f : process.getAllocationFactors()) {
				if (f.getAllocationType() == method
						&& f.getProductId() == exchange.getFlow().getId()) {
					factor = f;
					break;
				}
			}
			return factor == null ? 1 : factor.getValue();
		}

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 0)
				return ImageType.FLOW_PRODUCT.get();
			return null;
		}
	}

}
